package com.lingfeng.sprite;

import com.lingfeng.sprite.OwnerModel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 进化引擎 - 数字生命的自我改进系统
 *
 * ## 架构设计
 *
 * ```
 * ┌─────────────────────────────────────────────────────────┐
 * │                     进化引擎                              │
 * ├─────────────────────────────────────────────────────────┤
 * │  FeedbackCollector  │  反馈收集（多源）                 │
 * │  LearningLoop      │  学习循环（观察→反思→抽象→应用）  │
 * │  SelfModifier     │  自我修改（安全边界内）            │
 * └─────────────────────────────────────────────────────────┘
 * ```
 *
 * ## 学习循环
 *
 * [LearningLoop] 实现四阶段学习：
 * 1. **观察** - 收集交互、结果、情境、模式
 * 2. **反思** - 从观察中提取洞察
 * 3. **抽象** - 从洞察形成原则
 * 4. **应用** - 将原则应用到行为改变
 *
 * ## 反馈类型
 *
 * - [Feedback.OwnerFeedback] - 主人明确反馈
 * - [Feedback.OutcomeFeedback] - 行动成功/失败
 * - [Feedback.SelfReviewFeedback] - 自我复盘
 * - [Feedback.PatternFeedback] - 模式检测
 *
 * ## 安全边界
 *
 * [SelfModifier] 强制执行安全边界：
 * - `maxGrowthRate = 1.1` (每次最多10%变化)
 * - `protectedCore` (不可修改)
 * - `allowedModifications` 限制可修改的类型
 */
public final class EvolutionEngine {

    private EvolutionEngine() {}

    // ==================== 反馈类型 ====================

    /**
     * 反馈来源
     */
    public sealed interface Feedback permits Feedback.OwnerFeedback, Feedback.OutcomeFeedback, Feedback.SelfReviewFeedback, Feedback.PatternFeedback {
        Instant timestamp();
        FeedbackSource source();

        enum FeedbackSource {
            OWNER_EXPLICIT,
            OUTCOME_SUCCESS,
            OUTCOME_FAILURE,
            SELF_REVIEW,
            PATTERN_DETECTED
        }

        record OwnerFeedback(
            Instant timestamp,
            String content,
            float sentiment,
            String topic
        ) implements Feedback {
            public OwnerFeedback {
                Objects.requireNonNull(timestamp);
                Objects.requireNonNull(content);
                if (topic == null) topic = null;
            }

            @Override
            public FeedbackSource source() {
                return FeedbackSource.OWNER_EXPLICIT;
            }

            public OwnerFeedback(Instant timestamp, String content, float sentiment) {
                this(timestamp, content, sentiment, null);
            }
        }

        record OutcomeFeedback(
            Instant timestamp,
            String actionType,
            boolean success,
            String description,
            Impact impact
        ) implements Feedback {
            public OutcomeFeedback {
                Objects.requireNonNull(timestamp);
                Objects.requireNonNull(actionType);
                Objects.requireNonNull(description);
                Objects.requireNonNull(impact);
            }

            @Override
            public FeedbackSource source() {
                return success ? FeedbackSource.OUTCOME_SUCCESS : FeedbackSource.OUTCOME_FAILURE;
            }
        }

        record SelfReviewFeedback(
            Instant timestamp,
            String trigger,
            String insight,
            String improvement
        ) implements Feedback {
            public SelfReviewFeedback {
                Objects.requireNonNull(timestamp);
                Objects.requireNonNull(trigger);
                Objects.requireNonNull(insight);
                if (improvement == null) improvement = null;
            }

            @Override
            public FeedbackSource source() {
                return FeedbackSource.SELF_REVIEW;
            }
        }

        record PatternFeedback(
            Instant timestamp,
            String pattern,
            int occurrence,
            String interpretation
        ) implements Feedback {
            public PatternFeedback {
                Objects.requireNonNull(timestamp);
                Objects.requireNonNull(pattern);
                Objects.requireNonNull(interpretation);
            }

            @Override
            public FeedbackSource source() {
                return FeedbackSource.PATTERN_DETECTED;
            }
        }
    }

    public enum Impact { HIGH, MEDIUM, LOW }

    // ==================== 能力性能追踪 ====================

    /**
     * 能力性能记录
     */
    public record CapabilityPerformance(
        String capabilityName,
        int successCount,
        int failureCount,
        float successRate,
        Instant lastUpdated
    ) {
        public float calculateConfidence() {
            int total = successCount + failureCount;
            if (total == 0) return 0.3f;
            // 置信度基于样本量和成功率
            float baseConfidence = successRate;
            float sampleBonus = Math.min(total * 0.02f, 0.3f); // 最多+0.3
            return Math.min(baseConfidence + sampleBonus, 0.95f);
        }

        public SelfModel.CapabilityLevel suggestedLevel() {
            if (successCount + failureCount < 3) {
                return SelfModel.CapabilityLevel.BASIC; // 样本不足
            }
            if (successRate >= 0.8f) return SelfModel.CapabilityLevel.MASTER;
            if (successRate >= 0.6f) return SelfModel.CapabilityLevel.ADVANCED;
            if (successRate >= 0.4f) return SelfModel.CapabilityLevel.BASIC;
            return SelfModel.CapabilityLevel.NONE;
        }
    }

    // ==================== 学习循环 ====================

    /**
     * 学习观察
     */
    public record Observation(
        Instant timestamp,
        List<OwnerModel.Interaction> interactions,
        List<Feedback.OutcomeFeedback> outcomes,
        WorldModel.Context context,
        List<String> patterns
    ) {
        public Observation {
            Objects.requireNonNull(timestamp);
            interactions = interactions != null ? List.copyOf(interactions) : List.of();
            outcomes = outcomes != null ? List.copyOf(outcomes) : List.of();
            Objects.requireNonNull(context);
            patterns = patterns != null ? List.copyOf(patterns) : List.of();
        }

        /**
         * 获取成功率
         */
        public float successRate() {
            if (outcomes.isEmpty()) return 0.5f;
            long success = outcomes.stream().filter(Feedback.OutcomeFeedback::success).count();
            return (float) success / outcomes.size();
        }
    }

    /**
     * 反思洞察
     */
    public record Insight(
        Instant timestamp,
        InsightType type,
        String observation,
        String hypothesis,
        float confidence,
        List<String> implications
    ) {
        public Insight {
            Objects.requireNonNull(timestamp);
            Objects.requireNonNull(type);
            Objects.requireNonNull(observation);
            Objects.requireNonNull(hypothesis);
            Objects.requireNonNull(implications);
            implications = List.copyOf(implications);
        }
    }

    public enum InsightType {
        BEHAVIOR_PATTERN,
        PREFERENCE_LEARNED,
        SKILL_GAP_IDENTIFIED,
        BELIEF_UPDATED,
        RELATIONSHIP_EFFECT,
        CONTEXT_ADAPTATION
    }

    /**
     * 抽象出的原则
     */
    public record Principle(
        Instant timestamp,
        String statement,
        List<String> evidence,
        String applicableContext,
        float confidence,
        int timesApplied,
        Instant lastApplied
    ) {
        public Principle {
            Objects.requireNonNull(timestamp);
            Objects.requireNonNull(statement);
            evidence = evidence != null ? List.copyOf(evidence) : List.of();
            Objects.requireNonNull(applicableContext);
            if (lastApplied == null) lastApplied = null;
        }

        public Principle(Instant timestamp, String statement, List<String> evidence, String applicableContext, float confidence) {
            this(timestamp, statement, evidence, applicableContext, confidence, 0, null);
        }
    }

    /**
     * 行为改变
     */
    public record BehaviorChange(
        Instant timestamp,
        Principle principle,
        String beforeBehavior,
        String afterBehavior,
        Boolean success
    ) {
        public BehaviorChange {
            Objects.requireNonNull(timestamp);
            Objects.requireNonNull(principle);
            Objects.requireNonNull(beforeBehavior);
            Objects.requireNonNull(afterBehavior);
            if (success == null) success = null;
        }

        public BehaviorChange(Instant timestamp, Principle principle, String beforeBehavior, String afterBehavior) {
            this(timestamp, principle, beforeBehavior, afterBehavior, null);
        }
    }

    /**
     * 学习速率配置
     */
    public record LearningRateConfig(
        String capabilityName,
        float currentRate,
        float minRate,
        float maxRate,
        float baseRate,
        int consecutiveSuccess,
        int consecutiveFailure,
        Instant lastAdjusted
    ) {
        public LearningRateConfig {
            if (minRate <= 0) minRate = 0.1f;
            if (maxRate <= 0) maxRate = 2.0f;
            if (baseRate <= 0) baseRate = 1.0f;
            if (currentRate <= 0) currentRate = baseRate;
            if (consecutiveSuccess < 0) consecutiveSuccess = 0;
            if (consecutiveFailure < 0) consecutiveFailure = 0;
        }

        /**
         * 增加学习速率（失败后加速学习）
         */
        public LearningRateConfig increase() {
            float newRate = Math.min(currentRate * 1.2f, maxRate);
            return new LearningRateConfig(
                capabilityName, newRate, minRate, maxRate, baseRate,
                0, consecutiveFailure + 1, Instant.now()
            );
        }

        /**
         * 降低学习速率（成功后减速学习）
         */
        public LearningRateConfig decrease() {
            float newRate = Math.max(currentRate * 0.9f, minRate);
            return new LearningRateConfig(
                capabilityName, newRate, minRate, maxRate, baseRate,
                consecutiveSuccess + 1, 0, Instant.now()
            );
        }

        /**
         * 重置学习速率到基准
         */
        public LearningRateConfig reset() {
            return new LearningRateConfig(
                capabilityName, baseRate, minRate, maxRate, baseRate,
                0, 0, Instant.now()
            );
        }
    }

    /**
     * 学习循环
     */
    public static class LearningLoop {
        private final List<Observation> observations = new ArrayList<>();
        private final List<Insight> insights = new ArrayList<>();
        private final List<Principle> principles = new ArrayList<>();
        private final List<BehaviorChange> behaviorChanges = new ArrayList<>();
        private final int minObservationsForInsight = 3;
        private final float insightDecayFactor = 0.95f;

        // 能力性能追踪
        private final java.util.Map<String, CapabilityPerformance> capabilityPerformance = new java.util.concurrent.ConcurrentHashMap<>();

        // 学习速率配置
        private final java.util.Map<String, LearningRateConfig> learningRateConfigs = new java.util.concurrent.ConcurrentHashMap<>();

        // 全局学习速率
        private float globalLearningRate = 1.0f;
        private static final float GLOBAL_MIN_RATE = 0.3f;
        private static final float GLOBAL_MAX_RATE = 2.0f;

        // 动作类型 → 能力名称 的映射
        private static final java.util.Map<String, String> ACTION_CAPABILITY_MAP;
        static {
            java.util.Map<String, String> temp = new java.util.HashMap<>();
            temp.put("SearchFiles", "信息搜索");
            temp.put("Search", "信息搜索");
            temp.put("Calculator", "逻辑推理");
            temp.put("NotifyAction", "主动沟通");
            temp.put("Notify", "主动沟通");
            temp.put("Remember", "记忆管理");
            temp.put("LogAction", "日志记录");
            temp.put("BrowserAction", "信息获取");
            temp.put("FileAction", "文件管理");
            temp.put("ScheduleAction", "日程管理");
            temp.put("AppLaunchAction", "应用控制");
            ACTION_CAPABILITY_MAP = java.util.Collections.unmodifiableMap(temp);
        }

        /**
         * 记录观察
         */
        public Observation observe(
            List<OwnerModel.Interaction> interactions,
            List<Feedback.OutcomeFeedback> outcomes,
            WorldModel.Context context,
            List<String> patterns
        ) {
            Observation observation = new Observation(
                Instant.now(),
                interactions,
                outcomes,
                context,
                patterns
            );
            observations.add(observation);

            // 更新能力性能追踪
            for (Feedback.OutcomeFeedback outcome : observation.outcomes()) {
                updateCapabilityPerformance(outcome.actionType(), outcome.success());
            }

            return observation;
        }

