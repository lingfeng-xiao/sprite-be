package com.lingfeng.sprite.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.LongTermMemory;

/**
 * S4: GitHub备份服务
 *
 * 负责将记忆数据定期备份到GitHub
 *
 * 功能：
 * 1. 定时导出记忆到GitHub仓库
 * 2. 支持版本回溯
 * 3. 处理冲突
 */
@Service
public class GitHubBackupService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubBackupService.class);

    // GitHub API 配置
    @Value("${github.token:}")
    private String githubToken;

    @Value("${github.owner:lingfeng-xiao}")
    private String owner;

    @Value("${github.repo:soul-hub}")
    private String repo;

    @Value("${github.backup-branch:main}")
    private String backupBranch;

    @Value("${github.backup-enabled:false}")
    private boolean backupEnabled;

    // 备份路径
    private static final String BACKUP_BASE_PATH = "backups/memory";
    private static final String MEMORY_DIR = "data/memory/long-term";

    // 备份间隔（小时）
    private static final long BACKUP_INTERVAL_HOURS = 6;

    private final MemorySystem.Memory memory;
    private final MemoryPersistenceService memoryPersistenceService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<String, Instant> lastBackupTimes = new ConcurrentHashMap<>();

    // HTTP 客户端
    private final CloseableHttpClient httpClient;
    private static final String GITHUB_API_BASE = "https://api.github.com";

    public GitHubBackupService(
            @Autowired MemorySystem.Memory memory,
            @Autowired MemoryPersistenceService memoryPersistenceService
    ) {
        this.memory = memory;
        this.memoryPersistenceService = memoryPersistenceService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 创建HTTP客户端
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(10);
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();

        if (backupEnabled) {
            startPeriodicBackup();
        }
    }

    /**
     * 启动定期备份
     */
    private void startPeriodicBackup() {
        if (!backupEnabled || githubToken == null || githubToken.isEmpty()) {
            logger.info("GitHub backup is disabled or token not configured");
            return;
        }

        scheduler.scheduleAtFixedRate(
                this::performBackup,
                BACKUP_INTERVAL_HOURS,
                BACKUP_INTERVAL_HOURS,
                TimeUnit.HOURS
        );
        logger.info("Periodic GitHub backup started (every {} hours)", BACKUP_INTERVAL_HOURS);
    }

    /**
     * 执行备份
     */
    public BackupResult performBackup() {
        if (!backupEnabled || githubToken == null || githubToken.isEmpty()) {
            return new BackupResult(false, "Backup disabled or token not configured", 0);
        }

        Instant startTime = Instant.now();
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .withZone(ZoneId.of("Asia/Shanghai"))
                .format(startTime);

        try {
            // 先保存本地记忆
            memoryPersistenceService.saveMemory();

            // 获取记忆文件
            Path memoryPath = Paths.get(MEMORY_DIR);
            if (!Files.exists(memoryPath)) {
                return new BackupResult(false, "Memory directory not found", 0);
            }

            int filesBackedUp = 0;

            // 备份情景记忆
            if (backupFile(memoryPath.resolve("episodic.json"), timestamp)) {
                filesBackedUp++;
            }

            // 备份语义记忆
            if (backupFile(memoryPath.resolve("semantic.json"), timestamp)) {
                filesBackedUp++;
            }

            // 备份程序记忆
            if (backupFile(memoryPath.resolve("procedural.json"), timestamp)) {
                filesBackedUp++;
            }

            // 更新备份索引
            updateBackupIndex(timestamp, filesBackedUp);

            lastBackupTimes.put("lastBackup", startTime);

            long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
            logger.info("GitHub backup completed: {} files in {}ms", filesBackedUp, duration);

            return new BackupResult(true, "Backup completed successfully", filesBackedUp);

        } catch (Exception e) {
            logger.error("GitHub backup failed: {}", e.getMessage());
            return new BackupResult(false, "Backup failed: " + e.getMessage(), 0);
        }
    }

    /**
     * 备份单个文件到GitHub
     */
    private boolean backupFile(Path filePath, String timestamp) {
        if (!Files.exists(filePath)) {
            logger.debug("File not found, skipping: {}", filePath);
            return false;
        }

        try {
            String fileName = filePath.getFileName().toString();
            String fileContent = Files.readString(filePath);

            // 计算备份路径
            String backupPath = String.format("%s/%s/%s", BACKUP_BASE_PATH, timestamp, fileName);
            String latestPath = String.format("%s/latest/%s", BACKUP_BASE_PATH, fileName);

            // 写入带时间戳的版本
            commitFile(backupPath, fileContent, "Backup: " + fileName + " (" + timestamp + ")");

            // 更新 latest 版本
            commitFile(latestPath, fileContent, "Update: " + fileName);

            logger.debug("Backed up file: {}", fileName);
            return true;

        } catch (Exception e) {
            logger.error("Failed to backup file {}: {}", filePath, e.getMessage());
            return false;
        }
    }

    /**
     * 向GitHub提交文件
     */
    private void commitFile(String path, String content, String message) throws IOException {
        // 检查文件是否存在
        String existingSha = getFileSha(path);

        // 构建请求
        String url = String.format("%s/repos/%s/%s/contents/%s", GITHUB_API_BASE, owner, repo, path);

        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + githubToken);
        request.setHeader("Accept", "application/vnd.github+json");
        request.setHeader("X-GitHub-Api-Version", "2022-11-28");

        // 构建请求体
        String jsonBody;
        if (existingSha != null) {
            // 更新现有文件
            jsonBody = String.format("""
                {
                    "message": "%s",
                    "content": "%s",
                    "sha": "%s",
                    "branch": "%s"
                }
                """, message, java.util.Base64.getEncoder().encodeToString(content.getBytes()), existingSha, backupBranch);
        } else {
            // 创建新文件
            jsonBody = String.format("""
                {
                    "message": "%s",
                    "content": "%s",
                    "branch": "%s"
                }
                """, message, java.util.Base64.getEncoder().encodeToString(content.getBytes()), backupBranch);
        }

        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            if (statusCode != 200 && statusCode != 201) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.warn("GitHub API returned {} for path {}: {}", statusCode, path, responseBody);
            }
        }
    }

    /**
     * 获取文件的 SHA（用于更新现有文件）
     */
    private String getFileSha(String path) {
        String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                GITHUB_API_BASE, owner, repo, path, backupBranch);

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + githubToken);
        request.setHeader("Accept", "application/vnd.github+json");
        request.setHeader("X-GitHub-Api-Version", "2022-11-28");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() == 200) {
                var mapper = new ObjectMapper();
                var tree = mapper.readTree(response.getEntity().getContent());
                return tree.path("sha").asText();
            }
        } catch (Exception e) {
            logger.debug("File does not exist yet on GitHub: {}", path);
        }
        return null;
    }

    /**
     * 更新备份索引
     */
    private void updateBackupIndex(String timestamp, int filesCount) throws IOException {
        // 读取现有索引
        String indexPath = BACKUP_BASE_PATH + "/index.json";
        BackupIndex index;

        try {
            index = getBackupIndex();
        } catch (Exception e) {
            index = new BackupIndex();
        }

        // 添加新备份记录
        BackupRecord record = new BackupRecord(
                timestamp,
                Instant.now().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime().toString(),
                filesCount,
                "success"
        );
        index.records().add(0, record); // 添加到列表开头

        // 只保留最近100条记录
        if (index.records().size() > 100) {
            index = new BackupIndex(index.records().subList(0, 100));
        }

        // 提交索引
        String indexContent = objectMapper.writeValueAsString(index);
        commitFile(indexPath, indexContent, "Update backup index: " + timestamp);
    }

    /**
     * 获取备份索引
     */
    public BackupIndex getBackupIndex() throws IOException {
        String indexPath = BACKUP_BASE_PATH + "/index.json";
        String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                GITHUB_API_BASE, owner, repo, indexPath, backupBranch);

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + githubToken);
        request.setHeader("Accept", "application/vnd.github.raw+json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() == 200) {
                return objectMapper.readValue(response.getEntity().getContent(), BackupIndex.class);
            }
        }
        return new BackupIndex();
    }

    /**
     * 获取指定版本的记忆
     */
    public MemorySnapshot getMemorySnapshot(String timestamp) {
        String episodicPath = String.format("%s/%s/episodic.json", BACKUP_BASE_PATH, timestamp);
        String semanticPath = String.format("%s/%s/semantic.json", BACKUP_BASE_PATH, timestamp);
        String proceduralPath = String.format("%s/%s/procedural.json", BACKUP_BASE_PATH, timestamp);

        MemorySnapshot snapshot = new MemorySnapshot(timestamp);

        try {
            snapshot.episodicContent(fetchFileContent(episodicPath));
        } catch (Exception e) {
            logger.debug("Could not fetch episodic memory for {}", timestamp);
        }

        try {
            snapshot.semanticContent(fetchFileContent(semanticPath));
        } catch (Exception e) {
            logger.debug("Could not fetch semantic memory for {}", timestamp);
        }

        try {
            snapshot.proceduralContent(fetchFileContent(proceduralPath));
        } catch (Exception e) {
            logger.debug("Could not fetch procedural memory for {}", timestamp);
        }

        return snapshot;
    }

    /**
     * 获取文件内容
     */
    private String fetchFileContent(String path) throws IOException {
        String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                GITHUB_API_BASE, owner, repo, path, backupBranch);

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + githubToken);
        request.setHeader("Accept", "application/vnd.github.raw+json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() == 200) {
                return new String(response.getEntity().getContent().readAllBytes());
            }
            return null;
        }
    }

    /**
     * 手动触发备份
     */
    public BackupResult forceBackup() {
        return performBackup();
    }

    /**
     * 获取上次备份时间
     */
    public Instant getLastBackupTime() {
        return lastBackupTimes.get("lastBackup");
    }

    /**
     * 备份结果
     */
    public record BackupResult(
            boolean success,
            String message,
            int filesBackedUp
    ) {}

    /**
     * 备份记录
     */
    public record BackupRecord(
            String timestamp,
            String localDateTime,
            int filesCount,
            String status
    ) {}

    /**
     * 备份索引
     */
    public record BackupIndex(
            java.util.List<BackupRecord> records
    ) {
        public BackupIndex() {
            this(new java.util.ArrayList<>());
        }
    }

    /**
     * 记忆快照
     */
    public static class MemorySnapshot {
        private final String timestamp;
        private String episodicContent;
        private String semanticContent;
        private String proceduralContent;

        public MemorySnapshot(String timestamp) {
            this.timestamp = timestamp;
        }

        public String timestamp() { return timestamp; }
        public String episodicContent() { return episodicContent; }
        public void episodicContent(String c) { this.episodicContent = c; }
        public String semanticContent() { return semanticContent; }
        public void semanticContent(String c) { this.semanticContent = c; }
        public String proceduralContent() { return proceduralContent; }
        public void proceduralContent(String c) { this.proceduralContent = c; }
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("Error closing HTTP client: {}", e.getMessage());
        }
        logger.info("GitHubBackupService shutdown complete");
    }
}
