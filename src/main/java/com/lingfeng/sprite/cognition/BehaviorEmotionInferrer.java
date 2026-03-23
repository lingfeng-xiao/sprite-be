package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.WorldModel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 行为信号情感推断器 - 基于行为模式推断主人情绪
 *
 * 核心职责：
 * 1. 基于应用类型推断情绪（开发=专注，聊天=放松）
 * 2. 基于空闲时间模式推断（短空闲=活跃，长空闲=休息）
 * 3. 基于时间上下文调整（工作时间vs休息时间）
 * 4. 基于活动类型调整情绪预期
 *
 * ## 推断规则
 *
 * - 开发工具使用中 + 早晨 = 专注/积极
 * - 聊天工具使用中 = 放松
 * - 媒体播放中 = 放松/愉悦
 * - 长时间空闲后恢复 = 工作狂倾向
 * - 频繁切换窗口 = 可能焦虑/分心
 * - 电量低时使用 = 可能焦虑
 */
public class BehaviorEmotionInferrer {

    private final ZoneId timezone = ZoneId.of("Asia/Shanghai");
    private Instant lastPresenceChange = Instant.now();
    private OwnerModel.PresenceStatus lastPresence = OwnerModel.PresenceStatus.UNKNOWN;
    private int appSwitchCount = 0;
    private String lastAppType = null;
    private Instant lastAppSwitchTime = Instant.now();

    /**
     * 基于感知和行为信号推断情绪
     *
     * @param perception 当前感知
     * @param worldModel 当前世界模型
     * @return 推断的情绪状态
     */
    public OwnerModel.EmotionalState infer(PerceptionSystem.Perception perception, WorldModel.World worldModel) {
        OwnerModel.Mood baseMood = OwnerModel.Mood.NEUTRAL;
        float intensity = 0.5f;
        String reason = "";

        // 1. 基于应用类型调整情绪
        if (perception != null && perception.user() != null && perception.user().activeWindow() != null) {
            PerceptionSystem.AppType appType = perception.user().activeWindow().appType();
            var moodAdjustment = inferFromAppType(appType);
            baseMood = moodAdjustment.mood();
            intensity = moodAdjustment.intensity();
            reason = moodAdjustment.reason();
        }

        // 2. 基于存在状态调整
        if (perception != null && perception.user() != null) {
            var presenceMood = inferFromPresence(
                perception.user().presence(),
                perception.platform() != null ? perception.platform().battery() : null
            );
            // 如果存在状态提供更强的信号，使用它
            if (presenceMood.intensity() > intensity) {
                baseMood = presenceMood.mood();
                intensity = presenceMood.intensity();
                reason = presenceMood.reason();
            }
        }

        // 3. 基于时间上下文调整
        var timeMood = inferFromTimeContext();
        if (timeMood.intensity() > intensity) {
            baseMood = timeMood.mood();
            intensity = timeMood.intensity();
            reason = timeMood.reason();
        }

        // 4. 基于活动上下文调整
        if (worldModel != null && worldModel.currentContext() != null) {
            WorldModel.Activity activity = worldModel.currentContext().activity();
            var activityMood = inferFromActivity(activity);
            if (activityMood.intensity() > intensity) {
                baseMood = activityMood.mood();
                intensity = activityMood.intensity();
                reason = activityMood.reason();
            }
        }

        // 5. 追踪应用切换模式
        trackAppSwitching(perception);

        // 6. 如果频繁切换应用，增加焦虑信号
        if (appSwitchCount > 3 && intensity < 0.7f) {
            baseMood = OwnerModel.Mood.ANXIOUS;
            intensity = 0.6f;
            reason = "频繁切换应用";
        }

        return new OwnerModel.EmotionalState(baseMood, intensity, Instant.now(), reason);
    }

    /**
     * 从应用类型推断情绪
     */
    private MoodAdjustment inferFromAppType(PerceptionSystem.AppType appType) {
        if (appType == null) {
            return new MoodAdjustment(OwnerModel.Mood.NEUTRAL, 0.3f, "未知应用");
        }

        return switch (appType) {
            case DEVELOPMENT -> new MoodAdjustment(OwnerModel.Mood.FOCUSED, 0.7f, "开发中-专注");
            case CHAT -> new MoodAdjustment(OwnerModel.Mood.RELAXED, 0.5f, "聊天中-放松");
            case BROWSER -> new MoodAdjustment(OwnerModel.Mood.CALM, 0.5f, "浏览中");
            case MEDIA -> new MoodAdjustment(OwnerModel.Mood.HAPPY, 0.6f, "媒体播放-愉悦");
            case PRODUCTIVITY -> new MoodAdjustment(OwnerModel.Mood.FOCUSED, 0.6f, "工作中");
            case SYSTEM -> new MoodAdjustment(OwnerModel.Mood.NEUTRAL, 0.3f, "系统操作");
            default -> new MoodAdjustment(OwnerModel.Mood.NEUTRAL, 0.3f, "其他活动");
        };
    }