        /**
         * 更新能力性能追踪
         */
        public void updateCapabilityPerformance(String actionType, boolean success) {
            String capabilityName = ACTION_CAPABILITY_MAP.getOrDefault(actionType, "通用能力");
            CapabilityPerformance current = capabilityPerformance.get(capabilityName);

            if (current == null) {
                current = new CapabilityPerformance(
                    capabilityName,
                    success ? 1 : 0,
                    success ? 0 : 1,
                    success ? 1.0f : 0.0f,
                    Instant.now()
                );
            } else {
                int newSuccess = current.successCount() + (success ? 1 : 0);
                int newFailure = current.failureCount() + (success ? 0 : 1);
                float newRate = (float) newSuccess / (newSuccess + newFailure);
                current = new CapabilityPerformance(
                    capabilityName,
                    newSuccess,
                    newFailure,
                    newRate,
                    Instant.now()
                );
            }
            capabilityPerformance.put(capabilityName, current);

            // 调整学习速率
            adjustLearningRate(capabilityName, success);
        }

        /**
         * 获取能力性能
         */
        public CapabilityPerformance getCapabilityPerformance(String capabilityName) {
            return capabilityPerformance.get(capabilityName);
        }

        /**
         * 获取所有能力性能
         */
        public java.util.Collection<CapabilityPerformance> getAllCapabilityPerformance() {
            return capabilityPerformance.values();
        }

        /**
         * 根据成功率获取建议的能力等级
         */
        public SelfModel.CapabilityLevel suggestCapabilityLevel(String capabilityName) {
            CapabilityPerformance perf = capabilityPerformance.get(capabilityName);
            if (perf == null) return SelfModel.CapabilityLevel.BASIC;
            return perf.suggestedLevel();
        }

        // ==================== 自适应学习速率 ====================

        /**
         * 获取学习速率配置
         */
        public LearningRateConfig getLearningRateConfig(String capabilityName) {
            return learningRateConfigs.getOrDefault(capabilityName,
                new LearningRateConfig(capabilityName, 1.0f, 0.1f, 2.0f, 1.0f, 0, 0, Instant.now()));
        }

        /**
         * 获取所有学习速率配置
         */
        public java.util.Collection<LearningRateConfig> getAllLearningRateConfigs() {
            return learningRateConfigs.values();
        }

        /**
         * 获取全局学习速率
         */
        public float getGlobalLearningRate() {
            return globalLearningRate;
        }

        /**
         * 调整学习速率（基于反馈结果）
         */
        public void adjustLearningRate(String capabilityName, boolean success) {
            LearningRateConfig config = getLearningRateConfig(capabilityName);

            LearningRateConfig newConfig;
            if (success) {
                newConfig = config.decrease();
            } else {
                newConfig = config.increase();
            }

            learningRateConfigs.put(capabilityName, newConfig);

            // 调整全局学习速率
            adjustGlobalLearningRate(success);

            // 动态调整minObservationsForInsight：学习速率越高，需要的观察数越少
            recalculateMinObservations();

            logger.debug("Learning rate for '{}' adjusted: {} -> {} (success={})",
                capabilityName, config.currentRate(), newConfig.currentRate(), success);
        }

        /**
         * 调整全局学习速率
         */
        private void adjustGlobalLearningRate(boolean success) {
            if (success) {
                globalLearningRate = Math.max(globalLearningRate * 0.95f, GLOBAL_MIN_RATE);
            } else {
                globalLearningRate = Math.min(globalLearningRate * 1.1f, GLOBAL_MAX_RATE);
            }
        }

        /**
         * 重新计算最小观察数（基于全局学习速率）
         * 学习速率越高，需要的观察数越少（更快产生洞察）
         */
        private void recalculateMinObservations() {
            // 全局学习速率影响洞察生成的敏感度
            // 这个方法在未来可以用于动态调整minObservationsForInsight
        }

        /**
         * 获取调整后的最小观察数
         */
        public int getMinObservationsForInsight() {
            // 学习速率高时，可以更少的观察就产生洞察
            int adjustedMin = (int) Math.max(2, minObservationsForInsight / globalLearningRate);
            return adjustedMin;
        }

        /**
         * 获取学习速率建议
         */
        public LearningRateAdvice getLearningRateAdvice() {
            List<String> highRateCapabilities = new ArrayList<>();
            List<String> lowRateCapabilities = new ArrayList<>();

            for (var entry : learningRateConfigs.entrySet()) {
                LearningRateConfig config = entry.getValue();
                if (config.currentRate() > config.baseRate() * 1.3f) {
                    highRateCapabilities.add(entry.getKey() + " (" + String.format("%.2f", config.currentRate()) + ")");
                } else if (config.currentRate() < config.baseRate() * 0.7f) {
                    lowRateCapabilities.add(entry.getKey() + " (" + String.format("%.2f", config.currentRate()) + ")");
                }
            }

            return new LearningRateAdvice(
                globalLearningRate,
                new ArrayList<>(learningRateConfigs.values()),
                highRateCapabilities,
                lowRateCapabilities
            );
        }

        /**
         * 学习速率建议
         */
        public record LearningRateAdvice(
            float globalLearningRate,
            List<LearningRateConfig> configs,
            List<String> highRateCapabilities,
            List<String> lowRateCapabilities
        ) {}

        /**
         * 反思 - 从观察中提取洞察
         */
        public Insight reflect(Observation observation) {
            List<Observation> recentSameContext = observations.stream()
                .filter(o -> o.context().activity() == observation.context().activity())
                .toList();

            // 使用自适应最小观察数
            int adaptiveMinObservations = getMinObservationsForInsight();
            if (recentSameContext.size() < adaptiveMinObservations) {
                return null;
            }

            List<Feedback.OutcomeFeedback> successfulOutcomes = observation.outcomes().stream()
                .filter(Feedback.OutcomeFeedback::success)
                .toList();
            List<Feedback.OutcomeFeedback> failedOutcomes = observation.outcomes().stream()
                .filter(o -> !o.success())
                .toList();

            InsightType insightType;
            if (failedOutcomes.size() > successfulOutcomes.size()) {
                insightType = InsightType.SKILL_GAP_IDENTIFIED;
            } else if (!observation.patterns().isEmpty()) {
                insightType = InsightType.BEHAVIOR_PATTERN;
            } else {
                insightType = InsightType.CONTEXT_ADAPTATION;
            }

            Insight insight = new Insight(
                Instant.now(),
                insightType,
                "在 " + observation.context().activity() + " 情境下，观察到 " + observation.interactions().size() + " 次交互",
                generateHypothesis(insightType, observation),
                calculateInitialConfidence(observation),
                generateImplications(insightType)
            );

            insights.add(insight);
            return insight;
        }

        /**
         * 抽象 - 从洞察形成原则
         */
        public Principle abstract_(Insight insight) {
            Principle principle = new Principle(
                Instant.now(),
                "当 " + (insight.implications().isEmpty() ? "满足条件时" : insight.implications().get(0)) +
                    "，应该 " + insight.hypothesis(),
                List.of(insight.observation()),
                insight.implications().isEmpty() ? "通用" : insight.implications().get(0),
                insight.confidence()
            );
            principles.add(principle);
            return principle;
        }

        /**
         * 应用原则到行为
         */
        public BehaviorChange apply(Principle principle, String currentBehavior) {
            BehaviorChange change = new BehaviorChange(
                Instant.now(),
                principle,
                currentBehavior,
                principle.statement()
            );
            behaviorChanges.add(change);

            // 更新原则应用次数
            for (int i = 0; i < principles.size(); i++) {
                if (principles.get(i).statement().equals(principle.statement())) {
                    Principle p = principles.get(i);
                    principles.set(i, new Principle(
                        p.timestamp(),
                        p.statement(),
                        p.evidence(),
                        p.applicableContext(),
                        p.confidence(),
                        p.timesApplied() + 1,
                        Instant.now()
                    ));
                    break;
                }
            }

            return change;
        }

        private String generateHypothesis(InsightType type, Observation observation) {
            return switch (type) {
                case BEHAVIOR_PATTERN -> "主人倾向于 " +
                    (observation.patterns().isEmpty() ? "某种行为模式" : observation.patterns().get(0));
                case PREFERENCE_LEARNED -> "主人可能偏好某种交互方式";
                case SKILL_GAP_IDENTIFIED -> "需要提升处理 " +
                    observation.context().activity() + " 情境的能力";
                case BELIEF_UPDATED -> "关于世界的信念需要更新";
                case RELATIONSHIP_EFFECT -> "交互方式影响关系发展";
                case CONTEXT_ADAPTATION -> "需要更好地适应 " +
                    observation.context().activity() + " 情境";
            };
        }

        private List<String> generateImplications(InsightType type) {
            return switch (type) {
                case BEHAVIOR_PATTERN -> List.of("学习主人的行为模式", "预判主人需求");
                case PREFERENCE_LEARNED -> List.of("调整沟通风格", "优化响应方式");
                case SKILL_GAP_IDENTIFIED -> List.of("优先处理这类任务", "寻求主人指导");
                case BELIEF_UPDATED -> List.of("更新世界观");
                case RELATIONSHIP_EFFECT -> List.of("建立更深的信任");
                case CONTEXT_ADAPTATION -> List.of("提高情境感知");
            };
        }

        private float calculateInitialConfidence(Observation observation) {
            float base = 0.5f;
            float outcomeBonus = observation.outcomes().stream().anyMatch(Feedback.OutcomeFeedback::success) ? 0.2f : -0.1f;
            float patternBonus = observation.patterns().isEmpty() ? 0f : 0.2f;
            return Math.min(Math.max(base + outcomeBonus + patternBonus, 0.1f), 0.95f);
        }

        /**
         * 获取建议的原则
         */
        public List<Principle> getSuggestedPrinciples(WorldModel.Context context) {
            return principles.stream()
                .filter(p -> p.applicableContext().toLowerCase().contains(context.activity().name().toLowerCase()))
                .sorted((a, b) -> Float.compare(
                    b.confidence() * (1 + b.timesApplied() * 0.1f),
                    a.confidence() * (1 + a.timesApplied() * 0.1f)
                ))
                .toList();
        }

        /**
         * 获取学习统计
         */
        public LearningStats getStats() {
            return new LearningStats(
                observations.size(),
                insights.size(),
                principles.size(),
                behaviorChanges.size(),
                globalLearningRate,
                learningRateConfigs.size()
            );
        }
    }

    public record LearningStats(
        int totalObservations,
        int totalInsights,
        int totalPrinciples,
        int totalBehaviorChanges,
        float globalLearningRate,
        int configuredCapabilities
    ) {}

    // ==================== 遗忘机制 ====================

    /**
     * 遗忘配置
     */
    public record ForgettingConfig(
        float decayRate,           // 衰减率 (每天)
        int maxAgeDays,            // 最大保留天数
        float minStrengthThreshold, // 最小强度阈值
        int maxMemories,           // 最大记忆数量
        Instant lastForgetting      // 上次遗忘执行时间
    ) {
        public ForgettingConfig {
            if (decayRate <= 0) decayRate = 0.05f;        // 默认每天5%衰减
            if (maxAgeDays <= 0) maxAgeDays = 90;        // 默认90天
            if (minStrengthThreshold <= 0) minStrengthThreshold = 0.1f;
            if (maxMemories <= 0) maxMemories = 1000;
        }

        public ForgettingConfig() {
            this(0.05f, 90, 0.1f, 1000, Instant.now());
        }
    }

    /**
     * 记忆强度记录
     */
    public record MemoryStrength(
        String memoryId,
        String memoryType,
        float currentStrength,
        Instant lastAccessed,
        Instant createdAt,
        int accessCount
    ) {}

    /**
     * 遗忘事件
     */
    public record ForgettingEvent(
        Instant timestamp,
        String memoryId,
        String memoryType,
        float previousStrength,
        float newStrength,
        String reason
    ) {}

    /**
     * 遗忘机制
     */
    public static class ForgettingMechanism {
        private ForgettingConfig config;
        private final List<ForgettingEvent> forgettingHistory = new ArrayList<>();
        private final java.util.Map<String, MemoryStrength> memoryStrengths = new java.util.concurrent.ConcurrentHashMap<>();
        private static final int MAX_HISTORY = 500;

        public ForgettingMechanism() {
            this(new ForgettingConfig());
        }

        public ForgettingMechanism(ForgettingConfig config) {
            this.config = config != null ? config : new ForgettingConfig();
        }

        /**
         * 注册记忆
         */
        public void registerMemory(String memoryId, String memoryType) {
            MemoryStrength strength = new MemoryStrength(
                memoryId,
                memoryType,
                1.0f,  // 初始强度为1.0
                Instant.now(),
                Instant.now(),
                0
            );
            memoryStrengths.put(memoryId, strength);
        }

