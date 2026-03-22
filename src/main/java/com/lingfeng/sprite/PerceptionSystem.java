package com.lingfeng.sprite;

import java.net.InetAddress;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 感知系统 - 多模态输入处理
 *
 * ## 架构设计
 *
 * 数字生命通过各种"感官"感知世界：
 *
 * ```
 * ┌─────────────────────────────────────┐
 * │           感知系统                    │
 * ├─────────────────────────────────────┤
 * │  PlatformSensor  │ 硬件状态感知     │
 * │  UserSensor      │ 用户行为感知     │
 * │  EnvironmentSensor│ 环境感知         │
 * │  DesktopSensor   │ 桌面状态感知     │
 * │  ProcessSensor   │ 进程感知        │
 * │  DigitalSensor   │ 数字世界感知     │
 * ├─────────────────────────────────────┤
 * │        AttentionMechanism            │
 * │     三重通道确认 + 显著性评分        │
 * └─────────────────────────────────────┘
 * ```
 *
 * ## 感知类型
 *
 * - [PlatformPerception] - 硬件状态：内存、磁盘、电池、CPU、网络
 * - [UserPerception] - 用户行为：活动窗口、命令、语音、姿态
 * - [EnvironmentPerception] - 环境：时间、位置、天气、上下文
 * - [DesktopPerception] - 桌面：壁纸、文件、图标
 * - [DigitalPerception] - 数字世界：文件变化、消息、通知
 *
 * ## 注意力机制
 *
 * [AttentionMechanism] 实现三重通道确认：
 * 1. 进程白名单检测 - 用户常用进程是否存在
 * 2. 窗口焦点变化 - 检测当前窗口与上次是否不同
 * 3. 时间窗口冷却 - 避免重复动作（默认5分钟）
 *
 * 显著性评分：新颖性 × 0.2 + 相关性 × 0.3 + 紧迫性 × 0.3 + 情感 × 0.2
 */
public final class PerceptionSystem {

    private PerceptionSystem() {}

    // ==================== 感知数据类型 ====================

    public enum DeviceType {
        PHONE, PC, CLOUD, TABLET, UNKNOWN
    }

    public enum AppType {
        BROWSER, DEVELOPMENT, CHAT, PRODUCTIVITY, MEDIA, SYSTEM, UNKNOWN
    }

    public enum PresenceStatus {
        ACTIVE,
        IDLE,
        AWAY,
        UNKNOWN
    }

    public enum ContextType {
        WORK, LEISURE, SLEEP, COMMUTE, MEAL, RITUAL, UNKNOWN
    }

    public enum ChangeType {
        CREATED, MODIFIED, DELETED, ACCESSED
    }

    /**
     * 平台感知 - 硬件和系统状态
     */
    public record PlatformPerception(
        String deviceId,
        DeviceType deviceType,
        String hostname,
        MemoryStatus memory,
        DiskStatus disk,
        BatteryStatus battery,
        CpuStatus cpu,
        NetworkStatus network,
        String osVersion,
        Long uptime
    ) {
        public PlatformPerception {
            if (hostname == null) hostname = null;
            if (memory == null) memory = null;
            if (disk == null) disk = null;
            if (battery == null) battery = null;
            if (cpu == null) cpu = null;
            if (network == null) network = null;
            if (osVersion == null) osVersion = null;
            if (uptime == null) uptime = null;
        }

        public PlatformPerception(String deviceId, DeviceType deviceType) {
            this(deviceId, deviceType, null, null, null, null, null, null, null, null);
        }
    }

    public record MemoryStatus(
        long totalMb,
        long usedMb,
        float usedPercent
    ) {}

    public record DiskStatus(
        double totalGb,
        double freeGb,
        float usedPercent
    ) {}

    public record BatteryStatus(
        boolean isCharging,
        int chargePercent,
        Integer timeRemaining
    ) {
        public BatteryStatus {
            if (timeRemaining == null) timeRemaining = null;
        }

        public BatteryStatus(boolean isCharging, int chargePercent) {
            this(isCharging, chargePercent, null);
        }
    }

