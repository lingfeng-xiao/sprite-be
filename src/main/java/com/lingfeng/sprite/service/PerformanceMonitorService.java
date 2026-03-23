package com.lingfeng.sprite.service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * S11-4: 性能监控服务 - S13-4: 添加Spring服务注解
 *
 * 提供运行时性能指标收集和监控：
 * - JVM内存使用
 * - 线程状态
 * - 方法执行时间
 * - 自定义指标
 * - 性能历史
 */
@Service
public class PerformanceMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitorService.class);

    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final Map<String, MetricGauge> gauges;
    private final Map<String, ConcurrentLinkedDeque<MetricPoint>> metricHistory;
    private static final int MAX_HISTORY_SIZE = 1000;

    /**
     * 指标数据点
     */
    public record MetricPoint(
        Instant timestamp,
        double value,
        String unit
    ) {}

    /**
     * 指标仪表
     */
    public record MetricGauge(
        String name,
        String description,
        MetricType type,
        double currentValue,
        double minValue,
        double maxValue,
        Instant lastUpdated
    ) {}

    /**
     * 指标类型
     */
    public enum MetricType {
        GAUGE,      // 当前值
        COUNTER,     // 累加值
        TIMER        // 计时
    }

    /**
     * 性能快照
     */
    public record PerformanceSnapshot(
        Instant timestamp,
        MemoryInfo memory,
        ThreadInfo threads,
        Map<String, Double> customMetrics,
        SystemInfo system
    ) {}

    /**
     * 内存信息
     */
    public record MemoryInfo(
        long heapUsed,
        long heapMax,
        float heapUsagePercent,
        long nonHeapUsed,
        long nonHeapMax,
        List<MemoryPoolInfo> pools
    ) {
        public record MemoryPoolInfo(String name, long used, long max, float usagePercent) {}
    }

    /**
     * 线程信息
     */
    public record ThreadInfo(
        int totalThreads,
        int peakThreads,
        int daemonThreads,
        long totalStarted
    ) {}

    /**
     * 系统信息
     */
    public record SystemInfo(
        double processCpuLoad,
        double systemCpuLoad,
        long uptime
    ) {}

    public PerformanceMonitorService() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.gauges = new ConcurrentHashMap<>();
        this.metricHistory = new ConcurrentHashMap<>();
    }

    // ==================== 内置指标 ====================

    /**
     * 获取性能快照
     */
    public PerformanceSnapshot getSnapshot() {
        Instant now = Instant.now();

        // 内存信息
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        List<MemoryInfo.MemoryPoolInfo> pools = new ArrayList<>();
        memoryBean.getMemoryPoolMXBeans().forEach(pool -> {
            MemoryUsage usage = pool.getUsage();
            pools.add(new MemoryInfo.MemoryPoolInfo(
                pool.getName(),
                usage.getUsed(),
                usage.getMax() >= 0 ? usage.getMax() : usage.getCommitted(),
                usage.getUsagePercent()
            ));
        });

        MemoryInfo memory = new MemoryInfo(
            heapUsage.getUsed(),
            heapUsage.getMax() >= 0 ? heapUsage.getMax() : heapUsage.getCommitted(),
            heapUsage.getUsagePercent(),
            nonHeapUsage.getUsed(),
            nonHeapUsage.getMax() >= 0 ? nonHeapUsage.getMax() : nonHeapUsage.getCommitted(),
            pools
        );

        // 线程信息
        ThreadInfo threads = new ThreadInfo(
            threadBean.getThreadCount(),
            threadBean.getPeakThreadCount(),
            (int) threadBean.getDaemonThreadCount(),
            threadBean.getTotalStartedThreadCount()
        );

        // 自定义指标
        Map<String, Double> customMetrics = new ConcurrentHashMap<>();
        gauges.forEach((name, gauge) -> customMetrics.put(name, gauge.currentValue()));

        // 系统信息
        double processCpu = 0;
        double systemCpu = 0;
        try {
            var osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                processCpu = sunOs.getProcessCpuLoad();
                systemCpu = sunOs.getSystemCpuLoad();
            }
        } catch (Exception e) {
            // 忽略
        }

        SystemInfo system = new SystemInfo(
            processCpu,
            systemCpu,
            ManagementFactory.getRuntimeMXBean().getUptime()
        );

        return new PerformanceSnapshot(now, memory, threads, customMetrics, system);
    }

    /**
     * 获取简洁的性能状态
     */
    public String getPerformanceStatus() {
        PerformanceSnapshot snapshot = getSnapshot();

        return String.format(
            "Memory: %.1f%% (%dMB/%dMB) | Threads: %d | CPU: %.1f%% | Uptime: %s",
            snapshot.memory().heapUsagePercent(),
            snapshot.memory().heapUsed() / (1024 * 1024),
            snapshot.memory().heapMax() / (1024 * 1024),
            snapshot.threads().totalThreads(),
            snapshot.system().processCpuLoad() * 100,
            formatUptime(snapshot.system().uptime())
        );
    }

    private String formatUptime(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    // ==================== 自定义指标 ====================

    /**
     * 注册指标
     */
    public void registerMetric(String name, String description, MetricType type) {
        gauges.put(name, new MetricGauge(name, description, type, 0, Double.MAX_VALUE, Double.MIN_VALUE, Instant.now()));
        metricHistory.put(name, new ConcurrentLinkedDeque<>());
        logger.debug("Registered metric: {} ({})", name, type);
    }

    /**
     * 记录指标值
     */
    public void recordValue(String name, double value) {
        MetricGauge gauge = gauges.get(name);
        if (gauge == null) {
            registerMetric(name, "", MetricType.GAUGE);
            gauge = gauges.get(name);
        }

        long now = System.currentTimeMillis();
        gauge = new MetricGauge(
            name,
            gauge.description(),
            gauge.type(),
            value,
            Math.min(gauge.minValue(), value),
            Math.max(gauge.maxValue(), value),
            Instant.ofEpochMilli(now)
        );
        gauges.put(name, gauge);

        // 记录历史
        ConcurrentLinkedDeque<MetricPoint> history = metricHistory.get(name);
        if (history != null) {
            history.addLast(new MetricPoint(Instant.now(), value, ""));
            while (history.size() > MAX_HISTORY_SIZE) {
                history.removeFirst();
            }
        }
    }

    /**
     * 增加计数器
     */
    public void incrementCounter(String name) {
        recordValue(name, gauges.getOrDefault(name, new MetricGauge(name, "", MetricType.COUNTER, 0, 0, 0, Instant.now()).currentValue() + 1);
    }

    /**
     * 记录计时
     */
    public TimerContext startTimer(String name) {
        return new TimerContext(name, System.currentTimeMillis());
    }

    /**
     * 计时上下文
     */
    public class TimerContext implements AutoCloseable {
        private final String name;
        private final long startTime;

        public TimerContext(String name, long startTime) {
            this.name = name;
            this.startTime = startTime;
        }

        @Override
        public void close() {
            long duration = System.currentTimeMillis() - startTime;
            recordValue(name + ".duration", duration);
        }
    }

    // ==================== 历史数据 ====================

    /**
     * 获取指标历史
     */
    public List<MetricPoint> getMetricHistory(String name, int limit) {
        ConcurrentLinkedDeque<MetricPoint> history = metricHistory.get(name);
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        int size = Math.min(limit, history.size());
        return new ArrayList<>(history).subList(history.size() - size, history.size());
    }

    /**
     * 获取所有指标当前值
     */
    public Map<String, Double> getAllCurrentValues() {
        Map<String, Double> values = new ConcurrentHashMap<>();
        gauges.forEach((name, gauge) -> values.put(name, gauge.currentValue()));
        return values;
    }

    // ==================== 告警检查 ====================

    /**
     * 检查是否需要告警
     */
    public List<Alert> checkAlerts() {
        List<Alert> alerts = new ArrayList<>();
        PerformanceSnapshot snapshot = getSnapshot();

        // 内存告警
        if (snapshot.memory().heapUsagePercent() > 90) {
            alerts.add(new Alert(AlertLevel.CRITICAL, "Memory", "Heap usage above 90%", snapshot.memory().heapUsagePercent()));
        } else if (snapshot.memory().heapUsagePercent() > 80) {
            alerts.add(new Alert(AlertLevel.WARNING, "Memory", "Heap usage above 80%", snapshot.memory().heapUsagePercent()));
        }

        // 线程告警
        if (snapshot.threads().totalThreads() > 200) {
            alerts.add(new Alert(AlertLevel.WARNING, "Threads", "High thread count", snapshot.threads().totalThreads()));
        }

        return alerts;
    }

    public record Alert(AlertLevel level, String source, String message, double value) {}
    public enum AlertLevel { INFO, WARNING, CRITICAL }

    /**
     * 获取性能报告
     */
    public String getPerformanceReport() {
        PerformanceSnapshot snapshot = getSnapshot();
        StringBuilder sb = new StringBuilder();

        sb.append("=== Performance Report ===\n");
        sb.append(String.format("Timestamp: %s\n\n", snapshot.timestamp()));

        sb.append("--- Memory ---\n");
        sb.append(String.format("Heap: %.1f%% (%dMB/%dMB)\n",
            snapshot.memory().heapUsagePercent(),
            snapshot.memory().heapUsed() / (1024 * 1024),
            snapshot.memory().heapMax() / (1024 * 1024)));
        sb.append(String.format("Non-Heap: %dMB\n",
            snapshot.memory().nonHeapUsed() / (1024 * 1024)));

        sb.append("\n--- Threads ---\n");
        sb.append(String.format("Total: %d (Peak: %d, Daemon: %d)\n",
            snapshot.threads().totalThreads(),
            snapshot.threads().peakThreads(),
            snapshot.threads().daemonThreads()));

        sb.append("\n--- Custom Metrics ---\n");
        if (snapshot.customMetrics().isEmpty()) {
            sb.append("No custom metrics\n");
        } else {
            snapshot.customMetrics().forEach((name, value) ->
                sb.append(String.format("%s: %s\n", name, formatValue(value))));
        }

        return sb.toString();
    }

    private String formatValue(double value) {
        if (value >= 1_000_000) {
            return String.format("%.2fM", value / 1_000_000);
        } else if (value >= 1_000) {
            return String.format("%.2fK", value / 1_000);
        } else {
            return String.format("%.2f", value);
        }
    }
}