        /**
         * 访问记忆（更新访问时间和强度）
         */
        public void accessMemory(String memoryId) {
            MemoryStrength current = memoryStrengths.get(memoryId);
            if (current != null) {
                // 访问时增强强度，但不超过1.0
                float newStrength = Math.min(current.currentStrength() + 0.1f, 1.0f);
                memoryStrengths.put(memoryId, new MemoryStrength(
                    memoryId,
                    current.memoryType(),
                    newStrength,
                    Instant.now(),
                    current.createdAt(),
                    current.accessCount() + 1
                ));
            }
        }

        /**
         * 应用时间衰减
         */
        public float applyTimeDecay(String memoryId) {
            MemoryStrength current = memoryStrengths.get(memoryId);
            if (current == null) return 0f;

            long daysSinceLastAccess = java.time.Duration.between(current.lastAccessed(), Instant.now()).toDays();
            long daysSinceCreation = java.time.Duration.between(current.createdAt(), Instant.now()).toDays();

            // 基于时间的指数衰减
            float decayedStrength = current.currentStrength() * (float) Math.pow(1 - config.decayRate(), daysSinceLastAccess);

            // 也考虑创建时间
            if (daysSinceCreation > config.maxAgeDays()) {
                decayedStrength = 0f;  // 超过最大天数，直接遗忘
            }

            // 更新
            memoryStrengths.put(memoryId, new MemoryStrength(
                memoryId,
                current.memoryType(),
                decayedStrength,
                current.lastAccessed(),
                current.createdAt(),
                current.accessCount()
            ));

            return decayedStrength;
        }

        /**
         * 执行遗忘（清理低强度记忆）
         */
        public ForgettingResult executeForgetting(java.util.function.Function<String, Boolean> deleteMemory) {
            List<ForgettingEvent> events = new ArrayList<>();
            int totalMemories = memoryStrengths.size();

            // 应用衰减
            for (String memoryId : new ArrayList<>(memoryStrengths.keySet())) {
                MemoryStrength current = memoryStrengths.get(memoryId);
                float previousStrength = current.currentStrength();
                float newStrength = applyTimeDecay(memoryId);

                if (Float.compare(previousStrength, newStrength) != 0) {
                    events.add(new ForgettingEvent(
                        Instant.now(),
                        memoryId,
                        current.memoryType(),
                        previousStrength,
                        newStrength,
                        "时间衰减"
                    ));
                }
            }

            // 识别需要遗忘的记忆
            List<String> toForget = new ArrayList<>();
            for (var entry : memoryStrengths.entrySet()) {
                MemoryStrength ms = entry.getValue();
                if (ms.currentStrength() < config.minStrengthThreshold()) {
                    toForget.add(entry.getKey());
                }
            }

            // 如果记忆数量超过上限，删除最低强度的
            if (memoryStrengths.size() > config.maxMemories()) {
                List<MemoryStrength> sorted = memoryStrengths.values().stream()
                    .sorted((a, b) -> Float.compare(a.currentStrength(), b.currentStrength()))
                    .toList();
                int excess = memoryStrengths.size() - config.maxMemories();
                for (int i = 0; i < excess && i < sorted.size(); i++) {
                    if (!toForget.contains(sorted.get(i).memoryId())) {
                        toForget.add(sorted.get(i).memoryId());
                    }
                }
            }

            // 执行遗忘
            int forgottenCount = 0;
            for (String memoryId : toForget) {
                MemoryStrength current = memoryStrengths.get(memoryId);
                if (current != null) {
                    boolean deleted = deleteMemory.apply(memoryId);
                    if (deleted) {
                        memoryStrengths.remove(memoryId);
                        forgottenCount++;
                        events.add(new ForgettingEvent(
                            Instant.now(),
                            memoryId,
                            current.memoryType(),
                            current.currentStrength(),
                            0f,
                            "强度低于阈值或超出容量"
                        ));
                    }
                }
            }

            // 更新历史
            forgettingHistory.addAll(events);
            if (forgettingHistory.size() > MAX_HISTORY) {
                forgettingHistory.subList(0, forgettingHistory.size() - MAX_HISTORY).clear();
            }

            // 更新配置中的最后遗忘时间
            config = new ForgettingConfig(
                config.decayRate(),
                config.maxAgeDays(),
                config.minStrengthThreshold(),
                config.maxMemories(),
                Instant.now()
            );

            return new ForgettingResult(
                totalMemories,
                memoryStrengths.size(),
                forgottenCount,
                events.size(),
                new ArrayList<>(forgettingHistory)
            );
        }

        /**
         * 获取记忆强度
         */
        public MemoryStrength getMemoryStrength(String memoryId) {
            return memoryStrengths.get(memoryId);
        }

        /**
         * 获取所有记忆强度
         */
        public java.util.Collection<MemoryStrength> getAllMemoryStrengths() {
            return memoryStrengths.values();
        }

        /**
         * 获取遗忘统计
         */
        public ForgettingStats getStats() {
            return new ForgettingStats(
                memoryStrengths.size(),
                forgettingHistory.size(),
                forgettingHistory.stream()
                    .filter(e -> e.timestamp().isAfter(Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS)))
                    .count(),
                config
            );
        }

