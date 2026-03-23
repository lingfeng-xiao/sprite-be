package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.WorldModel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * S15-4: 感知融合 - 多源感知 → 统一输入
 *
 * ## 融合策略
 *
 * 1. **时序加权**：最近感知权重更高（指数移动平均）
 * 2. **类型融合**：合并不同类型的感知数据
 * 3. **冲突解决**：当冲突时，使用最新数据或更高置信度
 * 4. **S15-4: 传感器准确性加权**：基于历史准确性调整传感器权重
 * 5. **S15-4: 上下文相关性加权**：根据当前上下文调整权重
 */
public class PerceptionFusion {
    private final float fusionFactor;
    private PerceptionSystem.Perception lastFusedPerception = null;

    // S15-4: 传感器准确性追踪 (sensorName -> accuracy score)
    private final Map<String, SensorAccuracy> sensorAccuracyMap = new HashMap<>();

    // S15-4: 上下文相关性权重
    private final Map<PerceptionSystem.ContextType, Map<String, Float>> contextRelevanceWeights = new HashMap<>();

    /**
     * S15-4: 传感器准确性记录
     */
    private static class SensorAccuracy {
        float totalAccuracy;      // 累计准确性
        int observationCount;     // 观察次数
        float emaAccuracy;        // 指数移动平均准确性
        Instant lastUpdated;

        SensorAccuracy(float initialAccuracy) {
            this.totalAccuracy = initialAccuracy;
            this.observationCount = 1;
            this.emaAccuracy = initialAccuracy;
            this.lastUpdated = Instant.now();
        }

        void update(boolean wasAccurate) {
            float newAccuracy = wasAccurate ? 1.0f : 0.0f;
            // 指数移动平均更新
            this.emaAccuracy = 0.3f * newAccuracy + 0.7f * this.emaAccuracy;
            this.observationCount++;
            this.lastUpdated = Instant.now();
        }

        float getAccuracy() {
            // 观察次数越多，EMA准确性越可靠
            float reliability = Math.min(1.0f, observationCount / 10.0f);
            return emaAccuracy * reliability + 0.5f * (1 - reliability);
        }
    }

    public PerceptionFusion() {
        this(0.7f);
        initializeContextRelevanceWeights();
    }

    public PerceptionFusion(float fusionFactor) {
        this.fusionFactor = fusionFactor;
        initializeContextRelevanceWeights();
    }

    /**
     * S15-4: 初始化上下文相关性权重
     * 定义在不同上下文类型下各传感器的相关性
     */
    private void initializeContextRelevanceWeights() {
        // 工作上下文：环境传感器、用户传感器更相关
        Map<String, Float> workWeights = new HashMap<>();
        workWeights.put("EnvironmentSensor", 0.9f);
        workWeights.put("UserSensor", 0.9f);
        workWeights.put("PlatformSensor", 0.6f);
        workWeights.put("DesktopSensor", 0.7f);
        workWeights.put("ProcessSensor", 0.8f);
        workWeights.put("DigitalSensor", 0.5f);
        contextRelevanceWeights.put(PerceptionSystem.ContextType.WORK, workWeights);

        // 休闲上下文：环境传感器、桌面传感器更相关
        Map<String, Float> leisureWeights = new HashMap<>();
        leisureWeights.put("EnvironmentSensor", 0.8f);
        leisureWeights.put("UserSensor", 0.6f);
        leisureWeights.put("PlatformSensor", 0.5f);
        leisureWeights.put("DesktopSensor", 0.9f);
        leisureWeights.put("ProcessSensor", 0.4f);
        leisureWeights.put("DigitalSensor", 0.7f);
        contextRelevanceWeights.put(PerceptionSystem.ContextType.LEISURE, leisureWeights);

        // 通勤上下文：环境传感器更相关
        Map<String, Float> commuteWeights = new HashMap<>();
        commuteWeights.put("EnvironmentSensor", 0.95f);
        commuteWeights.put("UserSensor", 0.5f);
        commuteWeights.put("PlatformSensor", 0.7f);
        commuteWeights.put("DesktopSensor", 0.3f);
        commuteWeights.put("ProcessSensor", 0.3f);
        commuteWeights.put("DigitalSensor", 0.6f);
        contextRelevanceWeights.put(PerceptionSystem.ContextType.COMMUTE, commuteWeights);

        // 餐饮上下文：环境传感器和用户传感器更相关
        Map<String, Float> mealWeights = new HashMap<>();
        mealWeights.put("EnvironmentSensor", 0.9f);
        mealWeights.put("UserSensor", 0.7f);
        mealWeights.put("PlatformSensor", 0.4f);
        mealWeights.put("DesktopSensor", 0.5f);
        mealWeights.put("ProcessSensor", 0.3f);
        mealWeights.put("DigitalSensor", 0.5f);
        contextRelevanceWeights.put(PerceptionSystem.ContextType.MEAL, mealWeights);

        // 睡眠上下文：环境传感器更相关
        Map<String, Float> sleepWeights = new HashMap<>();
        sleepWeights.put("EnvironmentSensor", 0.95f);
        sleepWeights.put("UserSensor", 0.3f);
        sleepWeights.put("PlatformSensor", 0.4f);
        sleepWeights.put("DesktopSensor", 0.2f);
        sleepWeights.put("ProcessSensor", 0.2f);
        sleepWeights.put("DigitalSensor", 0.3f);
        contextRelevanceWeights.put(PerceptionSystem.ContextType.SLEEP, sleepWeights);

        // 仪式性活动：环境传感器和用户传感器更相关
        Map<String, Float> ritualWeights = new HashMap<>();
        ritualWeights.put("EnvironmentSensor", 0.85f);
        ritualWeights.put("UserSensor", 0.8f);
        ritualWeights.put("PlatformSensor", 0.5f);
        ritualWeights.put("DesktopSensor", 0.6f);
        ritualWeights.put("ProcessSensor", 0.5f);
        ritualWeights.put("DigitalSensor", 0.5f);
        contextRelevanceWeights.put(PerceptionSystem.ContextType.RITUAL, ritualWeights);
    }

