package com.lingfeng.sprite.sensor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lingfeng.sprite.PerceptionSystem.Perception;
import com.lingfeng.sprite.PerceptionSystem.Sensor;

/**
 * S9-1/S15-3: 声音传感器
 *
 * 检测音频环境信息：
 * - 是否有声音播放
 * - 音频设备状态
 * - 环境声音级别（如果可用）
 * - S15-3: 说话者情绪指标
 * - S15-3: 活动上下文（基于环境声音模式）
 *
 * 注意：这是一个探索性传感器，完整的音频捕获需要平台特定的实现
 */
public class AudioSensor implements Sensor {

    private final String name;
    private final boolean isAvailable;
    private final AudioInfo cachedInfo;

    // S15-3: 情绪指标历史
    private final Map<String, Float> moodIndicators = new HashMap<>();

    // S15-3: 活动上下文检测结果
    private ActivityContext lastActivityContext = ActivityContext.UNKNOWN;

    /**
     * S15-3: 音频信息增强版
     */
    public record AudioInfo(
        Instant timestamp,
        boolean isPlaying,
        String audioDevice,
        float volumeLevel,
        String currentAudioApp,
        SoundContext soundContext,
        boolean isHeadphonesConnected,
        MoodIndicator moodIndicator,    // S15-3: 新增
        ActivityContext activityContext  // S15-3: 新增
    ) {
        public AudioInfo {
            if (timestamp == null) timestamp = Instant.now();
            if (audioDevice == null) audioDevice = "default";
            if (soundContext == null) soundContext = SoundContext.UNKNOWN;
            if (moodIndicator == null) moodIndicator = MoodIndicator.UNKNOWN;
            if (activityContext == null) activityContext = ActivityContext.UNKNOWN;
        }

        /**
         * S15-3: 简化构造器（向后兼容）
         */
        public AudioInfo(Instant timestamp, boolean isPlaying, String audioDevice,
                float volumeLevel, String currentAudioApp, SoundContext soundContext,
                boolean isHeadphonesConnected) {
            this(timestamp, isPlaying, audioDevice, volumeLevel, currentAudioApp,
                 soundContext, isHeadphonesConnected, MoodIndicator.UNKNOWN, ActivityContext.UNKNOWN);
        }
    }

    /**
     * S15-3: 声音上下文
     */
    public enum SoundContext {
        UNKNOWN,
        SILENT,         // 安静
        MUSIC,          // 音乐播放
        VIDEO,          // 视频播放
        VOICE_CALL,     // 语音通话
        NOTIFICATION,   // 通知声音
        TYPING,         // 键盘声
        AMBIENT,        // 环境声
        SPEECH          // S15-3: 语音/说话
    }

    /**
     * S15-3: 情绪指标
     */
    public enum MoodIndicator {
        UNKNOWN,
        CALM,           // 平静
        EXCITED,        // 兴奋
        TENSE,          // 紧张
        RELAXED,        // 放松
        FOCUSED,        // 专注
        FATIGUED        // 疲劳
    }

    /**
     * S15-3: 活动上下文
     */
    public enum ActivityContext {
        UNKNOWN,
        WORKING,        // 工作中
        COMMUTING,      // 通勤中
        EXERCISING,     // 锻炼中
        RESTING,        // 休息中
        SOCIALIZING,    // 社交中
        DINING,         // 用餐中
        ENTERTAINMENT   // 娱乐中
    }

    public AudioSensor() {
        this("AudioSensor");
    }

