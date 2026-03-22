package com.lingfeng.sprite.sensor;

import java.time.Instant;
import java.time.ZoneOffset;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.PerceptionSystem.ContextType;
import com.lingfeng.sprite.PerceptionSystem.EnvironmentPerception;
import com.lingfeng.sprite.PerceptionSystem.Perception;
import com.lingfeng.sprite.PerceptionSystem.Sensor;

/**
 * 真实环境传感器
 *
 * 基于时间和系统信息推断环境状态
 */
public class RealEnvironmentSensor implements Sensor {

    @Override
    public Perception perceive() {
        Instant now = Instant.now();
        ZoneOffset offset = ZoneOffset.UTC;
        int hour = now.atZone(offset).getHour();
        int dayOfWeek = now.atZone(offset).getDayOfWeek().getValue();

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
     * 基于时间和星期推断上下文
     */
    private ContextType inferContext(int hour, int dayOfWeek) {
        boolean isWeekend = dayOfWeek == 6 || dayOfWeek == 7;

        if (hour >= 6 && hour <= 7) {
            return ContextType.RITUAL;  // 晨间习惯
        }
        if (hour >= 8 && hour <= 9) {
            return isWeekend ? ContextType.LEISURE : ContextType.WORK;  // 通勤/早工作
        }
        if (hour >= 10 && hour <= 12) {
            return ContextType.WORK;  // 高效工作时段
        }
        if (hour >= 12 && hour <= 13) {
            return ContextType.MEAL;  // lunch
        }
        if (hour >= 14 && hour <= 17) {
            return ContextType.WORK;  // 下午工作
        }
        if (hour >= 18 && hour <= 19) {
            return ContextType.COMMUTE;  // 通勤
        }
        if (hour >= 20 && hour <= 21) {
            return ContextType.LEISURE;  // 休闲
        }
        if (hour >= 22 && hour <= 23) {
            return ContextType.RITUAL;  // 晚间习惯
        }
        return ContextType.SLEEP;  // 睡眠
    }

    @Override
    public String name() {
        return "RealEnvironmentSensor";
    }
}
