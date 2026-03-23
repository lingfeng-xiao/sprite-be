package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * S9-4: 多设备协同服务
 *
 * 支持多设备间的状态同步和协调：
 * - 设备注册和状态跟踪
 * - 设备间消息传递
 * - 状态同步
 * - 协调决策
 */
public class MultiDeviceCoordinationService {

    private static final Logger logger = LoggerFactory.getLogger(MultiDeviceCoordinationService.class);

    private final Map<String, DeviceInfo> registeredDevices = new ConcurrentHashMap<>();
    private final List<CoordinationMessage> messageHistory = new ArrayList<>();
    private final String localDeviceId;
    private static final int MAX_MESSAGE_HISTORY = 100;

    /**
     * 设备信息
     */
    public record DeviceInfo(
        String deviceId,
        String deviceName,
        DeviceType deviceType,
        Instant lastSeen,
        DeviceState state,
        String ipAddress,
        int capabilityScore
    ) {
        public DeviceInfo {
            if (deviceId == null) deviceId = "";
            if (deviceName == null) deviceName = "Unknown";
            if (deviceType == null) deviceType = DeviceType.UNKNOWN;
            if (lastSeen == null) lastSeen = Instant.now();
            if (state == null) state = DeviceState.UNKNOWN;
            if (ipAddress == null) ipAddress = "";
        }

        public DeviceInfo withLastSeen(Instant newLastSeen) {
            return new DeviceInfo(deviceId, deviceName, deviceType, newLastSeen, state, ipAddress, capabilityScore);
        }

        public DeviceInfo withState(DeviceState newState) {
            return new DeviceInfo(deviceId, deviceName, deviceType, lastSeen, newState, ipAddress, capabilityScore);
        }
    }

    /**
     * 设备类型
     */
    public enum DeviceType {
        PHONE,
        TABLET,
        LAPTOP,
        DESKTOP,
        SERVER,
        CLOUD,
        UNKNOWN
    }

    /**
     * 设备状态
     */
    public enum DeviceState {
        ONLINE,
        OFFLINE,
        AWAY,
        BUSY,
        SYNCING,
        UNKNOWN
    }

    /**
     * 协调消息
     */
    public record CoordinationMessage(
        String messageId,
        String sourceDeviceId,
        String targetDeviceId,
        MessageType type,
        String content,
        Instant timestamp,
        boolean delivered
    ) {
        public CoordinationMessage {
            if (messageId == null) messageId = "";
            if (sourceDeviceId == null) sourceDeviceId = "";
            if (targetDeviceId == null) targetDeviceId = "";
            if (type == null) type = MessageType.INFO;
            if (content == null) content = "";
            if (timestamp == null) timestamp = Instant.now();
        }

        public CoordinationMessage withDelivered(boolean newDelivered) {
            return new CoordinationMessage(messageId, sourceDeviceId, targetDeviceId, type, content, timestamp, newDelivered);
        }
    }

    /**
     * 消息类型
     */
    public enum MessageType {
        INFO,           // 信息
        STATE_SYNC,     // 状态同步
        REQUEST,        // 请求
        RESPONSE,       // 响应
        NOTIFICATION,   // 通知
        SYNC_TRIGGER    // 同步触发
    }

    public MultiDeviceCoordinationService(String localDeviceId) {
        this.localDeviceId = localDeviceId != null ? localDeviceId : "local";
    }

    public MultiDeviceCoordinationService() {
        this(null);
    }

    // ==================== 设备管理 ====================

    /**
     * 注册设备
     */
    public void registerDevice(String deviceId, String deviceName, DeviceType deviceType, String ipAddress) {
        if (deviceId == null || deviceId.equals(localDeviceId)) {
            return;  // 不注册本地设备
        }

        DeviceInfo device = new DeviceInfo(
            deviceId,
            deviceName,
            deviceType,
            Instant.now(),
            DeviceState.ONLINE,
            ipAddress,
            100  // 默认能力分数
        );

        registeredDevices.put(deviceId, device);
        logger.info("Device registered: {} ({})", deviceName, deviceType);
    }

    /**
     * 注销设备
     */
    public void unregisterDevice(String deviceId) {
        DeviceInfo removed = registeredDevices.remove(deviceId);
        if (removed != null) {
            logger.info("Device unregistered: {}", deviceId);
        }
    }

