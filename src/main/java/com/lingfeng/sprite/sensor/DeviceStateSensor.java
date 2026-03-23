package com.lingfeng.sprite.sensor;

import java.time.Instant;

import com.lingfeng.sprite.PerceptionSystem.Sensor;

/**
 * S9-3: 设备状态传感器
 *
 * 检测设备详细状态信息：
 * - 设备模式（笔记本/台式机/平板/手机）
 * - 电源状态（连接电源/电池/低电量）
 * - 网络类型（WiFi/以太网/移动数据）
 * - 显示器状态
 * - 存储健康状态
 */
public class DeviceStateSensor implements Sensor {

    private final String name;
    private final DeviceStateInfo cachedInfo;

    /**
     * 设备状态信息
     */
    public record DeviceStateInfo(
        Instant timestamp,
        DeviceMode deviceMode,
        PowerState powerState,
        NetworkType networkType,
        DisplayState displayState,
        StorageHealth storageHealth,
        boolean isThermalThrottling,
        float cpuTemperature
    ) {
        public DeviceStateInfo {
            if (timestamp == null) timestamp = Instant.now();
            if (deviceMode == null) deviceMode = DeviceMode.UNKNOWN;
            if (powerState == null) powerState = PowerState.UNKNOWN;
            if (networkType == null) networkType = NetworkType.UNKNOWN;
            if (displayState == null) displayState = DisplayState.UNKNOWN;
            if (storageHealth == null) storageHealth = StorageHealth.UNKNOWN;
        }
    }

    /**
     * 设备模式
     */
    public enum DeviceMode {
        DESKTOP,    // 台式机
        LAPTOP,     // 笔记本
        TABLET,     // 平板
        PHONE,      // 手机
        SERVER,     // 服务器
        UNKNOWN     // 未知
    }

    /**
     * 电源状态
     */
    public enum PowerState {
        PLUGGED_IN,     // 连接电源
        ON_BATTERY,     // 使用电池
        LOW_BATTERY,    // 低电量
        CHARGING,       // 充电中
        FULL_POWER,     // 满电
        UNKNOWN         // 未知
    }

    /**
     * 网络类型
     */
    public enum NetworkType {
        WIFI,
        ETHERNET,
        MOBILE,
        BLUETOOTH,
        DISCONNECTED,
        UNKNOWN
    }

    /**
     * 显示器状态
     */
    public enum DisplayState {
        ON,
        OFF,
        LOCKED,
        SLEEP,
        UNKNOWN
    }

    /**
     * 存储健康状态
     */
    public enum StorageHealth {
        HEALTHY,
        WARNING,      // 存储空间不足或健康度下降
        CRITICAL,     // 严重问题
        UNKNOWN
    }

    public DeviceStateSensor() {
        this("DeviceStateSensor");
    }

