package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.WorldModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 感知融合 - 多源感知 → 统一输入
 *
 * ## 融合策略
 *
 * 1. **时序加权**：最近感知权重更高（指数移动平均）
 * 2. **类型融合**：合并不同类型的感知数据
 * 3. **冲突解决**：当冲突时，使用最新数据或更高置信度
 */
public class PerceptionFusion {
    private final float fusionFactor;
    private PerceptionSystem.Perception lastFusedPerception = null;

    public PerceptionFusion() {
        this(0.7f);
    }

    public PerceptionFusion(float fusionFactor) {
        this.fusionFactor = fusionFactor;
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
}