    /**
     * S15-4: 获取传感器在当前上下文中的权重
     * @param sensorName 传感器名称
     * @param context 当前上下文类型
     * @return 加权权重 (0.0 - 1.0)
     */
    public float getSensorWeight(String sensorName, PerceptionSystem.ContextType context) {
        // 1. 获取上下文相关性权重
        Map<String, Float> contextWeights = contextRelevanceWeights.getOrDefault(context, new HashMap<>());
        float contextWeight = contextWeights.getOrDefault(sensorName, 0.5f);

        // 2. 获取传感器准确性权重
        SensorAccuracy sensorAccuracy = sensorAccuracyMap.get(sensorName);
        float accuracyWeight = sensorAccuracy != null ? sensorAccuracy.getAccuracy() : 0.5f;

        // 3. 综合计算权重 (上下文权重 * 准确性权重，归一化)
        float combinedWeight = contextWeight * accuracyWeight;

        // 4. 归一化到 0.3 - 1.0 范围（确保即使低准确性传感器也有最小权重）
        return Math.max(0.3f, Math.min(1.0f, combinedWeight * 1.5f));
    }

    /**
     * S15-4: 记录传感器准确性反馈
     * @param sensorName 传感器名称
     * @param wasAccurate 感知是否准确
     */
    public void recordSensorAccuracy(String sensorName, boolean wasAccurate) {
        SensorAccuracy accuracy = sensorAccuracyMap.get(sensorName);
        if (accuracy != null) {
            accuracy.update(wasAccurate);
        } else {
            sensorAccuracyMap.put(sensorName, new SensorAccuracy(wasAccurate ? 1.0f : 0.0f));
        }
    }

    /**
     * S15-4: 获取当前上下文类型
     */
    private PerceptionSystem.ContextType getCurrentContext() {
        if (lastFusedPerception != null && lastFusedPerception.environment() != null) {
            return lastFusedPerception.environment().context();
        }
        // 默认返回工作时间上下文
        LocalDateTime now = LocalDateTime.now();
        int hour = now.atZone(ZoneId.systemDefault()).getHour();
        if (hour >= 9 && hour <= 17) {
            return PerceptionSystem.ContextType.WORK;
        }
        return PerceptionSystem.ContextType.LEISURE;
    }

    /**
     * S15-4: 获取所有传感器的权重信息
     */
    public Map<String, Float> getAllSensorWeights() {
        PerceptionSystem.ContextType currentContext = getCurrentContext();
        Map<String, Float> weights = new HashMap<>();

        for (String sensorName : List.of("EnvironmentSensor", "UserSensor", "PlatformSensor",
                "DesktopSensor", "ProcessSensor", "DigitalSensor")) {
            weights.put(sensorName, getSensorWeight(sensorName, currentContext));
        }

        return weights;
    }