    public AudioSensor(String name) {
        this.name = name;
        this.cachedInfo = detectAudioInfo();
        this.isAvailable = cachedInfo != null;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Perception perceive() {
        // 返回null表示不支持，因为完整的AudioPerception需要修改 PerceptionSystem
        // 这个传感器作为独立的声音上下文检测器
        return null;
    }

    /**
     * 获取音频信息
     */
    public AudioInfo getAudioInfo() {
        return cachedInfo;
    }

    /**
     * S15-3: 检测说话者情绪指标
     * 基于音频特征（音量、语调变化）推断情绪状态
     * 注意：这是一个简化实现，实际需要音频分析
     */
    private MoodIndicator detectMoodIndicator(boolean isPlaying, float volumeLevel, SoundContext soundContext) {
        // S15-3: 基于当前音频上下文和音量推断情绪
        if (!isPlaying || soundContext == SoundContext.SILENT) {
            return MoodIndicator.CALM;
        }

        if (soundContext == SoundContext.MUSIC) {
            // 音乐类型情绪推断（简化）
            if (volumeLevel > 0.8f) {
                return MoodIndicator.EXCITED;
            } else if (volumeLevel < 0.3f) {
                return MoodIndicator.RELAXED;
            } else {
                return MoodIndicator.CALM;
            }
        }

        if (soundContext == SoundContext.VOICE_CALL) {
            // 通话可能表示社交或工作
            return MoodIndicator.FOCUSED;
        }

        if (soundContext == SoundContext.SPEECH) {
            // 说话可能表示社交或讨论
            return MoodIndicator.CALM;
        }

        return MoodIndicator.UNKNOWN;
    }

    /**
     * S15-3: 基于环境声音模式检测活动上下文
     * @param soundContext 声音上下文
     * @param isHeadphonesConnected 是否连接耳机
     * @param volumeLevel 音量级别
     * @return 活动上下文
     */
    private ActivityContext detectActivityContext(SoundContext soundContext,
                                                   boolean isHeadphonesConnected,
                                                   float volumeLevel) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.atZone(ZoneId.systemDefault()).getHour();

        // 基于时间的声音上下文组合判断
        if (soundContext == SoundContext.TYPING) {
            // 键盘声通常表示工作
            lastActivityContext = ActivityContext.WORKING;
            return ActivityContext.WORKING;
        }

        if (soundContext == SoundContext.VOICE_CALL) {
            // 通话可能表示工作或社交
            if (hour >= 9 && hour <= 18) {
                lastActivityContext = ActivityContext.WORKING;
            } else {
                lastActivityContext = ActivityContext.SOCIALIZING;
            }
            return lastActivityContext;
        }

        if (soundContext == SoundContext.MUSIC && isHeadphonesConnected) {
            // 使用耳机听音乐可能表示工作（专注）或锻炼
            if (volumeLevel > 0.7f) {
                return ActivityContext.EXERCISING;
            }
            return ActivityContext.WORKING;
        }

        if (soundContext == SoundContext.MUSIC && !isHeadphonesConnected) {
            // 外放音乐可能表示休息或娱乐
            if (hour >= 19 && hour <= 22) {
                return ActivityContext.ENTERTAINMENT;
            }
            return ActivityContext.RESTING;
        }

        if (soundContext == SoundContext.AMBIENT) {
            // 环境声：通勤或休息
            if (hour >= 8 && hour <= 9 || hour >= 18 && hour <= 19) {
                return ActivityContext.COMMUTING;
            }
            return ActivityContext.RESTING;
        }

        return ActivityContext.UNKNOWN;
    }

    /**
     * S15-3: 基于历史情绪指标推断当前情绪
     * @return 推断的情绪指标
     */
    public MoodIndicator inferMoodFromHistory() {
        if (moodIndicators.isEmpty()) {
            return MoodIndicator.UNKNOWN;
        }

        // 计算各情绪指标的平均值
        Map<MoodIndicator, List<Float>> moodCounts = new HashMap<>();
        for (Map.Entry<String, Float> entry : moodIndicators.entrySet()) {
            // 从缓存键解析情绪
            try {
                MoodIndicator mood = MoodIndicator.valueOf(entry.getKey());
                moodCounts.computeIfAbsent(mood, k -> new ArrayList<>()).add(entry.getValue());
            } catch (IllegalArgumentException ignored) {}
        }

        // 返回最常见的情绪
        MoodIndicator mostCommon = MoodIndicator.UNKNOWN;
        int maxCount = 0;
        for (Map.Entry<MoodIndicator, List<Float>> entry : moodCounts.entrySet()) {
            if (entry.getValue().size() > maxCount) {
                maxCount = entry.getValue().size();
                mostCommon = entry.getKey();
            }
        }

        return mostCommon;
    }

    /**
     * S15-3: 记录情绪观察（用于历史学习）
     */
    public void recordMoodObservation(MoodIndicator mood, float confidence) {
        if (mood != MoodIndicator.UNKNOWN) {
            moodIndicators.put(mood.name(), confidence);
            // 限制缓存大小
            if (moodIndicators.size() > 100) {
                moodIndicators.clear();
            }
        }
    }

    /**
     * 检测音频信息
     */
    private AudioInfo detectAudioInfo() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            boolean isPlaying = false;
            String audioDevice = "default";
            float volumeLevel = 0.5f;
            String currentAudioApp = null;
            SoundContext soundContext = SoundContext.SILENT;
            boolean isHeadphonesConnected = false;

            if (os.contains("windows")) {
                isPlaying = detectWindowsAudio();
                audioDevice = "Windows Audio";
                isHeadphonesConnected = checkWindowsHeadphones();
            } else if (os.contains("linux")) {
                isPlaying = detectLinuxAudio();
                audioDevice = "PulseAudio/PipeWire";
                isHeadphonesConnected = checkLinuxHeadphones();
            } else if (os.contains("mac") || os.contains("darwin")) {
                isPlaying = detectMacAudio();
                audioDevice = "CoreAudio";
                isHeadphonesConnected = checkMacHeadphones();
            }