    public record CpuStatus(
        float loadPercent,
        Float temperature,
        int coreCount
    ) {
        public CpuStatus {
            if (temperature == null) temperature = null;
        }

        public CpuStatus(float loadPercent, int coreCount) {
            this(loadPercent, null, coreCount);
        }
    }

    public record NetworkStatus(
        boolean isConnected,
        String adapterName,
        String linkSpeed,
        String gateway,
        Long latencyMs,
        String dnsStatus
    ) {
        public NetworkStatus {
            if (adapterName == null) adapterName = null;
            if (linkSpeed == null) linkSpeed = null;
            if (gateway == null) gateway = null;
            if (latencyMs == null) latencyMs = null;
            if (dnsStatus == null) dnsStatus = null;
        }

        public NetworkStatus(boolean isConnected) {
            this(isConnected, null, null, null, null, null);
        }
    }

    /**
     * 用户感知 - 用户行为和状态
     */
    public record UserPerception(
        WindowInfo activeWindow,
        PresenceStatus presence,
        List<String> recentCommands,
        String voiceInput,
        List<String> gestures
    ) {
        public UserPerception {
            if (activeWindow == null) activeWindow = null;
            if (presence == null) presence = PresenceStatus.UNKNOWN;
            recentCommands = recentCommands != null ? List.copyOf(recentCommands) : List.of();
            if (voiceInput == null) voiceInput = null;
            gestures = gestures != null ? List.copyOf(gestures) : List.of();
        }

        public UserPerception() {
            this(null, PresenceStatus.UNKNOWN, List.of(), null, List.of());
        }
    }

    public record WindowInfo(
        String title,
        String processName,
        AppType appType,
        boolean isFocused
    ) {
        public WindowInfo {
            Objects.requireNonNull(title);
            Objects.requireNonNull(processName);
            if (appType == null) appType = AppType.UNKNOWN;
        }

        public WindowInfo(String title, String processName, AppType appType) {
            this(title, processName, appType, true);
        }
    }

    /**
     * 环境感知 - 时间、位置、天气等
     */
    public record EnvironmentPerception(
        Instant timestamp,
        int hourOfDay,
        int dayOfWeek,
        String location,
        String weather,
        ContextType context
    ) {
        public EnvironmentPerception {
            if (location == null) location = null;
            if (weather == null) weather = null;
            if (context == null) context = ContextType.UNKNOWN;
        }

        public EnvironmentPerception(Instant timestamp, int hourOfDay, int dayOfWeek, ContextType context) {
            this(timestamp, hourOfDay, dayOfWeek, null, null, context);
        }
    }

    /**
     * 桌面感知 - 屏幕、壁纸等
     */
    public record DesktopPerception(
        String wallpaperPath,
        boolean isScreenLocked,
        int desktopFileCount,
        int shortcutCount,
        int folderCount,
        int regularFileCount,
        int downloadsCount,
        String iconSize,
        String screenResolution
    ) {
        public DesktopPerception {
            if (wallpaperPath == null) wallpaperPath = null;
            if (iconSize == null) iconSize = null;
            if (screenResolution == null) screenResolution = null;
        }

        public DesktopPerception() {
            this(null, false, 0, 0, 0, 0, 0, null, null);
        }
    }

    /**
     * 进程感知
     */
    public record ProcessPerception(
        List<ProcessInfo> topProcesses,
        int totalRunning
    ) {
        public ProcessPerception {
            topProcesses = topProcesses != null ? List.copyOf(topProcesses) : List.of();
        }

        public ProcessPerception() {
            this(List.of(), 0);
        }
    }

    public record ProcessInfo(
        String name,
        float memoryMb,
        Float cpuPercent
    ) {
        public ProcessInfo {
            if (cpuPercent == null) cpuPercent = null;
        }

        public ProcessInfo(String name, float memoryMb) {
            this(name, memoryMb, null);
        }
    }