    /**
     * 融合当前感知与历史感知
     */
    public PerceptionSystem.Perception fuse(
        PerceptionSystem.Perception currentPerception,
        List<MemorySystem.Stimulus> historicalStimuli
    ) {
        if (historicalStimuli == null || historicalStimuli.isEmpty()) {
            lastFusedPerception = currentPerception;
            return currentPerception;
        }

        List<PerceptionSystem.Perception> historicalPerceptions = new ArrayList<>();
        for (MemorySystem.Stimulus s : historicalStimuli) {
            if (s.content() instanceof PerceptionSystem.Perception p) {
                historicalPerceptions.add(p);
            }
        }

        if (historicalPerceptions.isEmpty()) {
            lastFusedPerception = currentPerception;
            return currentPerception;
        }

        List<PerceptionSystem.Perception> last5 = historicalPerceptions.size() > 5 ?
            historicalPerceptions.subList(historicalPerceptions.size() - 5, historicalPerceptions.size()) :
            historicalPerceptions;

        PerceptionSystem.Perception fused = fuseWithHistory(currentPerception, last5);
        lastFusedPerception = fused;
        return fused;
    }

    private PerceptionSystem.Perception fuseWithHistory(
        PerceptionSystem.Perception current,
        List<PerceptionSystem.Perception> history
    ) {
        return new PerceptionSystem.Perception(
            Instant.now(),
            fusePlatform(current.platform(), history.stream().map(PerceptionSystem.Perception::platform).toList()),
            fuseUser(current.user(), history.stream().map(PerceptionSystem.Perception::user).toList()),
            fuseEnvironment(current.environment(), history.stream().map(PerceptionSystem.Perception::environment).toList()),
            fuseDesktop(current.desktop(), history.stream().map(PerceptionSystem.Perception::desktop).toList()),
            fuseProcesses(current.processes(), history.stream().map(PerceptionSystem.Perception::processes).toList()),
            fuseDigital(current.digital(), history.stream().map(PerceptionSystem.Perception::digital).toList())
        );
    }

    private PerceptionSystem.PlatformPerception fusePlatform(
        PerceptionSystem.PlatformPerception current,
        List<PerceptionSystem.PlatformPerception> history
    ) {
        if (current == null) {
            return history.isEmpty() ? null : history.get(history.size() - 1);
        }
        if (history.isEmpty()) return current;

        List<PerceptionSystem.PlatformPerception> histWithMem = new ArrayList<>();
        for (PerceptionSystem.PlatformPerception p : history) {
            if (p != null && p.memory() != null) histWithMem.add(p);
        }

        PerceptionSystem.MemoryStatus fusedMemory = current.memory();
        if (fusedMemory != null && !histWithMem.isEmpty()) {
            PerceptionSystem.MemoryStatus histMem = histWithMem.get(histWithMem.size() - 1).memory();
            float alpha = fusionFactor;
            fusedMemory = new PerceptionSystem.MemoryStatus(
                current.memory().totalMb(),
                (long) ((current.memory().usedMb() * alpha) + (histMem.usedMb() * (1 - alpha))),
                (current.memory().usedPercent() * alpha) + (histMem.usedPercent() * (1 - alpha))
            );
        }

        List<PerceptionSystem.PlatformPerception> histWithCpu = new ArrayList<>();
        for (PerceptionSystem.PlatformPerception p : history) {
            if (p != null && p.cpu() != null) histWithCpu.add(p);
        }

        PerceptionSystem.CpuStatus fusedCpu = current.cpu();
        if (fusedCpu != null && !histWithCpu.isEmpty()) {
            PerceptionSystem.CpuStatus histCpu = histWithCpu.get(histWithCpu.size() - 1).cpu();
            float alpha = fusionFactor;
            fusedCpu = new PerceptionSystem.CpuStatus(
                (current.cpu().loadPercent() * alpha) + (histCpu.loadPercent() * (1 - alpha)),
                current.cpu().temperature() != null ? current.cpu().temperature() : histCpu.temperature(),
                current.cpu().coreCount()
            );
        }

        PerceptionSystem.BatteryStatus battery = current.battery();
        if (battery == null) {
            for (PerceptionSystem.PlatformPerception p : history) {
                if (p != null && p.battery() != null) {
                    battery = p.battery();
                    break;
                }
            }
        }

        PerceptionSystem.NetworkStatus network = current.network();
        if (network == null) {
            for (PerceptionSystem.PlatformPerception p : history) {
                if (p != null && p.network() != null) {
                    network = p.network();
                    break;
                }
            }
        }

        return new PerceptionSystem.PlatformPerception(
            current.deviceId(),
            current.deviceType(),
            current.hostname(),
            fusedMemory,
            current.disk(),
            battery,
            fusedCpu,
            network,
            current.osVersion(),
            current.uptime()
        );
    }

