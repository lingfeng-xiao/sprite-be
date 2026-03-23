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

        /**
         * 反思 - 从观察中提取洞察
         */
        public Insight reflect(Observation observation) {
            List<Observation> recentSameContext = observations.stream()
                .filter(o -> o.context().activity() == observation.context().activity())
                .toList();

            if (recentSameContext.size() < minObservationsForInsight) {
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
                behaviorChanges.size()
            );
        }
    }

    public record LearningStats(
        int totalObservations,
        int totalInsights,
        int totalPrinciples,
        int totalBehaviorChanges
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
        private int evolutionLevel = 1;
        private int evolutionCount = 0;

        public Engine() {
            this(new LearningLoop(), new FeedbackCollector(), new SelfModifier());
        }

        public Engine(LearningLoop learningLoop, FeedbackCollector feedbackCollector, SelfModifier selfModifier) {
            this.learningLoop = learningLoop != null ? learningLoop : new LearningLoop();
            this.feedbackCollector = feedbackCollector != null ? feedbackCollector : new FeedbackCollector();
            this.selfModifier = selfModifier != null ? selfModifier : new SelfModifier();
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
}
