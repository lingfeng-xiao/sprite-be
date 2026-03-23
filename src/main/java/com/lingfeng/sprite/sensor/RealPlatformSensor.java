package com.lingfeng.sprite.sensor;

import java.net.InetAddress;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.PerceptionSystem.CpuStatus;
import com.lingfeng.sprite.PerceptionSystem.DeviceType;
import com.lingfeng.sprite.PerceptionSystem.DiskStatus;
import com.lingfeng.sprite.PerceptionSystem.MemoryStatus;
import com.lingfeng.sprite.PerceptionSystem.NetworkStatus;
import com.lingfeng.sprite.PerceptionSystem.Perception;
import com.lingfeng.sprite.PerceptionSystem.PlatformPerception;
import com.lingfeng.sprite.PerceptionSystem.PlatformSensor;
import com.lingfeng.sprite.PerceptionSystem.BatteryStatus;

/**
 * 真实平台传感器 - 使用 OSHI 获取真实系统数据
 */
public class RealPlatformSensor extends PlatformSensor {

    private final String sensorDeviceId;
    private final DeviceType sensorDeviceType;
    private static oshi.SystemInfo systemInfo;
    private static oshi.hardware.HardwareAbstractionLayer hardware;

    static {
        try {
            systemInfo = new oshi.SystemInfo();
            hardware = systemInfo.getHardware();
        } catch (Exception e) {
            systemInfo = null;
            hardware = null;
        }
    }

    public RealPlatformSensor(String deviceId, DeviceType deviceType) {
        super(deviceId, deviceType);
        this.sensorDeviceId = deviceId;
        this.sensorDeviceType = deviceType;
    }

    @Override
    public Perception perceive() {
        try {
            if (hardware == null) {
                return super.perceive();
            }

            String hostname = InetAddress.getLocalHost().getHostName();

            // CPU - OSHI 6.x uses getSystemCpuLoadBetweenTicks on OperatingSystem
            var processor = hardware.getProcessor();
            float cpuLoad = 0f;
            int cpuCount = processor.getLogicalProcessorCount();
            try {
                // OSHI 6.x - calculate CPU load using tick counts
                long[] ticks = processor.getSystemCpuLoadTicks();
                if (ticks != null && ticks.length >= 4) {
                    long idle = ticks[0];
                    long total = 0;
                    for (long t : ticks) {
                        total += t;
                    }
                    if (total > 0) {
                        cpuLoad = (float) ((total - idle) * 100.0 / total);
                    }
                }
            } catch (Exception e) {
                // Ignore - cpuLoad stays 0
            }

            // Memory
            var memory = hardware.getMemory();
            long totalMemory = memory.getTotal();
            long availableMemory = memory.getAvailable();
            float memoryUsagePercent = totalMemory > 0
                ? ((totalMemory - availableMemory) * 100f) / totalMemory
                : 0f;

            // Disk - simplified
            double totalDisk = 0;
            try {
                var disks = hardware.getDiskStores();
                for (var disk : disks) {
                    long size = disk.getSize();
                    if (size > 0) {
                        totalDisk += size;
                    }
                }
            } catch (Exception e) {
                // Ignore
            }

            // Network - enhanced with latency measurement
            var networkIFs = hardware.getNetworkIFs();
            boolean isConnected = !networkIFs.isEmpty();
            String adapterName = networkIFs.isEmpty() ? null : networkIFs.get(0).getName();
            Long latencyMs = null;
            try {
                // Measure latency to Google DNS
                long start = System.currentTimeMillis();
                InetAddress.getByName("8.8.8.8").isReachable(1000);
                latencyMs = System.currentTimeMillis() - start;
            } catch (Exception e) {
                // Could not measure latency
            }

            // Battery - use OSHI PowerSources if available
            boolean isCharging = false;
            int batteryPercent = 100;
            try {
                var powerSources = hardware.getPowerSources();
                if (powerSources != null && !powerSources.isEmpty()) {
                    var ps = powerSources.get(0);
                    isCharging = ps.isCharging();
                    batteryPercent = (int) Math.round(ps.getRemainingCapacityPercent() * 100);
                    // Clamp to 0-100
                    batteryPercent = Math.max(0, Math.min(100, batteryPercent));
                }
            } catch (Exception e) {
                // Power sources not available, use defaults
            }

            return new Perception(
                    java.time.Instant.now(),
                    new PlatformPerception(
                            sensorDeviceId,
                            sensorDeviceType,
                            hostname,
                            new MemoryStatus(
                                    totalMemory / (1024 * 1024),
                                    (totalMemory - availableMemory) / (1024 * 1024),
                                    memoryUsagePercent
                            ),
                            new DiskStatus(
                                    totalDisk / (1024.0 * 1024.0 * 1024.0),
                                    0,
                                    0f
                            ),
                            new BatteryStatus(isCharging, batteryPercent),
                            new CpuStatus(cpuLoad, cpuCount),
                            new NetworkStatus(isConnected, adapterName, null, null, latencyMs, null),
                            null,
                            null
                    ),
                    null, null, null, null, null
            );
        } catch (Exception e) {
            return super.perceive();
        }
    }

    @Override
    public String name() {
        return "RealPlatformSensor";
    }
}