    /**
     * 从存在状态推断情绪
     */
    private MoodAdjustment inferFromPresence(PerceptionSystem.PresenceStatus presence,
                                             PerceptionSystem.BatteryStatus battery) {
        if (presence == null) {
            return new MoodAdjustment(OwnerModel.Mood.NEUTRAL, 0.3f, "状态未知");
        }

        float baseIntensity = 0.5f;
        String reason = switch (presence) {
            case ACTIVE -> "用户活跃";
            case IDLE -> "用户空闲";
            case AWAY -> "用户离开";
            default -> "状态未知";
        };

        OwnerModel.Mood mood = switch (presence) {
            case ACTIVE -> OwnerModel.Mood.CALM;
            case IDLE -> OwnerModel.Mood.NEUTRAL;
            case AWAY -> OwnerModel.Mood.TIRED;
            default -> OwnerModel.Mood.NEUTRAL;
        };

        // 电量低时增加焦虑
        if (battery != null && !battery.isCharging() && battery.percent() < 20) {
            mood = OwnerModel.Mood.ANXIOUS;
            baseIntensity = 0.7f;
            reason += ", 电量低";
        }

        return new MoodAdjustment(mood, baseIntensity, reason);
    }

    /**
     * 从时间上下文推断情绪
     */
    private MoodAdjustment inferFromTimeContext() {
        LocalDateTime now = LocalDateTime.now(timezone);
        int hour = now.getHour();

        // 深夜工作 - 可能疲惫但专注
        if (hour >= 22 || hour < 5) {
            return new MoodAdjustment(OwnerModel.Mood.TIRED, 0.6f, "深夜时段");
        }

        // 早晨工作 - 精力充沛
        if (hour >= 7 && hour < 10) {
            return new MoodAdjustment(OwnerModel.Mood.HAPPY, 0.6f, "早晨精力充沛");
        }

        // 午休时段 - 放松
        if (hour >= 12 && hour < 14) {
            return new MoodAdjustment(OwnerModel.Mood.RELAXED, 0.5f, "午休时段");
        }

        // 下午疲惫时段
        if (hour >= 14 && hour < 17) {
            return new MoodAdjustment(OwnerModel.Mood.NEUTRAL, 0.4f, "下午工作");
        }

        // 傍晚 - 放松
        if (hour >= 17 && hour < 22) {
            return new MoodAdjustment(OwnerModel.Mood.RELAXED, 0.5f, "傍晚放松");
        }

        return new MoodAdjustment(OwnerModel.Mood.NEUTRAL, 0.3f, "普通时段");
    }

    /**
     * 从活动上下文推断情绪
     */
    private MoodAdjustment inferFromActivity(WorldModel.Activity activity) {
        if (activity == null) {
            return new MoodAdjustment(OwnerModel.Mood.NEUTRAL, 0.3f, "活动未知");
        }

        return switch (activity) {
            case WORK -> new MoodAdjustment(OwnerModel.Mood.FOCUSED, 0.6f, "工作模式");
            case LEISURE -> new MoodAdjustment(OwnerModel.Mood.RELAXED, 0.6f, "休闲模式");
            case CREATIVE -> new MoodAdjustment(OwnerModel.Mood.HAPPY, 0.7f, "创作模式");
            case SOCIAL -> new MoodAdjustment(OwnerModel.Mood.HAPPY, 0.6f, "社交模式");
            case IDLE -> new MoodAdjustment(OwnerModel.Mood.NEUTRAL, 0.3f, "空闲中");
            case UNKNOWN -> new MoodAdjustment(OwnerModel.Mood.NEUTRAL, 0.3f, "未知活动");
        };
    }

    /**
     * 追踪应用切换模式
     */
    private void trackAppSwitching(PerceptionSystem.Perception perception) {
        if (perception == null || perception.user() == null ||
            perception.user().activeWindow() == null) {
            return;
        }

        String currentAppType = perception.user().activeWindow().appType().name();
        Instant now = Instant.now();

        if (lastAppType != null && !lastAppType.equals(currentAppType)) {
            // 检测到应用切换
            long secondsSinceLastSwitch = ChronoUnit.SECONDS.between(lastAppSwitchTime, now);
            if (secondsSinceLastSwitch < 30) {
                // 快速切换 = 频繁切换
                appSwitchCount++;
            } else {
                // 重置计数
                appSwitchCount = 1;
            }
            lastAppSwitchTime = now;
        } else {
            // 没有切换，减少计数（但不低于0）
            appSwitchCount = Math.max(0, appSwitchCount - 1);
        }

        lastAppType = currentAppType;
    }

    /**
     * 获取应用切换计数
     */
    public int getAppSwitchCount() {
        return appSwitchCount;
    }

    /**
     * 重置应用切换计数
     */
    public void resetAppSwitchCount() {
        appSwitchCount = 0;
    }

    /**
     * 情绪调整结果
     */
    private record MoodAdjustment(OwnerModel.Mood mood, float intensity, String reason) {}
}