    /**
     * 更新设备状态
     */
    public void updateDeviceState(String deviceId, DeviceState state) {
        DeviceInfo current = registeredDevices.get(deviceId);
        if (current != null) {
            registeredDevices.put(deviceId, current.withState(state).withLastSeen(Instant.now()));
        }
    }

    /**
     * 获取设备信息
     */
    public DeviceInfo getDevice(String deviceId) {
        return registeredDevices.get(deviceId);
    }

    /**
     * 获取所有设备
     */
    public List<DeviceInfo> getAllDevices() {
        return new ArrayList<>(registeredDevices.values());
    }

    /**
     * 获取在线设备
     */
    public List<DeviceInfo> getOnlineDevices() {
        return registeredDevices.values().stream()
            .filter(d -> d.state() == DeviceState.ONLINE)
            .toList();
    }

    /**
     * 获取本地设备ID
     */
    public String getLocalDeviceId() {
        return localDeviceId;
    }

    // ==================== 消息传递 ====================

    /**
     * 发送消息到指定设备
     */
    public CoordinationMessage sendMessage(String targetDeviceId, MessageType type, String content) {
        String messageId = "msg-" + System.currentTimeMillis() + "-" + Math.random();

        CoordinationMessage message = new CoordinationMessage(
            messageId,
            localDeviceId,
            targetDeviceId,
            type,
            content,
            Instant.now(),
            false  // 未送达
        );

        messageHistory.add(message);

        // 如果目标设备是本地已知的设备，标记为已送达
        if (registeredDevices.containsKey(targetDeviceId)) {
            markAsDelivered(messageId);
        }

        // 清理过期的消息
        pruneMessageHistory();

        logger.debug("Message sent to {}: {} ({})", targetDeviceId, type, content);
        return message;
    }

    /**
     * 广播消息到所有在线设备
     */
    public List<CoordinationMessage> broadcast(MessageType type, String content) {
        List<CoordinationMessage> messages = new ArrayList<>();
        for (DeviceInfo device : getOnlineDevices()) {
            messages.add(sendMessage(device.deviceId(), type, content));
        }
        return messages;
    }

    /**
     * 标记消息已送达
     */
    public void markAsDelivered(String messageId) {
        for (int i = 0; i < messageHistory.size(); i++) {
            if (messageHistory.get(i).messageId().equals(messageId)) {
                messageHistory.set(i, messageHistory.get(i).withDelivered(true));
                break;
            }
        }
    }

    /**
     * 获取消息历史
     */
    public List<CoordinationMessage> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }

    /**
     * 获取发给指定设备的消息
     */
    public List<CoordinationMessage> getMessagesForDevice(String deviceId) {
        return messageHistory.stream()
            .filter(m -> m.targetDeviceId().equals(deviceId) || m.sourceDeviceId().equals(deviceId))
            .toList();
    }

    // ==================== 状态同步 ====================

    /**
     * 触发与其他设备的状态同步
     */
    public List<CoordinationMessage> triggerStateSync() {
        List<CoordinationMessage> messages = new ArrayList<>();
        for (DeviceInfo device : getOnlineDevices()) {
            messages.add(sendMessage(device.deviceId(), MessageType.SYNC_TRIGGER, "sync-request"));
        }
        return messages;
    }

    /**
     * 获取协调状态摘要
     */
    public CoordinationStatus getStatus() {
        List<DeviceInfo> online = getOnlineDevices();
        long totalMessages = messageHistory.size();
        long undeliveredMessages = messageHistory.stream().filter(m -> !m.delivered()).count();

        return new CoordinationStatus(
            localDeviceId,
            registeredDevices.size(),
            online.size(),
            totalMessages,
            undeliveredMessages,
            Instant.now()
        );
    }

    /**
     * 协调状态
     */
    public record CoordinationStatus(
        String localDeviceId,
        int totalDevices,
        int onlineDevices,
        long totalMessages,
        long undeliveredMessages,
        Instant timestamp
    ) {}

    // ==================== 私有方法 ====================

    /**
     * 清理过期的消息
     */
    private void pruneMessageHistory() {
        while (messageHistory.size() > MAX_MESSAGE_HISTORY) {
            messageHistory.remove(0);
        }
    }

    /**
     * 更新所有设备的最后 seen 时间
     */
    public void updateAllDevicesLastSeen() {
        Instant now = Instant.now();
        for (DeviceInfo device : registeredDevices.values()) {
            registeredDevices.put(device.deviceId(), device.withLastSeen(now));
        }
    }
}