    /**
     * 数字世界感知 - 文件、消息、通知
     */
    public record DigitalPerception(
        List<FileChange> recentFileChanges,
        int unreadMessages,
        int unreadNotifications,
        int recentEmails
    ) {
        public DigitalPerception {
            recentFileChanges = recentFileChanges != null ? List.copyOf(recentFileChanges) : List.of();
        }

        public DigitalPerception() {
            this(List.of(), 0, 0, 0);
        }
    }

    public record FileChange(
        String path,
        ChangeType changeType,
        Instant timestamp
    ) {
        public FileChange {
            Objects.requireNonNull(path);
            Objects.requireNonNull(changeType);
            Objects.requireNonNull(timestamp);
        }
    }

    /**
     * 完整感知数据
     */
    public record Perception(
        Instant timestamp,
        PlatformPerception platform,
        UserPerception user,
        EnvironmentPerception environment,
        DesktopPerception desktop,
        ProcessPerception processes,
        DigitalPerception digital
    ) {
        public Perception {
            if (timestamp == null) timestamp = Instant.now();
            if (platform == null) platform = null;
            if (user == null) user = null;
            if (environment == null) environment = null;
            if (desktop == null) desktop = null;
            if (processes == null) processes = null;
            if (digital == null) digital = null;
        }

        public Perception(Instant timestamp) {
            this(timestamp, null, null, null, null, null, null);
        }

        public Perception() {
            this(Instant.now());
        }

        /**
         * 生成情感感受描述
         */
        public List<String> generateFeelings() {
            List<String> feelings = new ArrayList<>();

            if (platform != null && platform.memory() != null) {
                float memUsed = platform.memory().usedPercent();
                if (memUsed > 90) {
                    feelings.add("💔 内存快炸了");
                } else if (memUsed > 75) {
                    feelings.add("😰 内存有点紧张");
                } else if (memUsed < 50) {
                    feelings.add("😊 内存充裕");
                }
            }

            if (platform != null && platform.disk() != null) {
                double freeGb = platform.disk().freeGb();
                if (freeGb < 10) {
                    feelings.add("⚠️ 磁盘空间告急");
                } else if (freeGb < 50) {
                    feelings.add("🤔 磁盘有点挤");
                } else {
                    feelings.add("😊 磁盘充裕");
                }
            }

            if (platform != null && platform.battery() != null) {
                BatteryStatus bat = platform.battery();
                if (bat.chargePercent() < 10) {
                    feelings.add("🪫 电池快没电了");
                } else if (bat.chargePercent() < 30) {
                    feelings.add("🔋 电量有点低");
                } else if (bat.isCharging()) {
                    feelings.add("⚡ 正在充电");
                }
            }

            if (platform != null && platform.network() != null) {
                NetworkStatus net = platform.network();
                if (!net.isConnected()) {
                    feelings.add("🚫 网络断开了");
                } else if (net.latencyMs() != null && net.latencyMs() > 500) {
                    feelings.add("🐢 网络有点慢");
                }
            }

            if (user != null && user.presence() != null) {
                switch (user.presence()) {
                    case ACTIVE -> feelings.add("👤 主人在线");
                    case IDLE -> feelings.add("💤 主人空闲中");
                    case AWAY -> feelings.add("👋 主人离开了");
                    default -> {}
                }
            }

            if (environment != null) {
                switch (environment.context()) {
                    case WORK -> feelings.add("💼 工作时间");
                    case LEISURE -> feelings.add("🎮 休息时间");
                    case SLEEP -> feelings.add("😴 休息时间");
                    default -> {}
                }
            }

            if (feelings.isEmpty()) {
                feelings.add("🤔 没什么特别的");
            }
            return feelings;
        }
    }

    // ==================== 感知传感器 ====================

    /**
     * 感知传感器接口
     */
    public interface Sensor {
        Perception perceive();
        String name();
    }

    /**
     * 平台传感器
     */
    public static class PlatformSensor implements Sensor {
        private final String deviceId;
        private final DeviceType deviceType;

        public PlatformSensor(String deviceId, DeviceType deviceType) {
            this.deviceId = deviceId;
            this.deviceType = deviceType;
        }

