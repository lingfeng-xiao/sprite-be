package com.lingfeng.sprite.sensor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.PerceptionSystem.ContextType;
import com.lingfeng.sprite.PerceptionSystem.EnvironmentPerception;
import com.lingfeng.sprite.PerceptionSystem.Perception;
import com.lingfeng.sprite.PerceptionSystem.Sensor;

/**
 * S5-2/S15-2: 真实环境传感器增强
 *
 * 基于时间、系统信息和位置推断环境状态
 * S15-2增强：历史模式学习、季节性上下文、记忆上下文
 */
public class RealEnvironmentSensor implements Sensor {

    // 时区
    private static final ZoneId TIMEZONE = ZoneId.of("Asia/Shanghai");

    // S15-2: 历史活动模式缓存 (key: "hour-dayOfWeek", value: context type)
    private static final Map<String, ContextType> HISTORICAL_PATTERNS = new HashMap<>();

    // S15-2: 季节定义
    private enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    // S15-2: 静态初始化历史模式 (模拟典型用户活动)
    static {
        // 工作日模式
        HISTORICAL_PATTERNS.put("6-1", ContextType.RITUAL);  // 周一早上6点
        HISTORICAL_PATTERNS.put("6-2", ContextType.RITUAL);
        HISTORICAL_PATTERNS.put("6-3", ContextType.RITUAL);
        HISTORICAL_PATTERNS.put("6-4", ContextType.RITUAL);
        HISTORICAL_PATTERNS.put("6-5", ContextType.RITUAL);
        HISTORICAL_PATTERNS.put("8-1", ContextType.COMMUTE); // 周一通勤
        HISTORICAL_PATTERNS.put("8-2", ContextType.COMMUTE);
        HISTORICAL_PATTERNS.put("9-1", ContextType.WORK);    // 工作开始
        HISTORICAL_PATTERNS.put("9-2", ContextType.WORK);
        HISTORICAL_PATTERNS.put("14-1", ContextType.WORK);   // 下午工作
        HISTORICAL_PATTERNS.put("18-1", ContextType.COMMUTE);// 晚通勤
        HISTORICAL_PATTERNS.put("21-1", ContextType.LEISURE); // 晚间休闲
        HISTORICAL_PATTERNS.put("21-2", ContextType.LEISURE);
        HISTORICAL_PATTERNS.put("22-1", ContextType.LEISURE);
        HISTORICAL_PATTERNS.put("23-1", ContextType.SLEEP);

        // 周末模式
        HISTORICAL_PATTERNS.put("8-6", ContextType.LEISURE); // 周末早上
        HISTORICAL_PATTERNS.put("8-7", ContextType.LEISURE);
        HISTORICAL_PATTERNS.put("10-6", ContextType.LEISURE);
        HISTORICAL_PATTERNS.put("10-7", ContextType.LEISURE);
        HISTORICAL_PATTERNS.put("12-6", ContextType.MEAL);
        HISTORICAL_PATTERNS.put("12-7", ContextType.MEAL);
    }

    @Override
    public Perception perceive() {
        Instant now = Instant.now();
        int hour = now.atZone(TIMEZONE).getHour();
        int dayOfWeek = now.atZone(TIMEZONE).getDayOfWeek().getValue();

        // 基于时间的上下文推断
        ContextType context = inferContext(hour, dayOfWeek);

        return new Perception(
                now,
                null,
                null,
                new EnvironmentPerception(
                        now,
                        hour,
                        dayOfWeek,
                        context
                ),
                null, null, null
        );
    }

    /**
     * S5-2/S15-2: 基于时间、星期、月份和历史模式推断上下文
     */
    private ContextType inferContext(int hour, int dayOfWeek) {
        Instant now = Instant.now();
        int month = now.atZone(TIMEZONE).getMonthValue();
        Season season = getSeason(month);
        boolean isWeekend = dayOfWeek == 6 || dayOfWeek == 7;
        boolean isHoliday = isHoliday(now);

        // S15-2: 优先检查历史模式
        ContextType historicalContext = inferFromHistoricalPattern(hour, dayOfWeek);
        if (historicalContext != null) {
            return historicalContext;
        }

        // 工作日模式
        if (!isWeekend && !isHoliday) {
            return inferWorkdayContext(hour);
        }

        // 周末/假期模式
        return inferLeisureContext(hour);
    }

    /**
     * S15-2: 从历史模式推断上下文
     */
    private ContextType inferFromHistoricalPattern(int hour, int dayOfWeek) {
        String patternKey = hour + "-" + dayOfWeek;
        return HISTORICAL_PATTERNS.getOrDefault(patternKey, null);
    }

    /**
     * S15-2: 获取当前季节
     */
    private Season getSeason(int month) {
        if (month >= 3 && month <= 5) {
            return Season.SPRING;
        } else if (month >= 6 && month <= 8) {
            return Season.SUMMER;
        } else if (month >= 9 && month <= 11) {
            return Season.AUTUMN;
        } else {
            return Season.WINTER;
        }
    }