    private PerceptionSystem.UserPerception fuseUser(
        PerceptionSystem.UserPerception current,
        List<PerceptionSystem.UserPerception> history
    ) {
        if (current == null) {
            return history.isEmpty() ? null : history.get(history.size() - 1);
        }
        if (history.isEmpty()) return current;

        PerceptionSystem.PresenceStatus finalPresence = current.presence();
        long histActiveCount = history.stream()
            .filter(u -> u != null && u.presence() == PerceptionSystem.PresenceStatus.ACTIVE)
            .count();
        if (current.presence() == PerceptionSystem.PresenceStatus.ACTIVE ||
            histActiveCount > history.size() / 2) {
            finalPresence = PerceptionSystem.PresenceStatus.ACTIVE;
        }

        PerceptionSystem.WindowInfo activeWindow = current.activeWindow();
        if (activeWindow == null) {
            for (PerceptionSystem.UserPerception u : history) {
                if (u != null && u.activeWindow() != null) {
                    activeWindow = u.activeWindow();
                    break;
                }
            }
        }

        return new PerceptionSystem.UserPerception(
            activeWindow,
            finalPresence,
            current.recentCommands(),
            current.voiceInput(),
            current.gestures()
        );
    }

    private PerceptionSystem.EnvironmentPerception fuseEnvironment(
        PerceptionSystem.EnvironmentPerception current,
        List<PerceptionSystem.EnvironmentPerception> history
    ) {
        if (current == null) {
            return history.isEmpty() ? null : history.get(history.size() - 1);
        }
        if (history.isEmpty()) return current;

        PerceptionSystem.EnvironmentPerception lastHist = history.get(history.size() - 1);
        PerceptionSystem.ContextType context = current.context();
        if (lastHist != null &&
            current.hourOfDay() == lastHist.hourOfDay() &&
            current.dayOfWeek() == lastHist.dayOfWeek()) {
            context = lastHist.context();
        }

        return new PerceptionSystem.EnvironmentPerception(
            current.timestamp(),
            current.hourOfDay(),
            current.dayOfWeek(),
            current.location(),
            current.weather(),
            context
        );
    }

    private PerceptionSystem.DesktopPerception fuseDesktop(
        PerceptionSystem.DesktopPerception current,
        List<PerceptionSystem.DesktopPerception> history
    ) {
        if (current == null) {
            return history.isEmpty() ? null : history.get(history.size() - 1);
        }
        return current;
    }

    private PerceptionSystem.ProcessPerception fuseProcesses(
        PerceptionSystem.ProcessPerception current,
        List<PerceptionSystem.ProcessPerception> history
    ) {
        if (current == null) {
            return history.isEmpty() ? null : history.get(history.size() - 1);
        }
        if (history.isEmpty()) return current;

        int totalRunning = current.totalRunning();
        for (int i = history.size() - 1; i >= 0; i--) {
            PerceptionSystem.ProcessPerception p = history.get(i);
            if (p != null && p.totalRunning() > 0) {
                float alpha = fusionFactor;
                totalRunning = (int) ((current.totalRunning() * alpha) + (p.totalRunning() * (1 - alpha)));
                break;
            }
        }

        return new PerceptionSystem.ProcessPerception(current.topProcesses(), totalRunning);
    }

    private PerceptionSystem.DigitalPerception fuseDigital(
        PerceptionSystem.DigitalPerception current,
        List<PerceptionSystem.DigitalPerception> history
    ) {
        if (current == null) {
            return history.isEmpty() ? null : history.get(history.size() - 1);
        }
        return current;
    }

    public PerceptionSystem.Perception getLastFused() {
        return lastFusedPerception;
    }

    /**
     * S15-4: 获取基于上下文的加权融合因子
     * @param sensorName 传感器名称
     * @return 加权融合因子
     */
    public float getContextWeightedFusionFactor(String sensorName) {
        PerceptionSystem.ContextType currentContext = getCurrentContext();
        float baseWeight = getSensorWeight(sensorName, currentContext);
        // 将权重映射到融合因子范围 [0.5, 0.9]
        return 0.5f + (baseWeight * 0.4f);
    }
}