    public DeviceStateSensor(String name) {
        this.name = name;
        this.cachedInfo = detectDeviceState();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public com.lingfeng.sprite.PerceptionSystem.Perception perceive() {
        return null;  // 独立传感器，不集成到感知系统
    }

    /**
     * 获取设备状态信息
     */
    public DeviceStateInfo getDeviceStateInfo() {
        return cachedInfo;
    }

    /**
     * 检测设备状态
     */
    private DeviceStateInfo detectDeviceState() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            DeviceMode deviceMode = detectDeviceMode();
            PowerState powerState = detectPowerState();
            NetworkType networkType = detectNetworkType();
            DisplayState displayState = detectDisplayState(os);
            StorageHealth storageHealth = detectStorageHealth();
            boolean isThermalThrottling = detectThermalThrottling();
            float cpuTemperature = detectCpuTemperature();

            return new DeviceStateInfo(
                Instant.now(),
                deviceMode,
                powerState,
                networkType,
                displayState,
                storageHealth,
                isThermalThrottling,
                cpuTemperature
            );
        } catch (Exception e) {
            return new DeviceStateInfo(
                Instant.now(),
                DeviceMode.UNKNOWN,
                PowerState.UNKNOWN,
                NetworkType.UNKNOWN,
                DisplayState.UNKNOWN,
                StorageHealth.UNKNOWN,
                false,
                0f
            );
        }
    }

    /**
     * 检测设备模式
     */
    private DeviceMode detectDeviceMode() {
        String os = System.getProperty("os.name").toLowerCase();
        String osVersion = System.getProperty("os.version").toLowerCase();

        // 检查是否是笔记本（电池存在通常表示笔记本）
        try {
            if (os.contains("windows")) {
                // Windows可以通过检查电池来判断是否是笔记本
                // 这里简化处理
                return DeviceMode.LAPTOP;
            } else if (os.contains("linux")) {
                // Linux上检查 /sys/class/power_supply/ 是否存在
                return DeviceMode.LAPTOP;
            } else if (os.contains("mac")) {
                return DeviceMode.LAPTOP;
            }
        } catch (Exception e) {
            // 忽略
        }

        // 默认根据操作系统推断
        if (os.contains("server")) {
            return DeviceMode.SERVER;
        }
        return DeviceMode.DESKTOP;
    }

    /**
     * 检测电源状态
     */
    private PowerState detectPowerState() {
        try {
            // 使用OSHI检测电源状态
            oshi.SystemInfo si = new oshi.SystemInfo();
            var powerSources = si.getHardware().getPowerSources();
            if (powerSources != null && !powerSources.isEmpty()) {
                var ps = powerSources.get(0);
                boolean isCharging = ps.isCharging();
                float remainingPercent = ps.getRemainingCapacityPercent() * 100;

                if (isCharging) {
                    return PowerState.CHARGING;
                }
                if (remainingPercent <= 20) {
                    return PowerState.LOW_BATTERY;
                }
                return PowerState.ON_BATTERY;
            }
            return PowerState.PLUGGED_IN;
        } catch (Exception e) {
            return PowerState.UNKNOWN;
        }
    }

    /**
     * 检测网络类型
     */
    private NetworkType detectNetworkType() {
        try {
            java.net.NetworkInterface[] interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    String name = ni.getDisplayName().toLowerCase();
                    if (name.contains("wi-fi") || name.contains("wlan") || name.contains("wifi")) {
                        return NetworkType.WIFI;
                    }
                    if (name.contains("ethernet") || name.contains("eth")) {
                        return NetworkType.ETHERNET;
                    }
                    if (name.contains("bluetooth") || name.contains("bt")) {
                        return NetworkType.BLUETOOTH;
                    }
                    if (name.contains("mobile") || name.contains("cell")) {
                        return NetworkType.MOBILE;
                    }
                }
            }
            return NetworkType.DISCONNECTED;
        } catch (Exception e) {
            return NetworkType.UNKNOWN;
        }
    }

    /**
     * 检测显示器状态
     */
    private DisplayState detectDisplayState(String os) {
        try {
            if (os.contains("windows")) {
                // Windows上检测显示器状态需要User32 API，这里简化处理
                return DisplayState.ON;
            } else if (os.contains("linux")) {
                // Linux上可以通过xset检测
                return DisplayState.ON;
            } else if (os.contains("mac")) {
                return DisplayState.ON;
            }
        } catch (Exception e) {
            // 忽略
        }
        return DisplayState.UNKNOWN;
    }

    /**
     * 检测存储健康状态
     */
    private StorageHealth detectStorageHealth() {
        try {
            // 检查磁盘空间
            java.io.File[] roots = java.io.File.listRoots();
            if (roots != null) {
                for (java.io.File root : roots) {
                    long total = root.getTotalSpace();
                    long free = root.getFreeSpace();
                    long used = total - free;
                    float usagePercent = total > 0 ? (used * 100f) / total : 0;

                    if (usagePercent > 95) {
                        return StorageHealth.CRITICAL;
                    }
                    if (usagePercent > 85) {
                        return StorageHealth.WARNING;
                    }
                }
            }
            return StorageHealth.HEALTHY;
        } catch (Exception e) {
            return StorageHealth.UNKNOWN;
        }
    }

    /**
     * 检测是否热节流
     */
    private boolean detectThermalThrottling() {
        try {
            // Linux上检查 /sys/class/thermal/thermal_zone*/trip_point_*_temp
            // 这里简化处理
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检测CPU温度
     */
    private float detectCpuTemperature() {
        try {
            oshi.SystemInfo si = new oshi.SystemInfo();
            var sensors = si.getHardware().getSensors();
            if (sensors != null) {
                float[] temps = sensors.getCpuTemperatures();
                if (temps != null && temps.length > 0) {
                    return temps[0];
                }
            }
            return 0f;
        } catch (Exception e) {
            return 0f;
        }
    }

    /**
     * 是否可用
     */
    public boolean isAvailable() {
        return cachedInfo != null && cachedInfo.deviceMode() != DeviceMode.UNKNOWN;
    }

    /**
     * 获取状态描述
     */
    public String getStateDescription() {
        if (cachedInfo == null) {
            return "Device state unavailable";
        }
        return String.format("%s | %s | %s | %s | Temp: %.1f°C",
            cachedInfo.deviceMode(),
            cachedInfo.powerState(),
            cachedInfo.networkType(),
            cachedInfo.storageHealth(),
            cachedInfo.cpuTemperature()
        );
    }
}