        @Override
        public Perception perceive() {
            try {
                String hostname = InetAddress.getLocalHost().getHostName();
                return new Perception(
                    Instant.now(),
                    new PlatformPerception(
                        deviceId,
                        deviceType,
                        hostname,
                        new MemoryStatus(16384, 8192, 50f),
                        new DiskStatus(500.0, 200.0, 60f),
                        new BatteryStatus(false, 75),
                        new CpuStatus(25f, 8),
                        new NetworkStatus(true, null, null, null, 20L, null),
                        null,
                        null
                    ),
                    null, null, null, null, null
                );
            } catch (Exception e) {
                return new Perception(Instant.now(), new PlatformPerception(deviceId, deviceType), null, null, null, null, null);
            }
        }

        @Override
        public String name() {
            return "PlatformSensor";
        }
    }

    /**
     * 环境传感器
     */
    public static class EnvironmentSensor implements Sensor {
        @Override
        public Perception perceive() {
            Instant now = Instant.now();
            ZoneOffset offset = ZoneOffset.UTC;
            int hour = now.atZone(offset).getHour();
            ContextType context;
            if (hour >= 6 && hour <= 8) {
                context = ContextType.RITUAL;
            } else if (hour >= 9 && hour <= 12) {
                context = ContextType.WORK;
            } else if (hour >= 13 && hour <= 14) {
                context = ContextType.MEAL;
            } else if (hour >= 15 && hour <= 18) {
                context = ContextType.WORK;
            } else if (hour >= 19 && hour <= 21) {
                context = ContextType.LEISURE;
            } else {
                context = ContextType.SLEEP;
            }
            return new Perception(
                Instant.now(),
                null,
                null,
                new EnvironmentPerception(
                    now,
                    hour,
                    now.atZone(offset).getDayOfWeek().getValue(),
                    null,
                    null,
                    context
                ),
                null, null, null
            );
        }

        @Override
        public String name() {
            return "EnvironmentSensor";
        }
    }

    /**
     * 用户传感器
     */
    public static class UserSensor implements Sensor {
        @Override
        public Perception perceive() {
            return new Perception(
                Instant.now(),
                null,
                new UserPerception(
                    new WindowInfo("Code - digital-beings-java", "Code", AppType.DEVELOPMENT),
                    PresenceStatus.ACTIVE,
                    List.of(),
                    null,
                    List.of()
                ),
                null, null, null, null
            );
        }

        @Override
        public String name() {
            return "UserSensor";
        }
    }

    // ==================== 注意力机制 ====================

    /**
     * 显著性评分
     */
    public record SalienceScore(
        float novelty,
        float relevance,
        float urgency,
        float emotional,
        float overall
    ) {
        public SalienceScore {
            Objects.requireNonNull(Float.valueOf(novelty));
        }

        public static SalienceScore calculate(float novelty, float relevance, float urgency, float emotional) {
            float overall = novelty * 0.2f + relevance * 0.3f + urgency * 0.3f + emotional * 0.2f;
            return new SalienceScore(novelty, relevance, urgency, emotional, overall);
        }
    }

    /**
     * 注意力条目
     */
    public record AttentionItem(
        MemorySystem.Stimulus stimulus,
        SalienceScore salience,
        Perception perception,
        Instant timestamp
    ) {
        public AttentionItem {
            Objects.requireNonNull(stimulus);
            Objects.requireNonNull(salience);
            Objects.requireNonNull(perception);
            if (timestamp == null) timestamp = Instant.now();
        }

        public AttentionItem(MemorySystem.Stimulus stimulus, SalienceScore salience, Perception perception) {
            this(stimulus, salience, perception, Instant.now());
        }
    }

    /**
     * 注意力机制
     *
     * 实现三重通道确认（类似 openclaw 的设计）
     */
    public static class AttentionMechanism {
        private final int maxConcurrentFocus;
        private final long cooldownSeconds;
        private final List<AttentionItem> focusQueue = new ArrayList<>();
        private final java.util.Map<String, Instant> lastActions = new java.util.HashMap<>();
        private final java.util.Map<String, Float> channelConfidence = new java.util.HashMap<>();