            // 基于检测结果推断声音上下文
            soundContext = inferSoundContext(isPlaying, currentAudioApp);

            // S15-3: 检测情绪指标
            MoodIndicator moodIndicator = detectMoodIndicator(isPlaying, volumeLevel, soundContext);
            recordMoodObservation(moodIndicator, 0.7f);

            // S15-3: 检测活动上下文
            ActivityContext activityContext = detectActivityContext(soundContext, isHeadphonesConnected, volumeLevel);

            return new AudioInfo(
                Instant.now(),
                isPlaying,
                audioDevice,
                volumeLevel,
                currentAudioApp,
                soundContext,
                isHeadphonesConnected,
                moodIndicator,
                activityContext
            );
        } catch (Exception e) {
            return new AudioInfo(
                Instant.now(),
                false,
                "unavailable",
                0f,
                null,
                SoundContext.UNKNOWN,
                false,
                MoodIndicator.UNKNOWN,
                ActivityContext.UNKNOWN
            );
        }
    }

    /**
     * 推断声音上下文
     */
    private SoundContext inferSoundContext(boolean isPlaying, String currentApp) {
        if (!isPlaying) {
            return SoundContext.SILENT;
        }

        if (currentApp != null) {
            currentApp = currentApp.toLowerCase();
            if (currentApp.contains("spotify") || currentApp.contains("music") || currentApp.contains("player")) {
                return SoundContext.MUSIC;
            }
            if (currentApp.contains("zoom") || currentApp.contains("teams") || currentApp.contains("wechat")) {
                return SoundContext.VOICE_CALL;
            }
            if (currentApp.contains("youtube") || currentApp.contains("video") || currentApp.contains("player")) {
                return SoundContext.VIDEO;
            }
        }

        return SoundContext.AMBIENT;
    }

    private boolean detectWindowsAudio() {
        try {
            // Windows上检测音频播放状态需要额外的API，这里返回false作为占位
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean detectLinuxAudio() {
        try {
            // 检查PulseAudio或PipeWire状态
            ProcessBuilder pb = new ProcessBuilder("pactl", "info");
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean detectMacAudio() {
        try {
            ProcessBuilder pb = new ProcessBuilder("pgrep", "-a", "coreaudiod");
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkWindowsHeadphones() {
        try {
            // 检测Windows音频设备
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkLinuxHeadphones() {
        try {
            ProcessBuilder pb = new ProcessBuilder("pactl", "list", "sinks", "short");
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkMacHeadphones() {
        try {
            ProcessBuilder pb = new ProcessBuilder("system_profiler", "SPAudioDataType");
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 是否可用
     */
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * 获取声音上下文描述
     */
    public String getSoundContextDescription() {
        if (cachedInfo == null) return "Audio unavailable";
        StringBuilder desc = new StringBuilder();
        desc.append(switch (cachedInfo.soundContext()) {
            case SILENT -> "安静";
            case MUSIC -> "音乐播放中";
            case VIDEO -> "视频播放中";
            case VOICE_CALL -> "语音通话中";
            case NOTIFICATION -> "通知声音";
            case TYPING -> "键盘声";
            case AMBIENT -> "环境声";
            case SPEECH -> "说话中";
            case UNKNOWN -> "未知";
        });

        // S15-3: 添加活动上下文描述
        if (cachedInfo.activityContext() != null && cachedInfo.activityContext() != ActivityContext.UNKNOWN) {
            desc.append(" | 活动:").append(switch (cachedInfo.activityContext()) {
                case WORKING -> "工作中";
                case COMMUTING -> "通勤中";
                case EXERCISING -> "锻炼中";
                case RESTING -> "休息中";
                case SOCIALIZING -> "社交中";
                case DINING -> "用餐中";
                case ENTERTAINMENT -> "娱乐中";
                default -> "未知";
            });
        }

        return desc.toString();
    }

    /**
     * S15-3: 获取当前情绪指标
     */
    public MoodIndicator getCurrentMood() {
        if (cachedInfo != null && cachedInfo.moodIndicator() != null) {
            return cachedInfo.moodIndicator();
        }
        return MoodIndicator.UNKNOWN;
    }

    /**
     * S15-3: 获取当前活动上下文
     */
    public ActivityContext getCurrentActivityContext() {
        if (cachedInfo != null && cachedInfo.activityContext() != null) {
            return cachedInfo.activityContext();
        }
        return ActivityContext.UNKNOWN;
    }
}