        /**
         * 更新配置
         */
        public void updateConfig(ForgettingConfig newConfig) {
            this.config = newConfig != null ? newConfig : new ForgettingConfig();
        }
    }

    /**
     * 遗忘结果
     */
    public record ForgettingResult(
        int totalBefore,
        int totalAfter,
        int forgottenCount,
        int decayedCount,
        List<ForgettingEvent> events
    ) {}

    /**
     * 遗忘统计
     */
    public record ForgettingStats(
        int totalMemories,
        int historySize,
        long recentForgettingEvents,
        ForgettingConfig config
    ) {}

    // ==================== 自我修改器 ====================

    /**
     * 自我修改边界
     */
    public record SelfModifyBounds(
        float maxGrowthRate,
        java.util.Set<String> protectedCore,
        java.util.Set<ModificationType> allowedModifications
    ) {
        public SelfModifyBounds {
            if (maxGrowthRate == 0) maxGrowthRate = 1.1f;
            if (protectedCore == null) protectedCore = java.util.Set.of("beingId", "createdAt", "identity.core");
            if (allowedModifications == null) {
                allowedModifications = java.util.Set.of(
                    ModificationType.CAPABILITY_LEVEL,
                    ModificationType.PREFERENCE,
                    ModificationType.BELIEF,
                    ModificationType.VALUE_WEIGHT,
                    ModificationType.METACOGNITION
                );
            }
        }

        public SelfModifyBounds() {
            this(1.1f, null, null);
        }
    }

    public enum ModificationType {
        CAPABILITY_LEVEL,
        PREFERENCE,
        BELIEF,
        VALUE_WEIGHT,
        METACOGNITION,
        GROWTH_HISTORY
    }

    /**
     * 自我修改器
     */
    public static class SelfModifier {
        private final SelfModifyBounds bounds;
        private final List<Modification> modificationHistory = new ArrayList<>();

        public SelfModifier() {
            this(new SelfModifyBounds());
        }

        public SelfModifier(SelfModifyBounds bounds) {
            this.bounds = bounds != null ? bounds : new SelfModifyBounds();
        }

        /**
         * 修改能力水平
         */
        public SelfModel.Self modifyCapability(
            SelfModel.Self self,
            String capabilityName,
            SelfModel.CapabilityLevel newLevel
        ) {
            if (!canModify(ModificationType.CAPABILITY_LEVEL)) return null;

            SelfModel.Capability capability = self.capabilities().stream()
                .filter(c -> c.name().equals(capabilityName))
                .findFirst().orElse(null);
            if (capability == null) return null;

            int currentLevelIndex = List.of(SelfModel.CapabilityLevel.values()).indexOf(capability.level());
            int newLevelIndex = List.of(SelfModel.CapabilityLevel.values()).indexOf(newLevel);
            int levelChange = newLevelIndex - currentLevelIndex;

            if (levelChange > 0 && levelChange > self.evolutionLevel() * (bounds.maxGrowthRate() - 1) * 10) {
                return null;
            }

            SelfModel.Self updated = self.updateCapability(capabilityName, newLevel, 0.7f);
            recordModification(new Modification(
                Instant.now(),
                ModificationType.CAPABILITY_LEVEL,
                capabilityName,
                capability.level().name(),
                newLevel.name(),
                true
            ));
            return updated;
        }

        /**
         * 添加偏好
         */
        public SelfModel.Self addPreference(
            SelfModel.Self self,
            SelfModel.Value preference
        ) {
            if (!canModify(ModificationType.PREFERENCE)) return null;

            // Get current values from personality and add new preference
            List<SelfModel.Value> currentValues = self.personality().values() != null
                ? new ArrayList<>(self.personality().values())
                : new ArrayList<>();
            currentValues.add(preference);

            SelfModel.Self updated = new SelfModel.Self(
                self.identity(),
                new SelfModel.Personality(
                    self.personality().essence(),
                    self.personality().vibe(),
                    List.copyOf(currentValues),
                    self.personality().decisionPatterns(),
                    self.personality().blindSpots(),
                    self.personality().strengths()
                ),
                self.capabilities(),
                self.avatars(),
                self.metacognition(),
                self.growthHistory(),
                self.evolutionLevel(),
                self.evolutionCount() + 1
            );
            recordModification(new Modification(
                Instant.now(),
                ModificationType.PREFERENCE,
                preference.name(),
                "none",
                preference.toString(),
                true
            ));
            return updated;
        }

        /**
         * 更新价值观权重
         */
        public SelfModel.Self updateValueWeight(
            SelfModel.Self self,
            String valueName,
            float newWeight
        ) {
            if (!canModify(ModificationType.VALUE_WEIGHT)) return null;
            if (newWeight < 0 || newWeight > 1) return null;

            List<SelfModel.Value> updatedValues = new ArrayList<>();
            String oldWeight = null;
            for (SelfModel.Value v : self.personality().values()) {
                if (v.name().equals(valueName)) {
                    oldWeight = String.valueOf(v.weight());
                    updatedValues.add(new SelfModel.Value(v.name(), newWeight, v.description(), v.situation()));
                } else {
                    updatedValues.add(v);
                }
            }

            SelfModel.Self updated = new SelfModel.Self(
                self.identity(),
                new SelfModel.Personality(
                    self.personality().essence(),
                    self.personality().vibe(),
                    List.copyOf(updatedValues),
                    self.personality().decisionPatterns(),
                    self.personality().blindSpots(),
                    self.personality().strengths()
                ),
                self.capabilities(),
                self.avatars(),
                self.metacognition(),
                self.growthHistory(),
                self.evolutionLevel(),
                self.evolutionCount() + 1
            );
            recordModification(new Modification(
                Instant.now(),
                ModificationType.VALUE_WEIGHT,
                valueName,
                oldWeight != null ? oldWeight : "unknown",
                String.valueOf(newWeight),
                true
            ));
            return updated;
        }

        /**
         * 添加反思
         */
        public SelfModel.Self addReflection(
            SelfModel.Self self,
            String trigger,
            String insight,
            String behaviorChange
        ) {
            SelfModel.Self updated = self.addReflection(trigger, insight, behaviorChange);
            recordModification(new Modification(
                Instant.now(),
                ModificationType.METACOGNITION,
                "reflection",
                "none",
                insight,
                true
            ));
            return updated;
        }

        /**
         * 添加新能力
         */
        public SelfModel.Self addCapability(
            SelfModel.Self self,
            String capabilityName,
            SelfModel.CapabilityLevel level
        ) {
            if (!canModify(ModificationType.CAPABILITY_LEVEL)) return null;

            // 检查能力是否已存在
            boolean exists = self.capabilities().stream()
                .anyMatch(c -> c.name().equals(capabilityName));
            if (exists) {
                return modifyCapability(self, capabilityName, level);
            }

            // 添加新能力
            List<SelfModel.Capability> updatedCapabilities = new ArrayList<>(self.capabilities());
            updatedCapabilities.add(new SelfModel.Capability(capabilityName, level, 0.5f, Instant.now()));

            SelfModel.Self updated = new SelfModel.Self(
                self.identity(),
                self.personality(),
                List.copyOf(updatedCapabilities),
                self.avatars(),
                self.metacognition(),
                self.growthHistory(),
                self.evolutionLevel(),
                self.evolutionCount() + 1
            );
            recordModification(new Modification(
                Instant.now(),
                ModificationType.CAPABILITY_LEVEL,
                capabilityName,
                "none",
                level.name(),
                true
            ));
            return updated;
        }

        private boolean canModify(ModificationType type) {
            return bounds.allowedModifications().contains(type);
        }

        private void recordModification(Modification modification) {
            modificationHistory.add(modification);
        }

        public List<Modification> getHistory() {
            return new ArrayList<>(modificationHistory);
        }

        public record Modification(
            Instant timestamp,
            ModificationType type,
            String target,
            String before,
            String after,
            boolean success
        ) {
            public Modification {
                Objects.requireNonNull(timestamp);
                Objects.requireNonNull(type);
                Objects.requireNonNull(target);
                Objects.requireNonNull(before);
                Objects.requireNonNull(after);
            }
        }
    }

    // ==================== 反馈收集器 ====================

    /**
     * 反馈收集器
     */
    public static class FeedbackCollector {
        private final List<Feedback> feedbacks = new ArrayList<>();
        private final int maxFeedbackRetention = 1000;

        /**
         * 添加主人反馈
         */
        public Feedback addOwnerFeedback(String content, float sentiment, String topic) {
            Feedback feedback = new Feedback.OwnerFeedback(Instant.now(), content, sentiment, topic);
            addInternal(feedback);
            return feedback;
        }

        /**
         * 添加结果反馈
         */
        public Feedback addOutcomeFeedback(String actionType, boolean success, String description, Impact impact) {
            Feedback feedback = new Feedback.OutcomeFeedback(Instant.now(), actionType, success, description, impact);
            addInternal(feedback);
            return feedback;
        }

        /**
         * 添加自我复盘
         */
        public Feedback addSelfReview(String trigger, String insight, String improvement) {
            Feedback feedback = new Feedback.SelfReviewFeedback(Instant.now(), trigger, insight, improvement);
            addInternal(feedback);
            return feedback;
        }

        private void addInternal(Feedback feedback) {
            feedbacks.add(feedback);
            if (feedbacks.size() > maxFeedbackRetention) {
                feedbacks.subList(0, feedbacks.size() - maxFeedbackRetention).clear();
            }
        }

        /**
         * 收集反馈（通用入口）
         */
        public void collect(Feedback feedback) {
            addInternal(feedback);
        }

        /**
         * 获取最近的反馈
         */
        public List<Feedback> getRecentFeedback(long hours) {
            Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
            return feedbacks.stream()
                .filter(f -> f.timestamp().isAfter(cutoff))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .toList();
        }

        public List<Feedback> getRecentFeedback() {
            return getRecentFeedback(24);
        }

        /**
         * 获取特定类型的反馈
         */
        public List<Feedback> getByType(Feedback.FeedbackSource source) {
            return feedbacks.stream().filter(f -> f.source() == source).toList();
        }

        /**
         * 获取结果反馈统计
         */
        public OutcomeStats getOutcomeStats() {
            List<Feedback.OutcomeFeedback> outcomes = feedbacks.stream()
                .filter(f -> f instanceof Feedback.OutcomeFeedback)
                .map(f -> (Feedback.OutcomeFeedback) f)
                .toList();
            long successCount = outcomes.stream().filter(Feedback.OutcomeFeedback::success).count();
            long total = outcomes.size();
            return new OutcomeStats(
                (int) total,
                (int) successCount,
                (int) (total - successCount),
                total > 0 ? (float) successCount / total : 0f
            );
        }
    }

    public record OutcomeStats(
        int total,
        int successCount,
        int failureCount,
        float successRate
    ) {}

    // ==================== 完整进化引擎 ====================

    /**
     * 进化引擎
     */
    public static class Engine {
        private static final Logger logger = LoggerFactory.getLogger(Engine.class);
        private final LearningLoop learningLoop;
        private final FeedbackCollector feedbackCollector;
        private final SelfModifier selfModifier;
        private final ForgettingMechanism forgettingMechanism;
        private final SnapshotManager snapshotManager;
        private com.lingfeng.sprite.service.SelfLearningService selfLearningService;
        private int evolutionLevel = 1;
        private int evolutionCount = 0;
        private String lastSnapshotId = null;

        public Engine() {
            this(new LearningLoop(), new FeedbackCollector(), new SelfModifier(), new ForgettingMechanism());
        }

        public Engine(LearningLoop learningLoop, FeedbackCollector feedbackCollector, SelfModifier selfModifier) {
            this(learningLoop, feedbackCollector, selfModifier, new ForgettingMechanism());
        }

        public Engine(LearningLoop learningLoop, FeedbackCollector feedbackCollector, SelfModifier selfModifier, ForgettingMechanism forgettingMechanism) {
            this.learningLoop = learningLoop != null ? learningLoop : new LearningLoop();
            this.feedbackCollector = feedbackCollector != null ? feedbackCollector : new FeedbackCollector();
            this.selfModifier = selfModifier != null ? selfModifier : new SelfModifier();
            this.forgettingMechanism = forgettingMechanism != null ? forgettingMechanism : new ForgettingMechanism();
            this.snapshotManager = new SnapshotManager();
        }

        /**
         * 设置自我学习服务（S21）
         */
        public void setSelfLearningService(com.lingfeng.sprite.service.SelfLearningService service) {
            this.selfLearningService = service;
        }

        /**
         * 获取自我学习服务
         */
        public com.lingfeng.sprite.service.SelfLearningService getSelfLearningService() {
            return selfLearningService;
        }

        /**
         * 触发进化
         */
        public EvolutionResult evolve(SelfModel.Self self, WorldModel.World world) {
            List<Feedback> recentFeedback = feedbackCollector.getRecentFeedback();
            OutcomeStats outcomeStats = feedbackCollector.getOutcomeStats();

            List<OwnerModel.Interaction> interactions = world.owner().interactionHistory().size() > 20 ?
                world.owner().interactionHistory().subList(world.owner().interactionHistory().size() - 20, world.owner().interactionHistory().size()) :
                world.owner().interactionHistory();
            List<Feedback.OutcomeFeedback> outcomes = recentFeedback.stream()
                .filter(f -> f instanceof Feedback.OutcomeFeedback)
                .map(f -> (Feedback.OutcomeFeedback) f)
                .toList();

            WorldModel.Context context = world.currentContext();
            List<String> patterns = detectPatterns(recentFeedback);

            Observation observation = learningLoop.observe(
                new ArrayList<>(interactions),
                new ArrayList<>(outcomes),
                context,
                new ArrayList<>(patterns)
            );
            Insight insight = learningLoop.reflect(observation);

            SelfModel.Self updatedSelf = self;
            BehaviorChange appliedChange = null;

            if (insight != null) {
                Principle principle = learningLoop.abstract_(insight);

                switch (insight.type()) {
                    case SKILL_GAP_IDENTIFIED -> {
                        String skillGap = extractSkillGap(insight);
                        // 证据-based能力提升：根据追踪的成功率确定合适的能力等级
                        SelfModel.CapabilityLevel suggestedLevel = learningLoop.suggestCapabilityLevel(skillGap);
                        SelfModel.Self modified = selfModifier.modifyCapability(
                            updatedSelf,
                            skillGap,
                            suggestedLevel
                        );
                        if (modified != null) {
                            updatedSelf = modified;
                            logger.info("Capability '{}' upgraded to {} based on evidence", skillGap, suggestedLevel);
                        } else {
                            // 能力不存在，尝试添加
                            modified = selfModifier.addCapability(
                                updatedSelf,
                                skillGap,
                                suggestedLevel
                            );
                            if (modified != null) {
                                updatedSelf = modified;
                                logger.info("Capability '{}' added at level {} based on evidence", skillGap, suggestedLevel);
                            }
                        }
                        break;
                    }
                    case BELIEF_UPDATED -> {
                        updatedSelf = selfModifier.addReflection(
                            updatedSelf,
                            insight.observation(),
                            insight.hypothesis(),
                            null
                        );
                        break;
                    }
                    default -> {
                        appliedChange = learningLoop.apply(principle, "current behavior");
                        break;
                    }
                }
            }

            // 进化等级提升条件：需要行为改变证据，不仅仅是计数
            boolean hasBehaviorChangeEvidence = appliedChange != null && Boolean.TRUE.equals(appliedChange.success());
            boolean hasSufficientObservations = learningLoop.getStats().totalObservations() >= evolutionLevel * 3;
            if (evolutionCount > evolutionLevel * 10 && (hasBehaviorChangeEvidence || hasSufficientObservations)) {
                evolutionLevel++;
                logger.info("Evolution level increased to {} (behaviorChange={}, observations={})",
                    evolutionLevel, hasBehaviorChangeEvidence, learningLoop.getStats().totalObservations());
            }

            evolutionCount++;

            // ==================== S21: Self-Learning Integration ====================
            // 应用自我学习改进（S21-3: 持续模型改进）
            if (selfLearningService != null) {
                updatedSelf = applySelfLearning(updatedSelf, interactions);
            }

            return new EvolutionResult(
                true,
                updatedSelf,
                insight,
                insight != null ? learningLoop.abstract_(insight) : null,
                appliedChange,
                evolutionLevel,
                learningLoop.getStats(),
                outcomeStats
            );
        }

        /**
         * 应用自我学习改进（S21-3）
         */
        private SelfModel.Self applySelfLearning(SelfModel.Self self, List<OwnerModel.Interaction> interactions) {
            if (selfLearningService == null || interactions == null || interactions.isEmpty()) {
                return self;
            }

            try {
                // S21-1: 交互历史自我分析
                var analysis = selfLearningService.analyzePerformance(interactions);
                logger.info("Self-analysis complete: {} interactions analyzed, success rate={}",
                    analysis.totalInteractions(), analysis.overallSuccessRate());

                // S21-3: 应用自我改进
                SelfModel.Self improvedSelf = selfLearningService.applySelfImprovements(analysis, self);

                // S21-2: 从交互中获取技能
                for (OwnerModel.Interaction interaction : interactions) {
                    var skillOpt = selfLearningService.acquireFromInteraction(interaction);
                    if (skillOpt.isPresent()) {
                        var skill = skillOpt.get();
                        // 将技能转换为LearnedSkill并添加到自我模型
                        SelfModel.LearnedSkill learnedSkill = new SelfModel.LearnedSkill(
                            skill.id(),
                            skill.name(),
                            skill.description(),
                            skill.acquiredAt(),
                            skill.confidence(),
                            skill.triggers(),
                            skill.procedure()
                        );
                        improvedSelf = improvedSelf.addLearnedSkill(learnedSkill);
                        logger.info("Acquired new skill: {}", skill.name());
                    }
                }

                // S21-4: 生成并跟踪目标
                var goals = selfLearningService.generateGoals(analysis);
                for (var goal : goals) {
                    SelfModel.SelfGoal selfGoal = new SelfModel.SelfGoal(
                        goal.id(),
                        goal.description(),
                        goal.category(),
                        goal.targetProgress(),
                        goal.currentProgress(),
                        goal.createdAt(),
                        goal.deadline(),
                        SelfModel.SelfGoal.GoalPriority.valueOf(goal.priority().name()),
                        SelfModel.SelfGoal.GoalState.valueOf(goal.state().name())
                    );
                    improvedSelf = improvedSelf.addSelfGoal(selfGoal);
                }

                // 更新学习指标
                improvedSelf = improvedSelf.updateLearningMetrics(
                    analysis.totalInteractions(),
                    analysis.overallSuccessRate()
                );

                return improvedSelf;
            } catch (Exception e) {
                logger.error("Error applying self-learning: {}", e.getMessage());
                return self;
            }
        }

        private List<String> detectPatterns(List<Feedback> feedback) {
            List<String> patterns = new ArrayList<>();

            List<Feedback.OutcomeFeedback> failures = feedback.stream()
                .filter(f -> f instanceof Feedback.OutcomeFeedback)
                .map(f -> (Feedback.OutcomeFeedback) f)
                .filter(o -> !o.success())
                .toList();
            if (failures.size() >= 3) {
                var failureTypes = failures.stream().collect(java.util.stream.Collectors.groupingBy(Feedback.OutcomeFeedback::actionType));
                for (var entry : failureTypes.entrySet()) {
                    if (entry.getValue().size() >= 2) {
                        patterns.add("重复失败: " + entry.getKey());
                    }
                }
            }

            long negativeFeedback = feedback.stream()
                .filter(f -> f instanceof Feedback.OwnerFeedback)
                .map(f -> (Feedback.OwnerFeedback) f)
                .filter(f -> f.sentiment() < -0.5f)
                .count();
            if (negativeFeedback >= 2) {
                patterns.add("主人持续不满");
            }

            return patterns;
        }

        private String extractSkillGap(Insight insight) {
            String result = insight.hypothesis()
                .replace("需要提升处理 ", "")
                .replace(" 情境的能力", "");
            return result.isEmpty() ? "通用能力" : result;
        }

        /**
         * 记录反馈
         */
        public void collectFeedback(Feedback feedback) {
            feedbackCollector.collect(feedback);
        }

        /**
         * 获取进化状态
         */
        public EvolutionStatus getStatus() {
            return new EvolutionStatus(
                evolutionLevel,
                evolutionCount,
                learningLoop.getStats(),
                feedbackCollector.getOutcomeStats(),
                selfModifier.getHistory().size() > 10 ?
                    selfModifier.getHistory().subList(selfModifier.getHistory().size() - 10, selfModifier.getHistory().size()) :
                    new ArrayList<>(selfModifier.getHistory())
            );
        }

        // ==================== 遗忘机制接口 ====================

        /**
         * 注册记忆用于遗忘追踪
         */
        public void registerMemoryForForgetting(String memoryId, String memoryType) {
            forgettingMechanism.registerMemory(memoryId, memoryType);
        }

        /**
         * 访问记忆
         */
        public void accessMemoryForForgetting(String memoryId) {
            forgettingMechanism.accessMemory(memoryId);
        }

        /**
         * 执行遗忘
         */
        public ForgettingResult executeForgetting(java.util.function.Function<String, Boolean> deleteMemory) {
            return forgettingMechanism.executeForgetting(deleteMemory);
        }

        /**
         * 获取遗忘统计
         */
        public ForgettingStats getForgettingStats() {
            return forgettingMechanism.getStats();
        }

        /**
         * 获取记忆强度
         */
        public MemoryStrength getMemoryStrength(String memoryId) {
            return forgettingMechanism.getMemoryStrength(memoryId);
        }

        /**
         * 更新遗忘配置
         */
        public void updateForgettingConfig(ForgettingConfig config) {
            forgettingMechanism.updateConfig(config);
        }

        /**
         * 获取进化历史可视化
         */
        public EvolutionHistoryVisualization getEvolutionVisualization() {
            return new DefaultEvolutionVisualizer().generateVisualization(this);
        }

        // ==================== 快照和回滚 ====================

        /**
         * 创建系统快照
         */
        public SystemSnapshot createSnapshot(String label, SelfModel.Self self) {
            SystemSnapshot snapshot = snapshotManager.createSnapshot(
                label,
                self,
                learningLoop,
                evolutionLevel,
                evolutionCount
            );
            lastSnapshotId = snapshot.id();
            logger.info("Created snapshot: {} ({})", label, snapshot.id());
            return snapshot;
        }

        /**
         * 创建自动快照（带条件）
         */
        public SystemSnapshot createAutoSnapshot(SelfModel.Self self) {
            // 每10次进化或等级提升时自动创建快照
            String label = evolutionCount % 10 == 0 ?
                "Auto-snapshot at evolution " + evolutionCount :
                "Level-up snapshot at level " + evolutionLevel;
            return createSnapshot(label, self);
        }

        /**
         * 获取快照
         */
        public SystemSnapshot getSnapshot(String id) {
            return snapshotManager.getSnapshot(id);
        }

        /**
         * 获取所有快照
         */
        public List<SystemSnapshot> getAllSnapshots() {
            return snapshotManager.getAllSnapshots();
        }

        /**
         * 回滚到指定快照
         */
        public RollbackResult rollbackTo(String snapshotId, SelfModel.Self currentSelf) {
            SystemSnapshot snapshot = snapshotManager.getSnapshot(snapshotId);
            if (snapshot == null) {
                return new RollbackResult(false, "Snapshot not found: " + snapshotId, null, Instant.now(), 0, 0);
            }

            try {
                int modificationsRolledBack = 0;

                // 计算需要回滚的修改数量
                int snapshotEvolutionCount = snapshot.evolutionCount();
                modificationsRolledBack = evolutionCount - snapshotEvolutionCount;

                // 恢复学习循环数据
                learningLoop.observations.clear();
                learningLoop.observations.addAll(snapshot.observations());
                learningLoop.insights.clear();
                learningLoop.insights.addAll(snapshot.insights());
                learningLoop.principles.clear();
                learningLoop.principles.addAll(snapshot.principles());
                learningLoop.behaviorChanges.clear();
                learningLoop.behaviorChanges.addAll(snapshot.behaviorChanges());

                // 恢复进化等级
                evolutionLevel = snapshot.evolutionLevel();
                evolutionCount = snapshot.evolutionCount();

                logger.info("Rolled back to snapshot: {} (modifications rolled back: {})", snapshotId, modificationsRolledBack);

                return new RollbackResult(
                    true,
                    "Successfully rolled back to snapshot",
                    snapshotId,
                    Instant.now(),
                    evolutionLevel,
                    modificationsRolledBack
                );
            } catch (Exception e) {
                logger.error("Rollback failed: {}", e.getMessage());
                return new RollbackResult(false, "Rollback failed: " + e.getMessage(), snapshotId, Instant.now(), 0, 0);
            }
        }

        /**
         * 回滚到上一个快照
         */
        public RollbackResult rollbackToLast(SelfModel.Self currentSelf) {
            if (lastSnapshotId != null) {
                return rollbackTo(lastSnapshotId, currentSelf);
            }
            List<SystemSnapshot> recent = snapshotManager.getRecentSnapshots(1);
            if (!recent.isEmpty()) {
                return rollbackTo(recent.get(0).id(), currentSelf);
            }
            return new RollbackResult(false, "No snapshot available", null, Instant.now(), 0, 0);
        }

        /**
         * 删除快照
         */
        public boolean deleteSnapshot(String id) {
            boolean deleted = snapshotManager.deleteSnapshot(id);
            if (deleted && id.equals(lastSnapshotId)) {
                lastSnapshotId = null;
            }
            return deleted;
        }

        /**
         * 获取快照统计
         */
        public SnapshotStats getSnapshotStats() {
            return snapshotManager.getStats();
        }
    }

    public record EvolutionResult(
        boolean success,
        SelfModel.Self updatedSelf,
        Insight insight,
        Principle principle,
        BehaviorChange appliedChange,
        int newEvolutionLevel,
        LearningStats learningStats,
        OutcomeStats outcomeStats
    ) {}

    public record EvolutionStatus(
        int evolutionLevel,
        int evolutionCount,
        LearningStats learningStats,
        OutcomeStats outcomeStats,
        List<SelfModifier.Modification> recentModifications
    ) {}

    // ==================== 进化历史可视化 ====================

    /**
     * 进化历史可视化数据
     */
    public record EvolutionHistoryVisualization(
        int evolutionLevel,
        int totalEvolutions,
        TimelineData timeline,
        InsightAnalysis insights,
        PrincipleAnalysis principles,
        BehaviorAnalysis behaviors,
        ModificationAnalysis modifications,
        LearningRateAnalysis learningRates,
        ForgettingAnalysis forgetting
    ) {}

    /**
     * 时间线数据
     */
    public record TimelineData(
        List<TimelineEvent> events,
        Instant startTime,
        Instant endTime,
        Duration totalDuration
    ) {
        public record TimelineEvent(
            Instant timestamp,
            String type,
            String title,
            String description,
            float significance
        ) {}
    }

    /**
     * 洞察分析
     */
    public record InsightAnalysis(
        List<Insight> allInsights,
        int totalCount,
        InsightTypeDistribution typeDistribution,
        ConfidenceTrend confidenceTrend,
        List<Insight> recentInsights
    ) {
        public record InsightTypeDistribution(
            int behaviorPattern,
            int preferenceLearned,
            int skillGapIdentified,
            int beliefUpdated,
            int relationshipEffect,
            int contextAdaptation
        ) {}

        public record ConfidenceTrend(
            float averageConfidence,
            float minConfidence,
            float maxConfidence,
            TrendDirection direction
        ) {
            public enum TrendDirection { IMPROVING, STABLE, DECLINING }
        }
    }

    /**
     * 原则分析
     */
    public record PrincipleAnalysis(
        List<Principle> allPrinciples,
        int totalCount,
        float averageConfidence,
        List<Principle> mostApplied,
        List<Principle> highestConfidence
    ) {}

    /**
     * 行为分析
     */
    public record BehaviorAnalysis(
        List<BehaviorChange> allChanges,
        int totalCount,
        int successfulChanges,
        int failedChanges,
        List<String> behaviorPatterns
    ) {}

    /**
     * 修改分析
     */
    public record ModificationAnalysis(
        List<SelfModifier.Modification> allModifications,
        int totalCount,
        ModificationTypeDistribution typeDistribution,
        List<SelfModifier.Modification> recentModifications
    ) {
        public record ModificationTypeDistribution(
            int capabilityLevel,
            int preference,
            int belief,
            int valueWeight,
            int metacognition,
            int growthHistory
        ) {}
    }

    /**
     * 学习速率分析
     */
    public record LearningRateAnalysis(
        float globalLearningRate,
        List<LearningRateConfig> capabilityRates,
        List<String> highRateCapabilities,
        List<String> lowRateCapabilities,
        LearningRateTrend trend
    ) {
        public record LearningRateTrend(
            float previousGlobalRate,
            float currentGlobalRate,
            TrendDirection direction
        ) {
            public enum TrendDirection { INCREASING, DECREASING, STABLE }
        }
    }

    /**
     * 遗忘分析
     */
    public record ForgettingAnalysis(
        int totalTrackedMemories,
        long recentForgettingEvents,
        float averageStrength,
        List<MemoryStrength> weakestMemories,
        ForgettingConfig config
    ) {}

    /**
     * 可视化引擎接口
     */
    public interface EvolutionVisualizer {
        EvolutionHistoryVisualization generateVisualization(Engine engine);
        TimelineData generateTimeline(Engine engine);
        InsightAnalysis analyzeInsights(LearningLoop loop);
        PrincipleAnalysis analyzePrinciples(LearningLoop loop);
        BehaviorAnalysis analyzeBehaviors(LearningLoop loop);
        ModificationAnalysis analyzeModifications(SelfModifier modifier);
        LearningRateAnalysis analyzeLearningRates(LearningLoop loop);
        ForgettingAnalysis analyzeForgetting(ForgettingMechanism mechanism);
    }

    /**
     * 默认可视化实现
     */
    public static class DefaultEvolutionVisualizer implements EvolutionVisualizer {

        @Override
        public EvolutionHistoryVisualization generateVisualization(Engine engine) {
            return new EvolutionHistoryVisualization(
                engine.evolutionLevel,
                engine.evolutionCount,
                generateTimeline(engine),
                analyzeInsights(engine.learningLoop),
                analyzePrinciples(engine.learningLoop),
                analyzeBehaviors(engine.learningLoop),
                analyzeModifications(engine.selfModifier),
                analyzeLearningRates(engine.learningLoop),
                analyzeForgetting(engine.forgettingMechanism)
            );
        }

        @Override
        public TimelineData generateTimeline(Engine engine) {
            List<TimelineEvent> events = new ArrayList<>();

            // 添加洞察事件
            for (Insight insight : engine.learningLoop.insights) {
                events.add(new TimelineEvent(
                    insight.timestamp(),
                    "INSIGHT",
                    insight.type().name(),
                    insight.hypothesis(),
                    insight.confidence()
                ));
            }

            // 添加行为改变事件
            for (BehaviorChange change : engine.learningLoop.behaviorChanges) {
                events.add(new TimelineEvent(
                    change.timestamp(),
                    "BEHAVIOR_CHANGE",
                    change.afterBehavior().substring(0, Math.min(30, change.afterBehavior().length())),
                    change.principle().statement(),
                    change.success() != null && change.success() ? 0.8f : 0.4f
                ));
            }

            // 添加修改事件
            for (SelfModifier.Modification mod : engine.selfModifier.getHistory()) {
                events.add(new TimelineEvent(
                    mod.timestamp(),
                    "MODIFICATION",
                    mod.type().name() + ": " + mod.target(),
                    mod.before() + " -> " + mod.after(),
                    mod.success() ? 0.9f : 0.3f
                ));
            }

            // 排序
            events.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

            Instant startTime = events.isEmpty() ? Instant.now() : events.get(0).timestamp();
            Instant endTime = events.isEmpty() ? Instant.now() : events.get(events.size() - 1).timestamp();
            Duration duration = Duration.between(startTime, endTime);

            return new TimelineData(events, startTime, endTime, duration);
        }

        @Override
        public InsightAnalysis analyzeInsights(LearningLoop loop) {
            List<Insight> insights = loop.insights;
            int total = insights.size();

            // 类型分布
            int[] typeCounts = new int[6];
            for (Insight insight : insights) {
                typeCounts[insight.type().ordinal()]++;
            }
            var typeDist = new InsightAnalysis.InsightTypeDistribution(
                typeCounts[0], typeCounts[1], typeCounts[2], typeCounts[3], typeCounts[4], typeCounts[5]
            );

            // 置信度趋势
            float avgConf = insights.isEmpty() ? 0f :
                (float) insights.stream().mapToDouble(Insight::confidence).average().orElse(0);
            float minConf = insights.isEmpty() ? 0f :
                (float) insights.stream().mapToDouble(Insight::confidence).min().orElse(0);
            float maxConf = insights.isEmpty() ? 0f :
                (float) insights.stream().mapToDouble(Insight::confidence).max().orElse(0);

            // 最近洞察
            List<Insight> recent = insights.size() > 5 ?
                insights.subList(insights.size() - 5, insights.size()) : insights;

            InsightAnalysis.TrendDirection trendDir = InsightAnalysis.TrendDirection.STABLE;
            if (recent.size() >= 2) {
                float older = recent.get(0).confidence();
                float newer = recent.get(recent.size() - 1).confidence();
                if (newer > older * 1.1f) trendDir = InsightAnalysis.TrendDirection.IMPROVING;
                else if (newer < older * 0.9f) trendDir = InsightAnalysis.TrendDirection.DECLINING;
            }

            return new InsightAnalysis(
                new ArrayList<>(insights),
                total,
                typeDist,
                new InsightAnalysis.ConfidenceTrend(avgConf, minConf, maxConf, trendDir),
                recent
            );
        }

        @Override
        public PrincipleAnalysis analyzePrinciples(LearningLoop loop) {
            List<Principle> principles = loop.principles;
            int total = principles.size();

            float avgConf = principles.isEmpty() ? 0f :
                (float) principles.stream().mapToDouble(Principle::confidence).average().orElse(0);

            List<Principle> mostApplied = principles.stream()
                .sorted((a, b) -> Integer.compare(b.timesApplied(), a.timesApplied()))
                .limit(5)
                .toList();

            List<Principle> highestConf = principles.stream()
                .sorted((a, b) -> Float.compare(b.confidence(), a.confidence()))
                .limit(5)
                .toList();

            return new PrincipleAnalysis(
                new ArrayList<>(principles),
                total,
                avgConf,
                mostApplied,
                highestConf
            );
        }

        @Override
        public BehaviorAnalysis analyzeBehaviors(LearningLoop loop) {
            List<BehaviorChange> changes = loop.behaviorChanges;
            int total = changes.size();
            int success = (int) changes.stream().filter(c -> Boolean.TRUE.equals(c.success())).count();
            int failed = (int) changes.stream().filter(c -> Boolean.FALSE.equals(c.success())).count();

            List<String> patterns = changes.stream()
                .map(BehaviorChange::afterBehavior)
                .distinct()
                .limit(10)
                .toList();

            return new BehaviorAnalysis(
                new ArrayList<>(changes),
                total,
                success,
                failed,
                patterns
            );
        }

        @Override
        public ModificationAnalysis analyzeModifications(SelfModifier modifier) {
            List<SelfModifier.Modification> mods = modifier.getHistory();
            int total = mods.size();

            int[] typeCounts = new int[6];
            for (SelfModifier.Modification mod : mods) {
                typeCounts[mod.type().ordinal()]++;
            }
            var typeDist = new ModificationAnalysis.ModificationTypeDistribution(
                typeCounts[0], typeCounts[1], typeCounts[2], typeCounts[3], typeCounts[4], typeCounts[5]
            );

            List<SelfModifier.Modification> recent = mods.size() > 10 ?
                mods.subList(mods.size() - 10, mods.size()) : mods;

            return new ModificationAnalysis(
                new ArrayList<>(mods),
                total,
                typeDist,
                recent
            );
        }

        @Override
        public LearningRateAnalysis analyzeLearningRates(LearningLoop loop) {
            float globalRate = loop.getGlobalLearningRate();
            List<LearningRateConfig> configs = new ArrayList<>(loop.getAllLearningRateConfigs());

            List<String> high = configs.stream()
                .filter(c -> c.currentRate() > c.baseRate() * 1.3f)
                .map(c -> c.capabilityName() + " (" + String.format("%.2f", c.currentRate()) + ")")
                .toList();

            List<String> low = configs.stream()
                .filter(c -> c.currentRate() < c.baseRate() * 0.7f)
                .map(c -> c.capabilityName() + " (" + String.format("%.2f", c.currentRate()) + ")")
                .toList();

            return new LearningRateAnalysis(
                globalRate,
                configs,
                high,
                low,
                new LearningRateAnalysis.LearningRateTrend(globalRate, globalRate, LearningRateAnalysis.LearningRateTrend.TrendDirection.STABLE)
            );
        }

        @Override
        public ForgettingAnalysis analyzeForgetting(ForgettingMechanism mechanism) {
            ForgettingStats stats = mechanism.getStats();
            List<MemoryStrength> strengths = new ArrayList<>(mechanism.getAllMemoryStrengths());

            float avgStrength = strengths.isEmpty() ? 0f :
                (float) strengths.stream().mapToDouble(MemoryStrength::currentStrength).average().orElse(0);

            List<MemoryStrength> weakest = strengths.stream()
                .sorted((a, b) -> Float.compare(a.currentStrength(), b.currentStrength()))
                .limit(10)
                .toList();

            return new ForgettingAnalysis(
                stats.totalMemories(),
                stats.recentForgettingEvents(),
                avgStrength,
                weakest,
                stats.config()
            );
        }
    }

    // ==================== 快速回滚机制 ====================

    /**
     * 系统快照
     */
    public record SystemSnapshot(
        String id,
        Instant timestamp,
        String label,
        SelfModel.Self selfSnapshot,
        List<Observation> observations,
        List<Insight> insights,
        List<Principle> principles,
        List<BehaviorChange> behaviorChanges,
        int evolutionLevel,
        int evolutionCount
    ) {}

    /**
     * 回滚点
     */
    public record RollbackPoint(
        String snapshotId,
        Instant timestamp,
        String reason,
        int selfEvolutionLevel,
        int modificationsSince
    ) {}

    /**
     * 回滚结果
     */
    public record RollbackResult(
        boolean success,
        String message,
        String snapshotId,
        Instant timestamp,
        int selfEvolutionLevel,
        int modificationsRolledBack
    ) {}

    /**
     * 快照管理器
     */
    public static class SnapshotManager {
        private final List<SystemSnapshot> snapshots = new ArrayList<>();
        private final int maxSnapshots;
        private final java.util.Map<String, SystemSnapshot> snapshotIndex = new java.util.concurrent.ConcurrentHashMap<>();

        public SnapshotManager() {
            this(10); // 默认保留10个快照
        }

        public SnapshotManager(int maxSnapshots) {
            this.maxSnapshots = maxSnapshots > 0 ? maxSnapshots : 10;
        }

        /**
         * 创建快照
         */
        public SystemSnapshot createSnapshot(
            String label,
            SelfModel.Self self,
            LearningLoop learningLoop,
            int evolutionLevel,
            int evolutionCount
        ) {
            String id = "snapshot-" + UUID.randomUUID().toString();
            Instant now = Instant.now();

            SystemSnapshot snapshot = new SystemSnapshot(
                id,
                now,
                label,
                self,
                new ArrayList<>(learningLoop.observations),
                new ArrayList<>(learningLoop.insights),
                new ArrayList<>(learningLoop.principles),
                new ArrayList<>(learningLoop.behaviorChanges),
                evolutionLevel,
                evolutionCount
            );

            snapshots.add(snapshot);
            snapshotIndex.put(id, snapshot);

            // 清理旧快照
            pruneSnapshots();

            return snapshot;
        }

        /**
         * 获取快照
         */
        public SystemSnapshot getSnapshot(String id) {
            return snapshotIndex.get(id);
        }

        /**
         * 获取所有快照
         */
        public List<SystemSnapshot> getAllSnapshots() {
            return new ArrayList<>(snapshots);
        }

        /**
         * 获取最近的快照
         */
        public List<SystemSnapshot> getRecentSnapshots(int count) {
            if (snapshots.isEmpty()) return List.of();
            int size = Math.min(count, snapshots.size());
            return snapshots.subList(snapshots.size() - size, snapshots.size());
        }

        /**
         * 删除快照
         */
        public boolean deleteSnapshot(String id) {
            SystemSnapshot snapshot = snapshotIndex.remove(id);
            if (snapshot != null) {
                snapshots.remove(snapshot);
                return true;
            }
            return false;
        }

        /**
         * 清理旧快照
         */
        private void pruneSnapshots() {
            while (snapshots.size() > maxSnapshots) {
                SystemSnapshot oldest = snapshots.remove(0);
                snapshotIndex.remove(oldest.id());
            }
        }

        /**
         * 获取快照统计
         */
        public SnapshotStats getStats() {
            return new SnapshotStats(
                snapshots.size(),
                maxSnapshots,
                snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1).timestamp()
            );
        }
    }

    public record SnapshotStats(
        int currentCount,
        int maxCount,
        Instant lastSnapshotTime
    ) {}

    /**
     * 工厂方法
     */
    public static class Factory {
        public static Engine create() {
            return new Engine();
        }

        public static Engine createWithDefaults() {
            Engine engine = new Engine();
            return engine;
        }
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> result = new ArrayList<>(a);
        result.addAll(b);
        return List.copyOf(result);
    }

    // ==================== S19-1: 贝叶斯信念更新 ====================

    /**
     * 贝叶斯信念更新器
     * 实现贝叶斯推断来更新信念：P(H|E) = P(E|H) * P(H) / P(E)
     */
    public static class BayesianBeliefUpdater {

        /**
         * 更新信念的贝叶斯推断
         * P(H|E) = P(E|H) * P(H) / P(E)
         *
         * @param prior 先验概率 P(H)
         * @param likelihood 似然 P(E|H)
         * @param evidence 证据概率 P(E)
         * @return 后验概率 P(H|E)
         */
        public float updateBelief(float prior, float likelihood, float evidence) {
            if (evidence == 0f) return prior;
            float posterior = (likelihood * prior) / evidence;
            return clampProbability(posterior);
        }

        /**
         * 使用成功率和失败率更新信念
         *
         * @param prior 先验概率
         * @param successCount 成功次数
         * @param failureCount 失败次数
         * @return 更新后的信念
         */
        public float updateBeliefFromOutcome(float prior, int successCount, int failureCount) {
            int total = successCount + failureCount;
            if (total == 0) return prior;

            // 似然：给定先验，成功率是多少
            float likelihood = total > 0 ? (float) successCount / total : 0.5f;
            // 证据：综合先验和观察
            float evidence = (prior + likelihood) / 2f;

            return updateBelief(prior, likelihood, evidence);
        }

        /**
         * 计算两个信念的融合
         * 使用贝叶斯条件化来合并不同来源的证据
         */
        public float fuseBeliefs(float belief1, float belief2, float weight1) {
            float weight2 = 1f - weight1;
            // 融合两个信念的权重
            return clampProbability(belief1 * weight1 + belief2 * weight2);
        }

        /**
         * 根据新证据调整信念
         *
         * @param currentBelief 当前信念
         * @param newEvidence 新证据
         * @param evidenceStrength 证据强度 (0-1)
         * @return 更新后的信念
         */
        public float adjustBelief(float currentBelief, float newEvidence, float evidenceStrength) {
            // 强证据移动信念更多
            float movement = (newEvidence - currentBelief) * evidenceStrength;
            return clampProbability(currentBelief + movement);
        }

        private float clampProbability(float value) {
            return Math.max(0.01f, Math.min(0.99f, value));
        }
    }

    /**
     * 信念状态追踪
     */
    public record BeliefState(
        String beliefId,
        float currentBelief,
        float priorBelief,
        int observationCount,
        Instant lastUpdated,
        List<BeliefEvidence> evidenceHistory
    ) {
        public BeliefState {
            evidenceHistory = evidenceHistory != null ? List.copyOf(evidenceHistory) : List.of();
        }
    }

    public record BeliefEvidence(
        Instant timestamp,
        float evidence,
        float likelihood,
        float posterior,
        String source
    ) {}

    // ==================== S19-2: 自动化原则提取 ====================

    /**
     * 原则提取器
     * 从交互历史中自动提取行动原则
     */
    public static class PrincipleExtractor {

        private final BayesianBeliefUpdater bayesianUpdater = new BayesianBeliefUpdater();
        private static final int MIN_INTERACTIONS_FOR_EXTRACTION = 3;
        private static final float MIN_CONFIDENCE_THRESHOLD = 0.4f;

        /**
         * 从交互历史中提取原则
         *
         * @param history 交互历史
         * @return 提取的原则列表
         */
        public List<Principle> extractPrinciples(List<OwnerModel.Interaction> history) {
            if (history == null || history.size() < MIN_INTERACTIONS_FOR_EXTRACTION) {
                return List.of();
            }

            List<Principle> extractedPrinciples = new ArrayList<>();

            // 分析交互类型模式
            List<Principle> typePrinciples = extractTypeBasedPrinciples(history);
            extractedPrinciples.addAll(typePrinciples);

            // 分析情感模式
            List<Principle> sentimentPrinciples = extractSentimentBasedPrinciples(history);
            extractedPrinciples.addAll(sentimentPrinciples);

            // 分析主题模式
            List<Principle> topicPrinciples = extractTopicBasedPrinciples(history);
            extractedPrinciples.addAll(topicPrinciples);

            // 分析结果模式
            List<Principle> outcomePrinciples = extractOutcomeBasedPrinciples(history);
            extractedPrinciples.addAll(outcomePrinciples);

            return extractedPrinciples;
        }

        /**
         * 基于交互类型提取原则
         */
        private List<Principle> extractTypeBasedPrinciples(List<OwnerModel.Interaction> history) {
            List<Principle> principles = new ArrayList<>();

            // 按类型分组统计
            var typeGroups = history.stream()
                .collect(java.util.stream.Collectors.groupingBy(OwnerModel.Interaction::type));

            for (var entry : typeGroups.entrySet()) {
                OwnerModel.InteractionType type = entry.getKey();
                List<OwnerModel.Interaction> interactions = entry.getValue();

                if (interactions.size() >= MIN_INTERACTIONS_FOR_EXTRACTION) {
                    // 计算平均情感
                    float avgSentiment = (float) interactions.stream()
                        .mapToDouble(OwnerModel.Interaction::sentiment)
                        .average().orElse(0.5);

                    // 判断成功模式
                    long positiveOutcomes = interactions.stream()
                        .filter(i -> i.outcome() != null && i.outcome().contains("成功"))
                        .count();
                    float successRate = (float) positiveOutcomes / interactions.size();

                    if (successRate > 0.6f || avgSentiment > 0.3f) {
                        String statement = generateTypePrinciple(type, avgSentiment, successRate);
                        float confidence = calculatePrincipleConfidence(interactions.size(), successRate, avgSentiment);

                        if (confidence >= MIN_CONFIDENCE_THRESHOLD) {
                            principles.add(new Principle(
                                Instant.now(),
                                statement,
                                List.of("基于 " + interactions.size() + " 次 " + type.name() + " 交互分析"),
                                "交互类型:" + type.name(),
                                confidence
                            ));
                        }
                    }
                }
            }

            return principles;
        }

        /**
         * 基于情感提取原则
         */
        private List<Principle> extractSentimentBasedPrinciples(List<OwnerModel.Interaction> history) {
            List<Principle> principles = new ArrayList<>();

            // 高情感交互
            List<OwnerModel.Interaction> highSentiment = history.stream()
                .filter(i -> i.sentiment() > 0.5f)
                .toList();

            if (highSentiment.size() >= MIN_INTERACTIONS_FOR_EXTRACTION) {
                String commonTopic = findCommonTopic(highSentiment);
                if (commonTopic != null) {
                    principles.add(new Principle(
                        Instant.now(),
                        "当主人情绪积极时，适合讨论 " + commonTopic + " 相关话题",
                        List.of("基于 " + highSentiment.size() + " 次高情感交互分析"),
                        "情境:主人情绪积极",
                        0.6f
                    ));
                }
            }

            // 低情感交互
            List<OwnerModel.Interaction> lowSentiment = history.stream()
                .filter(i -> i.sentiment() < -0.3f)
                .toList();

            if (lowSentiment.size() >= 2) {
                principles.add(new Principle(
                    Instant.now(),
                    "主人情绪低落时，应简洁回复并提供支持",
                    List.of("基于 " + lowSentiment.size() + " 次低情感交互分析"),
                    "情境:主人情绪低落",
                    0.5f
                ));
            }

            return principles;
        }

        /**
         * 基于主题提取原则
         */
        private List<Principle> extractTopicBasedPrinciples(List<OwnerModel.Interaction> history) {
            List<Principle> principles = new ArrayList<>();

            // 按主题分组
            var topicGroups = history.stream()
                .filter(i -> i.topic() != null)
                .collect(java.util.stream.Collectors.groupingBy(OwnerModel.Interaction::topic));

            for (var entry : topicGroups.entrySet()) {
                String topic = entry.getKey();
                List<OwnerModel.Interaction> interactions = entry.getValue();

                if (interactions.size() >= MIN_INTERACTIONS_FOR_EXTRACTION) {
                    float avgSentiment = (float) interactions.stream()
                        .mapToDouble(OwnerModel.Interaction::sentiment)
                        .average().orElse(0.5);

                    long positiveOutcomes = interactions.stream()
                        .filter(i -> i.outcome() != null && i.outcome().contains("成功"))
                        .count();
                    float successRate = (float) positiveOutcomes / interactions.size();

                    String statement;
                    if (successRate > 0.7f && avgSentiment > 0.3f) {
                        statement = "主人对 " + topic + " 相关话题反应积极，可以主动提供相关信息";
                    } else if (successRate < 0.3f || avgSentiment < -0.2f) {
                        statement = "主人对 " + topic + " 相关话题反应消极，应谨慎处理或减少提及";
                    } else {
                        continue;
                    }

                    principles.add(new Principle(
                        Instant.now(),
                        statement,
                        List.of("基于 " + interactions.size() + " 次 " + topic + " 交互分析"),
                        "主题:" + topic,
                        calculatePrincipleConfidence(interactions.size(), successRate, avgSentiment)
                    ));
                }
            }

            return principles;
        }

        /**
         * 基于结果提取原则
         */
        private List<Principle> extractOutcomeBasedPrinciples(List<OwnerModel.Interaction> history) {
            List<Principle> principles = new ArrayList<>();

            List<OwnerModel.Interaction> withOutcome = history.stream()
                .filter(i -> i.outcome() != null)
                .toList();

            if (withOutcome.size() < MIN_INTERACTIONS_FOR_EXTRACTION) {
                return principles;
            }

            // 成功模式
            List<OwnerModel.Interaction> successful = withOutcome.stream()
                .filter(i -> i.outcome().contains("成功"))
                .toList();

            if (successful.size() >= MIN_INTERACTIONS_FOR_EXTRACTION) {
                String commonReaction = findMostCommonReaction(successful);
                principles.add(new Principle(
                    Instant.now(),
                    "当交互成功时，表现出 " + commonReaction,
                    List.of("基于 " + successful.size() + " 次成功交互分析"),
                    "结果:成功",
                    0.7f
                ));
            }

            // 失败模式
            List<OwnerModel.Interaction> failed = withOutcome.stream()
                .filter(i -> i.outcome().contains("失败") || i.outcome().contains("拒绝"))
                .toList();

            if (failed.size() >= 2) {
                principles.add(new Principle(
                    Instant.now(),
                    "当交互失败时，应简洁承认并避免重复同样方式",
                    List.of("基于 " + failed.size() + " 次失败交互分析"),
                    "结果:失败",
                    0.6f
                ));
            }

            return principles;
        }

        private String generateTypePrinciple(OwnerModel.InteractionType type, float avgSentiment, float successRate) {
            return switch (type) {
                case REQUEST -> successRate > 0.6f ?
                    "主人请求时应及时响应并准确执行" :
                    "主人请求时需要先确认关键细节";
                case FEEDBACK -> avgSentiment > 0.3f ?
                    "主人反馈积极时继续保持当前方式" :
                    "主人反馈消极时应调整沟通策略";
                case QUESTION -> "主人提问时应直接给出明确答案";
                case COMPLAINT -> "主人抱怨时应该先安抚情绪再解决问题";
                case PRAISE -> "主人表扬时可以适当回应并继续保持";
                case CASUAL -> "主人闲聊时可以放松一些，保持友好氛围";
                case PROACTIVE_REPLY -> "主动消息得到回复说明时机合适";
                case PROACTIVE_IGNORE -> "主动消息被忽略说明时机不对或内容不相关";
                default -> "根据交互类型调整响应方式";
            };
        }

        private float calculatePrincipleConfidence(int interactionCount, float successRate, float avgSentiment) {
            // 基于样本量和成功率计算置信度
            float sampleFactor = Math.min(interactionCount * 0.1f, 0.4f);
            float successFactor = successRate * 0.4f;
            float sentimentFactor = (avgSentiment + 1f) * 0.2f; // 归一化到 0-0.2
            return Math.min(0.95f, 0.3f + sampleFactor + successFactor + sentimentFactor);
        }

        private String findCommonTopic(List<OwnerModel.Interaction> interactions) {
            var topicCounts = interactions.stream()
                .filter(i -> i.topic() != null)
                .collect(java.util.stream.Collectors.groupingBy(OwnerModel.Interaction::topic, java.util.stream.Collectors.counting()));

            return topicCounts.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);
        }

        private String findMostCommonReaction(List<OwnerModel.Interaction> interactions) {
            var reactionCounts = interactions.stream()
                .filter(i -> i.digitalBeingReaction() != null)
                .collect(java.util.stream.Collectors.groupingBy(OwnerModel.Interaction::digitalBeingReaction, java.util.stream.Collectors.counting()));

            return reactionCounts.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("满意");
        }
    }

    // ==================== S19-3: 自适应学习策略调整 ====================

    /**
     * 自适应学习速率计算器
     * 根据成功率和样本数动态调整学习速率
     */
    public static class AdaptiveLearningRate {

        private static final float MIN_RATE = 0.1f;
        private static final float MAX_RATE = 3.0f;
        private static final float DEFAULT_RATE = 1.0f;

        /**
         * 计算自适应学习速率
         *
         * @param successRate 当前成功率 (0-1)
         * @param sampleCount 样本数量
         * @return 推荐的学习速率
         */
        public float computeRate(float successRate, int sampleCount) {
            if (sampleCount < 3) {
                // 样本不足时，使用较高的学习率快速探索
                return Math.min(DEFAULT_RATE * 1.5f, MAX_RATE);
            }

            // 基础调整因子
            float baseAdjustment;

            if (successRate >= 0.8f) {
                // 高成功率 - 降低学习率（已经学得很好）
                baseAdjustment = 0.7f;
            } else if (successRate >= 0.6f) {
                // 中等成功率 - 保持稳定
                baseAdjustment = 1.0f;
            } else if (successRate >= 0.4f) {
                // 较低成功率 - 稍微提高学习率
                baseAdjustment = 1.3f;
            } else {
                // 低成功率 - 大幅提高学习率（需要快速学习新方式）
                baseAdjustment = 1.8f;
            }

            // 样本数调整：更多样本意味着更可信，步伐可以更大
            float sampleFactor = Math.min(1.0f + (sampleCount / 100f), 1.5f);

            // 组合调整
            float adjustedRate = DEFAULT_RATE * baseAdjustment * sampleFactor;

            // 如果样本很多但成功率很低，可能需要更激进的调整
            if (sampleCount > 50 && successRate < 0.3f) {
                adjustedRate *= 1.3f;
            }

            return clampRate(adjustedRate);
        }

        /**
         * 计算能力的具体学习速率
         */
        public float computeCapabilityRate(String capabilityName, float historicalSuccessRate, int totalAttempts, int recentFailures) {
            // 基础成功率调整
            float baseRate = computeRate(historicalSuccessRate, totalAttempts);

            // 近期失败惩罚/奖励
            if (recentFailures > 3) {
                // 连续失败后增加学习率
                baseRate *= 1.2f;
            } else if (recentFailures == 0 && totalAttempts > 10) {
                // 长期成功则减少学习率
                baseRate *= 0.9f;
            }

            return clampRate(baseRate);
        }

        /**
         * 计算全局学习速率
         */
        public float computeGlobalRate(float overallSuccessRate, int totalSamples, List<LearningRateConfig> capabilityConfigs) {
            // 计算所有能力的平均学习速率
            float avgCapabilityRate = capabilityConfigs.stream()
                .mapToDouble(LearningRateConfig::currentRate)
                .average()
                .orElse(DEFAULT_RATE);

            // 全局成功率调整
            float globalRate = computeRate(overallSuccessRate, totalSamples);

            // 融合能力和全局调整
            float fused = (globalRate * 0.6f) + (avgCapabilityRate * 0.4f);

            return clampRate(fused);
        }

        /**
         * 获取学习速率建议
         */
        public LearningRateRecommendation getRecommendation(float currentRate, float successRate, int sampleCount) {
            float recommended = computeRate(successRate, sampleCount);
            float change = recommended - currentRate;
            String advice;

            if (Math.abs(change) < 0.1f) {
                advice = "当前学习速率合适，保持不变";
            } else if (change > 0) {
                advice = String.format("建议提高学习速率 %.2f -> %.2f (成功率 %.1f%% 需要更积极学习)",
                    currentRate, recommended, successRate * 100);
            } else {
                advice = String.format("建议降低学习速率 %.2f -> %.2f (已稳定，可减少调整)",
                    currentRate, recommended);
            }

            return new LearningRateRecommendation(
                currentRate,
                recommended,
                change,
                advice,
                successRate,
                sampleCount
            );
        }

        private float clampRate(float rate) {
            return Math.max(MIN_RATE, Math.min(MAX_RATE, rate));
        }
    }

    public record LearningRateRecommendation(
        float currentRate,
        float recommendedRate,
        float change,
        String advice,
        float successRate,
        int sampleCount
    ) {}

    // ==================== S19-4: 进化效果量化评估 ====================

    /**
     * 进化效果量化评估指标
     */
    public record EvolutionMetrics(
        float improvementRate,        // 改进率：相比上次进化的提升
        float stabilityScore,         // 稳定性得分：进化的稳定性
        int skillsGained,             // 习得技能数
        int skillsRefined,            // 精炼技能数
        float beliefAccuracy,         // 信念准确度
        float principleEffectiveness,  // 原则有效性
        int successfulAdaptations,    // 成功适应次数
        int failedAdaptations,        // 失败适应次数
        float overallScore,           // 综合评分
        Instant calculatedAt
    ) {
        public EvolutionMetrics {
            if (calculatedAt == null) calculatedAt = Instant.now();
        }
    }

    /**
     * 进化效果评估器
     */
    public static class EvolutionEvaluator {

        private final BayesianBeliefUpdater beliefUpdater = new BayesianBeliefUpdater();

        /**
         * 评估进化效果
         *
         * @param currentMetrics 当前指标
         * @param previousMetrics 上次指标
         * @param evolutionCount 进化次数
         * @return 量化评估结果
         */
        public EvolutionMetrics evaluate(EvolutionMetrics currentMetrics, EvolutionMetrics previousMetrics, int evolutionCount) {
            float improvementRate = calculateImprovementRate(currentMetrics, previousMetrics);
            float stabilityScore = calculateStabilityScore(currentMetrics, previousMetrics);
            float beliefAccuracy = calculateBeliefAccuracy(currentMetrics);
            float principleEffectiveness = calculatePrincipleEffectiveness(currentMetrics);

            int successfulAdaptations = currentMetrics.successfulAdaptations();
            int failedAdaptations = currentMetrics.failedAdaptations();

            // 综合评分
            float overall = calculateOverallScore(
                improvementRate,
                stabilityScore,
                beliefAccuracy,
                principleEffectiveness,
                successfulAdaptations,
                failedAdaptations
            );

            return new EvolutionMetrics(
                improvementRate,
                stabilityScore,
                currentMetrics.skillsGained(),
                currentMetrics.skillsRefined(),
                beliefAccuracy,
                principleEffectiveness,
                successfulAdaptations,
                failedAdaptations,
                overall,
                Instant.now()
            );
        }

        /**
         * 从Engine状态生成评估指标
         */
        public EvolutionMetrics evaluateFromEngine(Engine engine, EvolutionMetrics previousMetrics) {
            LearningStats stats = engine.learningLoop.getStats();
            OutcomeStats outcomeStats = engine.feedbackCollector.getOutcomeStats();

            // 计算技能变化
            int skillsGained = countNewCapabilities(engine, previousMetrics);
            int skillsRefined = countRefinedCapabilities(engine);

            // 计算信念准确度（使用贝叶斯更新）
            float beliefAccuracy = calculateCurrentBeliefAccuracy(stats, outcomeStats);

            // 计算原则有效性
            float principleEffectiveness = calculateCurrentPrincipleEffectiveness(engine.learningLoop);

            // 适应次数统计
            int successfulAdaptations = (int) engine.learningLoop.behaviorChanges.stream()
                .filter(bc -> Boolean.TRUE.equals(bc.success()))
                .count();
            int failedAdaptations = (int) engine.learningLoop.behaviorChanges.stream()
                .filter(bc -> Boolean.FALSE.equals(bc.success()))
                .count();

            EvolutionMetrics current = new EvolutionMetrics(
                0f, // 稍后计算
                0f, // 稍后计算
                skillsGained,
                skillsRefined,
                beliefAccuracy,
                principleEffectiveness,
                successfulAdaptations,
                failedAdaptations,
                0f, // 稍后计算
                Instant.now()
            );

            return evaluate(current, previousMetrics, engine.evolutionCount);
        }

        /**
         * 创建初始指标（用于首次评估）
         */
        public EvolutionMetrics createBaseline() {
            return new EvolutionMetrics(
                0f,
                1f, // 初始稳定性为1
                0,
                0,
                0.5f, // 初始信念准确度0.5
                0f,
                0,
                0,
                0.5f,
                Instant.now()
            );
        }

        private float calculateImprovementRate(EvolutionMetrics current, EvolutionMetrics previous) {
            if (previous == null) return 0f;

            // 基于多个维度计算改进率
            float skillImprovement = previous.skillsGained() > 0 ?
                (float) (current.skillsGained() - previous.skillsGained()) / previous.skillsGained() : 0f;

            float beliefImprovement = current.beliefAccuracy() - previous.beliefAccuracy();

            float adaptationImprovement = calculateAdaptationImprovement(current, previous);

            // 加权平均
            return clampScore((skillImprovement * 0.3f) + (beliefImprovement * 0.3f) + (adaptationImprovement * 0.4f));
        }

        private float calculateStabilityScore(EvolutionMetrics current, EvolutionMetrics previous) {
            if (previous == null) return 1f;

            // 稳定性：成功适应 vs 总适应
            int totalAdaptations = current.successfulAdaptations() + current.failedAdaptations() +
                previous.successfulAdaptations() + previous.failedAdaptations();

            if (totalAdaptations == 0) return 1f;

            int successful = current.successfulAdaptations() + previous.successfulAdaptations();
            return clampScore((float) successful / totalAdaptations);
        }

        private float calculateBeliefAccuracy(EvolutionMetrics metrics) {
            return clampScore(metrics.beliefAccuracy());
        }

        private float calculatePrincipleEffectiveness(EvolutionMetrics metrics) {
            int total = metrics.successfulAdaptations() + metrics.failedAdaptations();
            if (total == 0) return 0.5f;

            return clampScore((float) metrics.successfulAdaptations() / total);
        }

        private float calculateOverallScore(
            float improvementRate,
            float stabilityScore,
            float beliefAccuracy,
            float principleEffectiveness,
            int successfulAdaptations,
            int failedAdaptations
        ) {
            // 综合评分公式
            float base = 0.25f; // 基础分

            // 稳定性贡献
            float stability = stabilityScore * 0.2f;

            // 信念准确度贡献
            float belief = beliefAccuracy * 0.15f;

            // 原则有效性贡献
            float principle = principleEffectiveness * 0.15f;

            // 改进率贡献（可以是负的）
            float improvement = (1f + improvementRate) * 0.15f;

            // 适应成功率贡献
            int totalAdaptations = successfulAdaptations + failedAdaptations;
            float adaptationScore = totalAdaptations > 0 ?
                ((float) successfulAdaptations / totalAdaptations) * 0.1f : 0f;

            return clampScore(base + stability + belief + principle + improvement + adaptationScore);
        }

        private float calculateAdaptationImprovement(EvolutionMetrics current, EvolutionMetrics previous) {
            int currentTotal = current.successfulAdaptations() + current.failedAdaptations();
            int previousTotal = previous.successfulAdaptations() + previous.failedAdaptations();

            if (currentTotal == 0 || previousTotal == 0) return 0f;

            float currentRate = (float) current.successfulAdaptations() / currentTotal;
            float previousRate = (float) previous.successfulAdaptations() / previousTotal;

            return currentRate - previousRate;
        }

        private int countNewCapabilities(Engine engine, EvolutionMetrics previous) {
            if (previous == null) return engine.learningLoop.capabilityPerformance.size();
            return Math.max(0, engine.learningLoop.capabilityPerformance.size() - previous.skillsGained());
        }

        private int countRefinedCapabilities(Engine engine) {
            // 计算被提升的能力数量
            return (int) engine.learningLoop.capabilityPerformance.values().stream()
                .filter(cp -> cp.successRate() >= 0.7f)
                .count();
        }

        private float calculateCurrentBeliefAccuracy(LearningStats stats, OutcomeStats outcomeStats) {
            // 使用贝叶斯方法综合多个信号
            float prior = 0.5f;
            float likelihood = outcomeStats.successRate();
            float evidence = (prior + likelihood) / 2f;

            return beliefUpdater.updateBelief(prior, likelihood, evidence);
        }

        private float calculateCurrentPrincipleEffectiveness(LearningLoop loop) {
            if (loop.principles.isEmpty()) return 0.5f;

            // 基于应用成功率计算原则有效性
            float totalEffectiveness = 0f;
            for (Principle p : loop.principles) {
                if (p.timesApplied() > 0) {
                    // 简化：假设应用次数多且置信度高 = 有效
                    totalEffectiveness += p.confidence() * Math.min(1f, p.timesApplied() / 5f);
                }
            }

            return loop.principles.isEmpty() ? 0.5f : clampScore(totalEffectiveness / loop.principles.size());
        }

        private float clampScore(float score) {
            return Math.max(0f, Math.min(1f, score));
        }
    }

    /**
     * 简化的进化进度追踪器
     */
    public static class EvolutionProgressTracker {

        private EvolutionMetrics lastMetrics = null;
        private final EvolutionEvaluator evaluator = new EvolutionEvaluator();

        /**
         * 追踪当前进度
         */
        public EvolutionMetrics track(Engine engine) {
            EvolutionMetrics current = evaluator.evaluateFromEngine(engine, lastMetrics);
            lastMetrics = current;
            return current;
        }

        /**
         * 获取上次指标
         */
        public EvolutionMetrics getLastMetrics() {
            return lastMetrics;
        }

        /**
         * 获取进度报告
         */
        public EvolutionProgressReport getProgressReport(Engine engine) {
            EvolutionMetrics current = track(engine);

            String status;
            if (current.overallScore() >= 0.8f) {
                status = "EXCELLENT";
            } else if (current.overallScore() >= 0.6f) {
                status = "GOOD";
            } else if (current.overallScore() >= 0.4f) {
                status = "FAIR";
            } else {
                status = "NEEDS_IMPROVEMENT";
            }

            return new EvolutionProgressReport(
                engine.evolutionLevel,
                engine.evolutionCount,
                current,
                lastMetrics,
                status,
                generateRecommendations(current)
            );
        }

        private List<String> generateRecommendations(EvolutionMetrics metrics) {
            List<String> recommendations = new ArrayList<>();

            if (metrics.stabilityScore() < 0.5f) {
                recommendations.add("稳定性较低，建议减少大幅改变，专注于已有能力的提升");
            }

            if (metrics.beliefAccuracy() < 0.5f) {
                recommendations.add("信念准确度不足，建议收集更多反馈来校准判断");
            }

            if (metrics.principleEffectiveness() < 0.4f) {
                recommendations.add("原则有效性较低，建议审视现有原则是否适用");
            }

            if (metrics.improvementRate() < 0) {
                recommendations.add("改进率为负，需要分析最近进化的效果并调整策略");
            }

            if (recommendations.isEmpty()) {
                recommendations.add("各项指标正常，继续当前进化策略");
            }

            return recommendations;
        }
    }

    public record EvolutionProgressReport(
        int currentLevel,
        int totalEvolutions,
        EvolutionMetrics currentMetrics,
        EvolutionMetrics previousMetrics,
        String status,
        List<String> recommendations
    ) {}
}