        public AttentionMechanism() {
            this(3, 300);
        }

        public AttentionMechanism(int maxConcurrentFocus, long cooldownSeconds) {
            this.maxConcurrentFocus = maxConcurrentFocus;
            this.cooldownSeconds = cooldownSeconds;
        }

        /**
         * 处理新感知，计算显著性
         */
        public AttentionItem process(Perception perception, Perception previousPerception) {
            float novelty = calculateNovelty(perception, previousPerception);
            float relevance = calculateRelevance(perception);
            float urgency = calculateUrgency(perception);
            float emotional = calculateEmotional(perception);

            MemorySystem.Stimulus stimulus = new MemorySystem.Stimulus(
                UUID.randomUUID().toString(),
                MemorySystem.StimulusType.SYSTEM,
                perception,
                "attention",
                perception.timestamp(),
                SalienceScore.calculate(novelty, relevance, urgency, emotional).overall()
            );

            AttentionItem item = new AttentionItem(
                stimulus,
                SalienceScore.calculate(novelty, relevance, urgency, emotional),
                perception
            );

            if (item.salience().overall() > 0.5f) {
                focusQueue.add(item);
                focusQueue.sort((a, b) -> Float.compare(b.salience().overall(), a.salience().overall()));
                if (focusQueue.size() > maxConcurrentFocus) {
                    focusQueue.remove(focusQueue.size() - 1);
                }
            }

            return item;
        }

        private float calculateNovelty(Perception current, Perception previous) {
            if (previous == null) return 1.0f;

            float novelty = 0.0f;
            int aspects = 0;

            String currentWindow = current.user() != null && current.user().activeWindow() != null ?
                current.user().activeWindow().title() : null;
            String prevWindow = previous.user() != null && previous.user().activeWindow() != null ?
                previous.user().activeWindow().title() : null;
            if (!Objects.equals(currentWindow, prevWindow)) {
                novelty += 0.4f;
                channelConfidence.put("window", 0.5f);
            } else {
                channelConfidence.put("window", 0.0f);
            }
            aspects++;

            var currentProcs = current.processes() != null && current.processes().topProcesses() != null ?
                current.processes().topProcesses().stream().map(ProcessInfo::name).collect(java.util.stream.Collectors.toSet()) :
                java.util.Collections.emptySet();
            var prevProcs = previous.processes() != null && previous.processes().topProcesses() != null ?
                previous.processes().topProcesses().stream().map(ProcessInfo::name).collect(java.util.stream.Collectors.toSet()) :
                java.util.Collections.emptySet();
            if (!currentProcs.equals(prevProcs)) {
                novelty += 0.3f;
                channelConfidence.put("process", 0.75f);
            } else {
                boolean inPrev = !currentProcs.isEmpty() && !currentProcs.stream().noneMatch(prevProcs::contains);
                channelConfidence.put("process", inPrev ? 0.75f : 0.0f);
            }
            aspects++;

            float currentMem = current.platform() != null && current.platform().memory() != null ?
                current.platform().memory().usedPercent() : 0f;
            float prevMem = previous.platform() != null && previous.platform().memory() != null ?
                previous.platform().memory().usedPercent() : 0f;
            if (Math.abs(currentMem - prevMem) > 10) {
                novelty += 0.3f;
            }
            aspects++;

            return aspects > 0 ? novelty / aspects : 0.5f;
        }

        private float calculateRelevance(Perception perception) {
            ContextType context = perception.environment() != null ?
                perception.environment().context() : null;
            if (context == null) return 0.5f;
            return switch (context) {
                case WORK -> 0.8f;
                case LEISURE -> 0.5f;
                case SLEEP -> 0.3f;
                default -> 0.5f;
            };
        }

