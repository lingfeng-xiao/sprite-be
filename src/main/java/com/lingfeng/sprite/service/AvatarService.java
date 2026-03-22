package com.lingfeng.sprite.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.SelfModel;

/**
 * 多端分身感知服务
 *
 * 负责管理数字生命的多个设备分身：
 * 1. 注册当前设备分身
 * 2. 心跳更新 lastSeen
 * 3. 跨设备感知（通过共享文件）
 */
@Service
public class AvatarService {

    private static final Logger logger = LoggerFactory.getLogger(AvatarService.class);
    private static final String AVATARS_FILE = "data/avatars.json";

    private final Map<String, SelfModel.Avatar> avatars = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final String currentDeviceId;

    public AvatarService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.currentDeviceId = generateDeviceId();
        loadAvatars();
    }

    /**
     * 生成设备唯一ID
     */
    private String generateDeviceId() {
        // 使用主机名 + 随机后缀生成唯一ID
        String hostname = System.getProperty("os.name", "unknown");
        return hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取当前设备ID
     */
    public String getCurrentDeviceId() {
        return currentDeviceId;
    }

    /**
     * 注册当前设备分身
     */
    public void registerCurrentDevice() {
        registerDevice(currentDeviceId, getDeviceType(), "current session");
    }

    /**
     * 注册一个设备分身
     */
    public void registerDevice(String deviceId, PerceptionSystem.DeviceType deviceType, String localContext) {
        SelfModel.Avatar avatar = new SelfModel.Avatar(
            deviceId,
            deviceType.name(),
            Instant.now(),
            localContext
        );
        avatars.put(deviceId, avatar);
        logger.info("Registered avatar: deviceId={}, type={}", deviceId, deviceType);
        saveAvatars();
    }

    /**
     * 更新设备分身的 lastSeen
     */
    public void updateLastSeen(String deviceId) {
        SelfModel.Avatar avatar = avatars.get(deviceId);
        if (avatar != null) {
            SelfModel.Avatar updated = new SelfModel.Avatar(
                avatar.deviceId(),
                avatar.deviceType(),
                Instant.now(),
                avatar.localContext()
            );
            avatars.put(deviceId, updated);
        }
    }

    /**
     * 心跳更新当前设备
     */
    @Scheduled(fixedRate = 60000) // 每分钟更新一次
    public void heartbeat() {
        updateLastSeen(currentDeviceId);
        logger.debug("Avatar heartbeat for device: {}", currentDeviceId);
    }

    /**
     * 获取所有分身
     */
    public List<SelfModel.Avatar> getAllAvatars() {
        return new ArrayList<>(avatars.values());
    }

    /**
     * 获取当前设备分身
     */
    public SelfModel.Avatar getCurrentAvatar() {
        return avatars.get(currentDeviceId);
    }

    /**
     * 获取其他设备分身（除当前设备外）
     */
    public List<SelfModel.Avatar> getOtherAvatars() {
        return avatars.values().stream()
            .filter(a -> !a.deviceId().equals(currentDeviceId))
            .toList();
    }

    /**
     * 判断是否有其他在线设备
     */
    public boolean hasOtherOnlineDevices(int onlineThresholdMinutes) {
        Instant threshold = Instant.now().minusSeconds(onlineThresholdMinutes * 60L);
        return avatars.values().stream()
            .filter(a -> !a.deviceId().equals(currentDeviceId))
            .anyMatch(a -> a.lastSeen().isAfter(threshold));
    }

    /**
     * 获取设备类型
     */
    private PerceptionSystem.DeviceType getDeviceType() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("windows")) {
            return PerceptionSystem.DeviceType.PC;
        } else if (os.contains("mac")) {
            return PerceptionSystem.DeviceType.PHONE; // Mac could be laptop
        } else if (os.contains("linux")) {
            return PerceptionSystem.DeviceType.CLOUD; // Linux server likely cloud
        }
        return PerceptionSystem.DeviceType.PC;
    }

    /**
     * 持久化分身列表到文件
     */
    private void saveAvatars() {
        try {
            Path dir = Paths.get(AVATARS_FILE).getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            objectMapper.writeValue(Paths.get(AVATARS_FILE).toFile(), avatars.values());
            logger.debug("Saved {} avatars to {}", avatars.size(), AVATARS_FILE);
        } catch (IOException e) {
            logger.error("Failed to save avatars", e);
        }
    }

    /**
     * 从文件加载分身列表
     */
    @SuppressWarnings("unchecked")
    private void loadAvatars() {
        try {
            Path path = Paths.get(AVATARS_FILE);
            if (Files.exists(path)) {
                List<SelfModel.Avatar> loaded = objectMapper.readValue(path.toFile(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SelfModel.Avatar.class));
                for (SelfModel.Avatar avatar : loaded) {
                    avatars.put(avatar.deviceId(), avatar);
                }
                logger.info("Loaded {} avatars from {}", avatars.size(), AVATARS_FILE);
            }
        } catch (IOException e) {
            logger.warn("Failed to load avatars, starting fresh", e);
        }
    }

    // UUID import would be needed - using simple implementation
    private static class UUID {
        public static String randomUUID() {
            return java.util.UUID.randomUUID().toString();
        }
    }
}