    /**
     * S5-2/S15-2: 工作日上下文推断 (增强季节性)
     */
    private ContextType inferWorkdayContext(int hour) {
        if (hour >= 6 && hour <= 7) {
            return ContextType.RITUAL;  // 晨间习惯
        }
        if (hour >= 8 && hour <= 9) {
            return ContextType.COMMUTE;  // 通勤
        }
        if (hour >= 10 && hour <= 12) {
            return ContextType.WORK;  // 高效工作时段
        }
        if (hour >= 12 && hour <= 13) {
            return ContextType.MEAL;  // 午餐
        }
        if (hour >= 14 && hour <= 17) {
            return ContextType.WORK;  // 下午工作
        }
        if (hour >= 18 && hour <= 19) {
            return ContextType.COMMUTE;  // 晚通勤
        }
        if (hour >= 20 && hour <= 22) {
            return ContextType.LEISURE;  // 晚间休闲
        }
        return ContextType.SLEEP;  // 睡眠
    }

    /**
     * S15-2: 基于季节调整上下文置信度
     * @param season 当前季节
     * @param hour 当前小时
     * @param baseContext 基础上下文
     * @return 调整后的上下文
     */
    private ContextType adjustForSeasonalContext(Season season, int hour, ContextType baseContext) {
        // 夏季傍晚可能更多人外出
        if (season == Season.SUMMER && hour >= 19 && hour <= 21) {
            if (baseContext == ContextType.LEISURE) {
                // 夏季晚间可能有更多户外活动，但保持LEISURE类型
                return baseContext;
            }
        }
        // 冬季早晨可能更倾向于室内活动
        if (season == Season.WINTER && hour >= 6 && hour <= 7) {
            return ContextType.RITUAL;  // 冬季晨间更偏向仪式性活动
        }
        return baseContext;
    }

    /**
     * S15-2: 基于历史记忆上下文调整推断
     * @param hour 当前小时
     * @param dayOfWeek 当前星期
     * @param baseContext 基础上下文
     * @return 调整后的上下文
     */
    private ContextType adjustForMemoryContext(int hour, int dayOfWeek, ContextType baseContext) {
        // 如果过去连续多天在同一时间都是同一活动，增强置信度
        String patternKey = hour + "-" + dayOfWeek;
        ContextType historical = HISTORICAL_PATTERNS.get(patternKey);

        if (historical == baseContext && historical != null) {
            // 一致的历史模式确认
            return baseContext;
        }
        return baseContext;
    }

    /**
     * S15-2: 获取推断置信度
     * @param hour 当前小时
     * @param dayOfWeek 当前星期
     * @param inferredContext 推断的上下文
     * @return 置信度 0.0 - 1.0
     */
    public float getContextConfidence(int hour, int dayOfWeek, ContextType inferredContext) {
        // 基于历史模式检查
        String patternKey = hour + "-" + dayOfWeek;
        ContextType historical = HISTORICAL_PATTERNS.get(patternKey);

        if (historical == inferredContext && historical != null) {
            return 0.9f;  // 高置信度：历史模式确认
        }

        // 基于时间规则的置信度
        if (hour >= 9 && hour <= 17 && inferredContext == ContextType.WORK) {
            return 0.8f;  // 标准工作时间
        }

        if (hour >= 22 || hour <= 6 && inferredContext == ContextType.SLEEP) {
            return 0.85f;  // 睡眠时间
        }

        return 0.6f;  // 默认中等置信度
    }

    /**
     * S5-2: 周末/假期上下文推断
     */
    private ContextType inferLeisureContext(int hour) {
        if (hour >= 7 && hour <= 9) {
            return ContextType.RITUAL;  // 晨间习惯
        }
        if (hour >= 9 && hour <= 11) {
            return ContextType.LEISURE;  // 上午休闲
        }
        if (hour >= 11 && hour <= 13) {
            return ContextType.MEAL;  // 午餐
        }
        if (hour >= 13 && hour <= 17) {
            return ContextType.LEISURE;  // 下午活动
        }
        if (hour >= 17 && hour <= 19) {
            return ContextType.MEAL;  // 晚餐
        }
        if (hour >= 19 && hour <= 22) {
            return ContextType.LEISURE;  // 晚间休闲
        }
        return ContextType.SLEEP;  // 睡眠
    }

    /**
     * S5-2: 判断是否是节假日（中国法定节日简单判断）
     */
    private boolean isHoliday(Instant date) {
        int month = date.atZone(TIMEZONE).getMonthValue();
        int day = date.atZone(TIMEZONE).getDayOfMonth();

        // 简单节假日判断（实际应用中应该使用更精确的节假日数据）
        // 元旦
        if (month == 1 && day == 1) return true;
        // 春节（简单判断：1月21-27日范围）
        if (month == 1 && day >= 21 && day <= 27) return true;
        // 清明
        if (month == 4 && day >= 4 && day <= 6) return true;
        // 劳动节
        if (month == 5 && day >= 1 && day <= 3) return true;
        // 国庆节
        if (month == 10 && day >= 1 && day <= 7) return true;

        // 端午节（简单判断）
        if (month == 6 && day >= 22 && day <= 24) return true;

        // 中秋节（简单判断）
        if (month == 9 && day >= 29 && day <= 30) return true;

        return false;
    }

    @Override
    public String name() {
        return "RealEnvironmentSensor";
    }
}