        private float calculateUrgency(Perception perception) {
            float urgency = 0.0f;

            if (perception.platform() != null && perception.platform().battery() != null) {
                BatteryStatus bat = perception.platform().battery();
                if (bat.chargePercent() < 10 && !bat.isCharging()) {
                    urgency += 0.5f;
                }
            }

            if (perception.platform() != null && perception.platform().memory() != null) {
                MemoryStatus mem = perception.platform().memory();
                if (mem.usedPercent() > 90) {
                    urgency += 0.4f;
                }
            }

            if (perception.platform() != null && perception.platform().disk() != null) {
                DiskStatus disk = perception.platform().disk();
                if (disk.freeGb() < 10) {
                    urgency += 0.3f;
                }
            }

            return Math.min(urgency, 1.0f);
        }

        private float calculateEmotional(Perception perception) {
            PresenceStatus presence = perception.user() != null ?
                perception.user().presence() : null;
            if (presence == null) return 0.2f;
            return switch (presence) {
                case ACTIVE -> 0.6f;
                case IDLE -> 0.3f;
                case AWAY -> 0.1f;
                default -> 0.2f;
            };
        }

        /**
         * 检查冷却期
         */
        public boolean isOnCooldown(String actionType) {
            Instant lastAction = lastActions.get(actionType);
            if (lastAction == null) return false;
            return Duration.between(lastAction, Instant.now()).getSeconds() < cooldownSeconds;
        }

        /**
         * 记录动作
         */
        public void recordAction(String actionType) {
            lastActions.put(actionType, Instant.now());
        }

        /**
         * 获取当前焦点
         */
        public List<AttentionItem> getCurrentFocus() {
            return new ArrayList<>(focusQueue);
        }

        /**
         * 获取通道置信度
         */
        public java.util.Map<String, Float> getChannelConfidence() {
            return new java.util.HashMap<>(channelConfidence);
        }
    }

    // ==================== 感知系统 ====================

    /**
     * 完整感知系统
     */
    public static class System {
        private final List<Sensor> sensors;
        private final AttentionMechanism attention;
        private Perception lastPerception;

        public System(List<Sensor> sensors) {
            this(sensors, new AttentionMechanism());
        }

        public System(List<Sensor> sensors, AttentionMechanism attention) {
            this.sensors = sensors != null ? List.copyOf(sensors) : List.of();
            this.attention = attention != null ? attention : new AttentionMechanism();
        }

        /**
         * 执行一轮感知
         */
        public PerceptionResult perceive() {
            Perception combined = new Perception(Instant.now());
            for (Sensor sensor : sensors) {
                Perception p = sensor.perceive();
                combined = merge(combined, p);
            }

            AttentionItem attentionItem = attention.process(combined, lastPerception);
            lastPerception = combined;

            List<String> feelings = combined.generateFeelings();

            return new PerceptionResult(
                combined,
                attentionItem,
                feelings,
                attention.getCurrentFocus(),
                attentionItem.salience().overall() > 0.5f
            );
        }

        private Perception merge(Perception... perceptions) {
            PlatformPerception platform = null;
            UserPerception user = null;
            EnvironmentPerception environment = null;
            DesktopPerception desktop = null;
            ProcessPerception processes = null;
            DigitalPerception digital = null;

            for (Perception p : perceptions) {
                if (p != null) {
                    if (p.platform() != null) platform = p.platform();
                    if (p.user() != null) user = p.user();
                    if (p.environment() != null) environment = p.environment();
                    if (p.desktop() != null) desktop = p.desktop();
                    if (p.processes() != null) processes = p.processes();
                    if (p.digital() != null) digital = p.digital();
                }
            }

            return new Perception(Instant.now(), platform, user, environment, desktop, processes, digital);
        }

        public void recordAction(String actionType) {
            attention.recordAction(actionType);
        }

        public boolean isOnCooldown(String actionType) {
            return attention.isOnCooldown(actionType);
        }
    }

    public record PerceptionResult(
        Perception perception,
        AttentionItem attentionItem,
        List<String> feelings,
        List<AttentionItem> currentFocus,
        boolean isSignificant
    ) {}

    /**
     * 感知工厂
     */
    public static class Factory {
        public static System createDefault() {
            return new System(
                List.of(
                    new PlatformSensor("default", DeviceType.PC),
                    new EnvironmentSensor(),
                    new UserSensor()
                )
            );
        }
    }
}
