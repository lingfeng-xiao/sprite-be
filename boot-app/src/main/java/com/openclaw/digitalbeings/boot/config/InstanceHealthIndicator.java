package com.openclaw.digitalbeings.boot.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;

@Component
public class InstanceHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            long startTimeMillis = runtimeBean.getStartTime();
            Instant startTime = Instant.ofEpochMilli(startTimeMillis);
            Duration uptime = Duration.ofMillis(runtimeBean.getUptime());

            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedHeap = memoryBean.getHeapMemoryUsage().getUsed();
            long maxHeap = memoryBean.getHeapMemoryUsage().getMax();

            return Health.up()
                    .withDetail("uptime", uptime.toString())
                    .withDetail("startTime", startTime.toString())
                    .withDetail("heapUsed", usedHeap)
                    .withDetail("heapMax", maxHeap)
                    .build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
