package com.lingfeng.sprite.sensor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.lingfeng.sprite.PerceptionSystem.Perception;
import com.lingfeng.sprite.PerceptionSystem.Sensor;

/**
 * S9-1: 声音传感器
 *
 * 检测音频环境信息：
 * - 是否有声音播放
 * - 音频设备状态
 * - 环境声音级别（如果可用）
 *
 * 注意：这是一个探索性传感器，完整的音频捕获需要平台特定的实现
 */
public class AudioSensor implements Sensor {

    private final String name;
    private final boolean isAvailable;
    private final AudioInfo cachedInfo;

    /**
     * 音频信息
     */
    public record AudioInfo(
        Instant timestamp,
        boolean isPlaying,
        String audioDevice,
        float volumeLevel,
        String currentAudioApp,
        SoundContext soundContext,
        boolean isHeadphonesConnected
    ) {
        public AudioInfo {
            if (timestamp == null) timestamp = Instant.now();
            if (audioDevice == null) audioDevice = "default";
            if (soundContext == null) soundContext = SoundContext.UNKNOWN;
        }
    }

    /**
     * 声音上下文
     */
    public enum SoundContext {
        UNKNOWN,
        SILENT,         // 安静
        MUSIC,          // 音乐播放
        VIDEO,          // 视频播放
        VOICE_CALL,     // 语音通话
        NOTIFICATION,   // 通知声音
        TYPING,         // 键盘声
        AMBIENT         // 环境声
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

            return new AudioInfo(
                Instant.now(),
                isPlaying,
                audioDevice,
                volumeLevel,
                currentAudioApp,
                soundContext,
                isHeadphonesConnected
            );
        } catch (Exception e) {
            return new AudioInfo(
                Instant.now(),
                false,
                "unavailable",
                0f,
                null,
                SoundContext.UNKNOWN,
                false
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
        return switch (cachedInfo.soundContext()) {
            case SILENT -> "安静";
            case MUSIC -> "音乐播放中";
            case VIDEO -> "视频播放中";
            case VOICE_CALL -> "语音通话中";
            case NOTIFICATION -> "通知声音";
            case TYPING -> "键盘声";
            case AMBIENT -> "环境声";
            case UNKNOWN -> "未知";
        };
    }
}
