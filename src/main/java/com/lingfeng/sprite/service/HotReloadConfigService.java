package com.lingfeng.sprite.service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * S11-3: 配置热更新服务 - S13-3: 添加Spring服务注解
 *
 * 支持运行时配置动态更新：
 * - JSON/YAML配置文件监听
 * - 变更自动检测
 * - 回调机制通知配置变更
 * - 配置版本管理
 */
@Service
public class HotReloadConfigService {

    private static final Logger logger = LoggerFactory.getLogger(HotReloadConfigService.class);

    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;
    private final Map<String, ConfigEntry> configs;
    private final Map<String, ConfigCallback> callbacks;
    private final ScheduledExecutorService scheduler;
    private static final long DEFAULT_POLL_INTERVAL = 5; // seconds

    /**
     * 配置条目
     */
    public record ConfigEntry(
        String path,
        String content,
        Instant lastModified,
        Instant lastLoaded,
        Map<String, Object> data
    ) {}

    /**
     * 配置变更回调
     */
    public interface ConfigCallback {
        void onConfigChanged(String configPath, Map<String, Object> newData);
    }

    /**
     * 配置统计
     */
    public record ConfigStats(
        int loadedConfigs,
        Instant lastCheck,
        int totalCallbacks
    ) {}

    public HotReloadConfigService() {
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.configs = new ConcurrentHashMap<>();
        this.callbacks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    // ==================== 配置加载 ====================

    /**
     * 加载配置文件
     */
    public ConfigEntry loadConfig(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            logger.warn("Config file not found: {}", path);
            return null;
        }

        String content = Files.readString(file.toPath());
        Instant lastModified = Instant.ofEpochMilli(file.lastModified());

        Map<String, Object> data = parseContent(content, path);

        ConfigEntry entry = new ConfigEntry(
            path,
            content,
            lastModified,
            Instant.now(),
            data
        );

        configs.put(path, entry);
        logger.info("Loaded config: {} ({} bytes)", path, content.length());

        return entry;
    }

    /**
     * 解析配置文件内容
     */
    private Map<String, Object> parseContent(String content, String path) throws Exception {
        if (path.endsWith(".json")) {
            return jsonMapper.readValue(content, Map.class);
        } else if (path.endsWith(".yaml") || path.endsWith(".yml")) {
            return yamlMapper.readValue(content, Map.class);
        } else {
            throw new IllegalArgumentException("Unsupported config format: " + path);
        }
    }

    /**
     * 获取配置
     */
    public Map<String, Object> getConfig(String path) {
        ConfigEntry entry = configs.get(path);
        return entry != null ? entry.data() : null;
    }

    /**
     * 获取配置条目
     */
    public ConfigEntry getConfigEntry(String path) {
        return configs.get(path);
    }

    // ==================== 配置更新 ====================

    /**
     * 保存配置
     */
    public void saveConfig(String path, Map<String, Object> data) throws Exception {
        String content;
        if (path.endsWith(".json")) {
            content = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } else if (path.endsWith(".yaml") || path.endsWith(".yml")) {
            content = yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } else {
            throw new IllegalArgumentException("Unsupported config format: " + path);
        }

        try (FileWriter writer = new FileWriter(path)) {
            writer.write(content);
        }

        Instant now = Instant.now();
        ConfigEntry entry = new ConfigEntry(path, content, now, now, data);
        configs.put(path, entry);

        // 触发回调
        notifyCallbacks(path, data);

        logger.info("Saved config: {}", path);
    }

    /**
     * 更新单个配置值
     */
    public void updateValue(String path, String key, Object value) throws Exception {
        Map<String, Object> current = getConfig(path);
        if (current == null) {
            current = new ConcurrentHashMap<>();
        }

        // 支持嵌套key如 "database.connection.timeout"
        setNestedValue(current, key, value);

        saveConfig(path, current);
    }

    /**
     * 设置嵌套值
     */
    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String key, Object value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = map;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);
            if (next == null) {
                next = new ConcurrentHashMap<>();
                current.put(part, next);
            }
            current = (Map<String, Object>) next;
        }

        current.put(parts[parts.length - 1], value);
    }

    // ==================== 热更新监听 ====================

    /**
     * 启动文件监听
     */
    public void startWatching(String path) {
        startWatching(path, DEFAULT_POLL_INTERVAL);
    }

    /**
     * 启动文件监听（自定义间隔）
     */
    public void startWatching(String path, long intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> checkForChanges(path), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        logger.info("Started watching config: {} (interval: {}s)", path, intervalSeconds);
    }

    /**
     * 检查配置变更
     */
    private void checkForChanges(String path) {
        ConfigEntry current = configs.get(path);
        if (current == null) return;

        try {
            File file = new File(path);
            if (!file.exists()) return;

            Instant fileModified = Instant.ofEpochMilli(file.lastModified());
            if (fileModified.isAfter(current.lastModified())) {
                logger.info("Config file changed: {}", path);
                ConfigEntry reloaded = loadConfig(path);
                if (reloaded != null) {
                    notifyCallbacks(path, reloaded.data());
                }
            }
        } catch (Exception e) {
            logger.error("Error checking config changes: {}", e.getMessage());
        }
    }

    /**
     * 停止监听
     */
    public void stopWatching() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        logger.info("Stopped config watching");
    }

    // ==================== 回调管理 ====================

    /**
     * 注册配置变更回调
     */
    public void registerCallback(String configPath, ConfigCallback callback) {
        callbacks.put(configPath, callback);
        logger.debug("Registered callback for: {}", configPath);
    }

    /**
     * 注销回调
     */
    public void unregisterCallback(String configPath) {
        callbacks.remove(configPath);
    }

    /**
     * 通知配置变更
     */
    private void notifyCallbacks(String path, Map<String, Object> newData) {
        ConfigCallback callback = callbacks.get(path);
        if (callback != null) {
            try {
                callback.onConfigChanged(path, newData);
            } catch (Exception e) {
                logger.error("Error in config callback: {}", e.getMessage());
            }
        }
    }

    // ==================== 版本管理 ====================

    /**
     * 备份配置
     */
    public void backupConfig(String path) throws Exception {
        ConfigEntry current = configs.get(path);
        if (current == null) {
            logger.warn("No config to backup: {}", path);
            return;
        }

        String backupPath = path + ".backup-" + System.currentTimeMillis();
        try (FileWriter writer = new FileWriter(backupPath)) {
            writer.write(current.content());
        }
        logger.info("Backed up config to: {}", backupPath);
    }

    /**
     * 列出可用备份
     */
    public java.util.List<String> listBackups(String path) throws Exception {
        File dir = new File(path).getParentFile();
        String baseName = new File(path).getName();

        return java.util.Arrays.stream(dir.listFiles())
            .filter(f -> f.getName().startsWith(baseName + ".backup-"))
            .map(f -> f.getName() + " (" + f.length() + " bytes)")
            .toList();
    }

    // ==================== 统计 ====================

    /**
     * 获取统计信息
     */
    public ConfigStats getStats() {
        return new ConfigStats(
            configs.size(),
            Instant.now(),
            callbacks.size()
        );
    }
}
