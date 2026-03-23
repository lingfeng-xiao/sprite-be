package com.lingfeng.sprite.sensor;

import java.time.Instant;

import com.lingfeng.sprite.PerceptionSystem.Sensor;

/**
 * S9-2: 位置传感器
 *
 * 检测位置信息：
 * - IP地理位置
 * - 时区
 * - 网络位置（家庭/工作/外出）
 *
 * 注意：这是一个探索性传感器，完整的位置检测需要平台特定的实现或外部API
 */
public class LocationSensor implements Sensor {

    private final String name;
    private final LocationInfo cachedInfo;

    /**
     * 位置信息
     */
    public record LocationInfo(
        Instant timestamp,
        String country,
        String city,
        String timezone,
        String locationType,  // HOME, WORK, OUTSIDE, UNKNOWN
        double latitude,
        double longitude,
        String ipAddress,
        boolean isValid
    ) {
        public LocationInfo {
            if (timestamp == null) timestamp = Instant.now();
            if (country == null) country = "Unknown";
            if (city == null) city = "Unknown";
            if (timezone == null) timezone = "UTC";
            if (locationType == null) locationType = "UNKNOWN";
        }
    }

    public LocationSensor() {
        this("LocationSensor");
    }

    public LocationSensor(String name) {
        this.name = name;
        this.cachedInfo = detectLocation();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public com.lingfeng.sprite.PerceptionSystem.Perception perceive() {
        // 返回null，这个传感器作为独立的位置检测器
        return null;
    }

    /**
     * 获取位置信息
     */
    public LocationInfo getLocationInfo() {
        return cachedInfo;
    }

    /**
     * 检测位置信息
     */
    private LocationInfo detectLocation() {
        try {
            String timezone = java.time.ZoneId.systemDefault().getId();
            String country = getCountryFromTimezone(timezone);
            String city = getCityFromTimezone(timezone);
            String locationType = inferLocationType();
            String ipAddress = "unavailable";
            double latitude = 0;
            double longitude = 0;

            // 尝试通过API获取公网IP（可选）
            try {
                // 这里可以添加外部API调用来获取真实IP和位置
                // 但为了保持轻量级，使用基于时区的推断
            } catch (Exception e) {
                // 忽略
            }

            boolean isValid = !timezone.equals("UTC") || !country.equals("Unknown");

            return new LocationInfo(
                Instant.now(),
                country,
                city,
                timezone,
                locationType,
                latitude,
                longitude,
                ipAddress,
                isValid
            );
        } catch (Exception e) {
            return new LocationInfo(
                Instant.now(),
                "Unknown",
                "Unknown",
                "UTC",
                "UNKNOWN",
                0,
                0,
                "error",
                false
            );
        }
    }

    /**
     * 从时区推断国家
     */
    private String getCountryFromTimezone(String timezone) {
        if (timezone.startsWith("Asia/Shanghai") || timezone.startsWith("Asia/Hong_Kong") ||
            timezone.startsWith("Asia/Taipei") || timezone.startsWith("Asia/Macau")) {
            return "China";
        }
        if (timezone.startsWith("America/New_York") || timezone.startsWith("America/Los_Angeles") ||
            timezone.startsWith("America/Chicago")) {
            return "USA";
        }
        if (timezone.startsWith("Europe/London")) {
            return "UK";
        }
        if (timezone.startsWith("Europe/")) {
            return "Europe";
        }
        if (timezone.startsWith("Asia/Tokyo") || timezone.startsWith("Asia/Seoul")) {
            return "Asia";
        }
        return "Unknown";
    }

    /**
     * 从时区推断城市
     */
    private String getCityFromTimezone(String timezone) {
        if (timezone.contains("/")) {
            return timezone.substring(timezone.indexOf("/") + 1).replace("_", " ");
        }
        return timezone;
    }

    /**
     * 推断位置类型（基于时间和网络信息）
     */
    private String inferLocationType() {
        try {
            int hour = Instant.now().atZone(java.time.ZoneId.systemDefault()).getHour();

            // 简单推断：
            // - 深夜/凌晨时段通常在家里
            // - 工作时间可能在工作地点
            // - 其他时间可能是外出

            if (hour >= 0 && hour < 6) {
                return "HOME";  // 深夜在家里
            }
            if (hour >= 9 && hour < 18) {
                return "WORK";  // 工作时间在工作地点
            }
            // 其他时间需要更多上下文，这里返回UNKNOWN
            return "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * 是否可用
     */
    public boolean isAvailable() {
        return cachedInfo != null && cachedInfo.isValid();
    }

    /**
     * 获取位置描述
     */
    public String getLocationDescription() {
        if (cachedInfo == null || !cachedInfo.isValid()) {
            return "Location unavailable";
        }
        return cachedInfo.city() + ", " + cachedInfo.country() + " (" + cachedInfo.timezone() + ")";
    }
}
