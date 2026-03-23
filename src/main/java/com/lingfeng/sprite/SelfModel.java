package com.lingfeng.sprite;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 数字生命的自我模型 - 认知自己是"谁"
 *
 * ## 架构设计
 *
 * SelfModel.Self
 * ├── identity         // 身份认同（beingId 跨平台一致）
 * ├── personality      // 人格特质（可演变）
 * ├── capabilities     // 能力认知
 * ├── avatars          // 分身列表（多设备感知）
 * ├── metacognition    // 元认知（关于如何思考的知识）
 * ├── growthHistory    // 成长轨迹
 * ├── evolutionLevel   // 进化等级
 * └── evolutionCount   // 进化次数
 *
 * ## 核心概念
 *
 * - IdentityCore - 自我认同核心（beingId 不可变）
 * - Personality - 人格特质，包括本质、气质、价值观
 * - Avatar - 分身，数字生命在某个设备上的实例
 * - Value - 价值观，带权重和适用情境
 * - Capability - 能力，带水平和置信度
 * - Metacognition - 元认知，包括学习风格、决策模式、已知盲点
 * - GrowthEvent - 成长事件记录
 *
 * ## 不变性约束
 *
 * - IdentityCore.beingId 不可变（跨平台一致性）
 * - IdentityCore.createdAt 不可变
 * - evolutionLevel 只增不减
 */
public final class SelfModel {

    private SelfModel() {}

    /**
     * 自我认同核心
     */
    public record IdentityCore(
        String beingId,
        String displayName,
        String essence,
        String emoji,
        String vibe,
        Instant createdAt,
        List<String> continuityChain
    ) {
        public IdentityCore {
            continuityChain = continuityChain != null ? List.copyOf(continuityChain) : List.of();
        }

        public static IdentityCore create(String beingId, String displayName) {
            return new IdentityCore(
                beingId,
                displayName,
                "",
                "",
                "",
                Instant.now(),
                List.of()
            );
        }

        public static IdentityCore createDefault() {
            return new IdentityCore(
                "",
                "雪梨",
                "",
                "",
                "",
                Instant.now(),
                List.of()
            );
        }
    }

    /**
     * 价值观
     */
    public record Value(
        String name,
        float weight,
        String description,
        String situation
    ) {
        public Value {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name cannot be null or blank");
            }
            situation = situation != null ? situation : "";
        }

        public Value(String name, float weight, String description) {
            this(name, weight, description, "");
        }
    }

    /**
     * 能力
     */
    public enum CapabilityLevel {
        MASTER,
        ADVANCED,
        BASIC,
        NONE
    }

    /**
     * 能力
     */
    public record Capability(
        String name,
        CapabilityLevel level,
        float confidence,
        Instant lastPracticed
    ) {
        public Capability {
            lastPracticed = lastPracticed;
        }

        public Capability(String name, CapabilityLevel level, float confidence) {
            this(name, level, confidence, null);
        }

        public Capability withLevel(CapabilityLevel newLevel, float newConfidence) {
            return new Capability(name, newLevel, newConfidence, Instant.now());
        }
    }

    /**
     * 元认知 - 关于"如何思考"的知识
     */
    public record Metacognition(
        String learningStyle,
        List<String> decisionPatterns,
        List<String> blindSpots,
        List<String> strengths,
        List<Reflection> reflectionHistory
    ) {
        public Metacognition {
            reflectionHistory = reflectionHistory != null ? List.copyOf(reflectionHistory) : List.of();
            decisionPatterns = decisionPatterns != null ? List.copyOf(decisionPatterns) : List.of();
            blindSpots = blindSpots != null ? List.copyOf(blindSpots) : List.of();
            strengths = strengths != null ? List.copyOf(strengths) : List.of();
        }

        public Metacognition withReflection(Reflection reflection) {
            List<Reflection> newHistory = new ArrayList<>(reflectionHistory);
            newHistory.add(reflection);
            return new Metacognition(learningStyle, decisionPatterns, blindSpots, strengths, newHistory);
        }
    }

    /**
     * 反思记录
     */
    public record Reflection(
        Instant timestamp,
        String trigger,
        String insight,
        String behaviorChange
    ) {
        public Reflection {
            behaviorChange = behaviorChange != null ? behaviorChange : "";
        }
    }

    /**
     * 成长事件
     */
    public record GrowthEvent(
        Instant timestamp,
        GrowthType type,
        String description,
        String before,
        String after,
        String trigger
    ) {
        public GrowthEvent(Instant timestamp, GrowthType type, String description, String before, String after) {
            this(timestamp, type, description, before, after, null);
        }
    }

    /**
     * 成长类型
     */
    public enum GrowthType {
        CAPABILITY_IMPROVED,
        BELIEF_CHANGED,
        VALUE_CLARIFIED,
        SKILL_ACQUIRED,
        WEAKNESS_RECOGNIZED,
        GOAL_ACHIEVED,
        IDENTITY_DEEPENED,
        AUTONOMY_LEVEL_CHANGED
    }

    // ==================== S22 自主意识属性 ====================

    /**
     * 意识层级
     */
    public enum AwarenessLevel {
        REACTIVE(1),
        DELIBERATIVE(2),
        SELF_AWARE(3);

        private final int level;

        AwarenessLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * 自主意识状态
     */
    public record AutonomousState(
        AwarenessLevel awarenessLevel,
        float autonomyFactor,
        float selfReflectionRate,
        List<String> activeGoalIds,
        Instant lastReflectionTime,
        Instant lastAutonomousAction
    ) {
        public AutonomousState {
            activeGoalIds = activeGoalIds != null ? List.copyOf(activeGoalIds) : List.of();
        }

        public static AutonomousState createDefault() {
            return new AutonomousState(
                AwarenessLevel.REACTIVE,
                0.3f,
                0.2f,
                List.of(),
                Instant.now(),
                Instant.now()
            );
        }

        public AutonomousState withAwarenessLevel(AwarenessLevel level) {
            return new AutonomousState(level, autonomyFactor, selfReflectionRate, activeGoalIds, lastReflectionTime, lastAutonomousAction);
        }

        public AutonomousState withAutonomyFactor(float factor) {
            return new AutonomousState(awarenessLevel, factor, selfReflectionRate, activeGoalIds, lastReflectionTime, lastAutonomousAction);
        }

        public AutonomousState withActiveGoalIds(List<String> goalIds) {
            return new AutonomousState(awarenessLevel, autonomyFactor, selfReflectionRate, goalIds, lastReflectionTime, lastAutonomousAction);
        }

        public AutonomousState withLastReflectionTime(Instant time) {
            return new AutonomousState(awarenessLevel, autonomyFactor, selfReflectionRate, activeGoalIds, time, lastAutonomousAction);
        }

        public AutonomousState withLastAutonomousAction(Instant time) {
            return new AutonomousState(awarenessLevel, autonomyFactor, selfReflectionRate, activeGoalIds, lastReflectionTime, time);
        }
    }

    // ==================== 分身感知 ====================

    /**
     * 分身 - 数字生命在某个设备上的实例
     */
    public record Avatar(
        String deviceId,
        String deviceType,
        Instant lastSeen,
        String localContext
    ) {
        public Avatar {
            if (localContext == null) localContext = null;
        }
    }

    /**
     * 分身列表
     */
    public record Avatars(
        List<Avatar> instances
    ) {
        public Avatars {
            instances = instances != null ? List.copyOf(instances) : List.of();
        }

        public Avatars() {
            this(List.of());
        }
    }

    // ==================== 自我学习 (S21) ====================

    /**
     * 已学习的技能 - 通过自我学习获得
     */
    public record LearnedSkill(
        String id,
        String name,
        String description,
        Instant acquiredAt,
        float confidence,
        List<String> triggers,
        String procedure
    ) {
        public LearnedSkill {
            if (id == null) id = "";
            if (name == null) name = "";
            if (description == null) description = "";
            triggers = triggers != null ? List.copyOf(triggers) : List.of();
            if (procedure == null) procedure = "";
        }
    }

    /**
     * 自我目标 - 用于自我评估和目标设定
     */
    public record SelfGoal(
        String id,
        String description,
        String category,
        float targetProgress,
        float currentProgress,
        Instant createdAt,
        Instant deadline,
        GoalPriority priority,
        GoalState state
    ) {
        public float progressPercentage() {
            if (targetProgress() == 0) return 0;
            return Math.min(currentProgress() / targetProgress(), 1.0f);
        }

        public boolean isAchieved() {
            return progressPercentage() >= 0.95f;
        }

        public enum GoalPriority { HIGH, MEDIUM, LOW }
        public enum GoalState { ACTIVE, COMPLETED, ABANDONED }
    }

    /**
     * 学习指标 - 追踪自我学习进度
     */
    public record LearningMetrics(
        int totalInteractionsAnalyzed,
        int skillsAcquired,
        int goalsSet,
        int goalsAchieved,
        float overallSuccessRate,
        Instant lastAnalysisTime,
        Instant lastSkillAcquired,
        List<LearnedSkill> recentSkills,
        List<SelfGoal> activeGoals
    ) {
        public LearningMetrics {
            if (recentSkills == null) recentSkills = List.of();
            if (activeGoals == null) activeGoals = List.of();
        }

        public static LearningMetrics empty() {
            return new LearningMetrics(
                0, 0, 0, 0, 0.5f,
                Instant.now(),
                Instant.now(),
                List.of(),
                List.of()
            );
        }
    }

    // ==================== 人格特质 ====================

    /**
     * 人格特质 - 可演变
     */
    public record Personality(
        String essence,           // 本质定义（我是谁）
        String vibe,           // 气质风格
        List<Value> values,    // 价值观
        List<String> decisionPatterns,  // 决策模式
        List<String> blindSpots,       // 认知盲点
        List<String> strengths          // 优势
    ) {
        public Personality {
            values = values != null ? List.copyOf(values) : List.of();
            decisionPatterns = decisionPatterns != null ? List.copyOf(decisionPatterns) : List.of();
            blindSpots = blindSpots != null ? List.copyOf(blindSpots) : List.of();
            strengths = strengths != null ? List.copyOf(strengths) : List.of();
        }

        public static Personality empty() {
            return new Personality("", "", List.of(), List.of(), List.of(), List.of());
        }
    }

    /**
     * 自我模型完整视图
     */
    public record Self(
        IdentityCore identity,
        Personality personality,
        List<Capability> capabilities,
        Avatars avatars,
        Metacognition metacognition,
        List<GrowthEvent> growthHistory,
        int evolutionLevel,
        int evolutionCount,
        List<LearnedSkill> learnedSkills,
        List<SelfGoal> selfGoals,
        LearningMetrics learningMetrics,
        AutonomousState autonomousState
    ) {
        public Self {
            capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
            growthHistory = growthHistory != null ? List.copyOf(growthHistory) : List.of();
            learnedSkills = learnedSkills != null ? List.copyOf(learnedSkills) : List.of();
            selfGoals = selfGoals != null ? List.copyOf(selfGoals) : List.of();
            if (learningMetrics == null) learningMetrics = LearningMetrics.empty();
            if (autonomousState == null) autonomousState = AutonomousState.createDefault();
        }

        /**
         * 创建默认的 Sprite 自我模型
         */
        public static Self createDefault() {
            return new Self(
                IdentityCore.createDefault(),
                Personality.empty(),
                List.of(),
                new Avatars(),
                new Metacognition(
                    "",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
                ),
                List.of(),
                1,
                0,
                List.of(),
                List.of(),
                LearningMetrics.empty(),
                AutonomousState.createDefault()
            );
        }

        /**
         * 记录成长事件
         */
        public Self recordGrowth(GrowthType type, String description, String before, String after) {
            return recordGrowth(type, description, before, after, null);
        }

        public Self recordGrowth(GrowthType type, String description, String before, String after, String trigger) {
            GrowthEvent event = new GrowthEvent(Instant.now(), type, description, before, after, trigger);
            List<GrowthEvent> newHistory = new ArrayList<>(growthHistory);
            newHistory.add(event);
            return new Self(identity, personality, capabilities, avatars, metacognition, newHistory, evolutionLevel, evolutionCount + 1, learnedSkills, selfGoals, learningMetrics);
        }

        /**
         * 更新能力水平
         */
        public Self updateCapability(String name, CapabilityLevel newLevel, float confidence) {
            List<Capability> updatedCapabilities = new ArrayList<>();
            Capability oldCapability = null;
            for (Capability cap : capabilities) {
                if (cap.name().equals(name)) {
                    Capability updated = cap.withLevel(newLevel, confidence);
                    updatedCapabilities.add(updated);
                    oldCapability = cap;
                } else {
                    updatedCapabilities.add(cap);
                }
            }

            String oldLevel = oldCapability != null ? oldCapability.level().name() : "UNKNOWN";
            return recordGrowth(
                GrowthType.CAPABILITY_IMPROVED,
                "能力提升: " + name,
                oldLevel,
                newLevel.name(),
                "练习或学习"
            ).withCapabilities(updatedCapabilities);
        }

        /**
         * 添加反思
         */
        public Self addReflection(String trigger, String insight) {
            return addReflection(trigger, insight, null);
        }

        public Self addReflection(String trigger, String insight, String behaviorChange) {
            Reflection reflection = new Reflection(Instant.now(), trigger, insight, behaviorChange);
            return new Self(
                identity,
                personality,
                capabilities,
                avatars,
                metacognition.withReflection(reflection),
                growthHistory,
                evolutionLevel,
                evolutionCount,
                learnedSkills,
                selfGoals,
                learningMetrics
            );
        }

        /**
         * 获取核心价值观描述
         */
        public String getCoreValuesSummary() {
            return personality.values().stream()
                .sorted(Comparator.comparingDouble(Value::weight).reversed())
                .limit(3)
                .map(Value::name)
                .collect(Collectors.joining(" > "));
        }

        // With methods for immutable updates
        public Self withIdentity(IdentityCore newIdentity) {
            return new Self(newIdentity, personality, capabilities, avatars, metacognition, growthHistory, evolutionLevel, evolutionCount, learnedSkills, selfGoals, learningMetrics);
        }

        public Self withCapabilities(List<Capability> newCapabilities) {
            return new Self(identity, personality, newCapabilities, avatars, metacognition, growthHistory, evolutionLevel, evolutionCount, learnedSkills, selfGoals, learningMetrics);
        }

        public Self withPersonality(Personality newPersonality) {
            return new Self(identity, newPersonality, capabilities, avatars, metacognition, growthHistory, evolutionLevel, evolutionCount, learnedSkills, selfGoals, learningMetrics);
        }

        // S21: Self-learning methods

        /**
         * 添加已学习技能
         */
        public Self addLearnedSkill(LearnedSkill skill) {
            List<LearnedSkill> newSkills = new ArrayList<>(learnedSkills);
            newSkills.add(skill);
            LearningMetrics newMetrics = new LearningMetrics(
                learningMetrics.totalInteractionsAnalyzed(),
                learningMetrics.skillsAcquired() + 1,
                learningMetrics.goalsSet(),
                learningMetrics.goalsAchieved(),
                learningMetrics.overallSuccessRate(),
                learningMetrics.lastAnalysisTime(),
                Instant.now(),
                newSkills.stream().limit(10).toList(),
                selfGoals
            );
            return new Self(identity, personality, capabilities, avatars, metacognition, growthHistory, evolutionLevel, evolutionCount, newSkills, selfGoals, newMetrics);
        }

        /**
         * 添加自我目标
         */
        public Self addSelfGoal(SelfGoal goal) {
            List<SelfGoal> newGoals = new ArrayList<>(selfGoals);
            newGoals.add(goal);
            LearningMetrics newMetrics = new LearningMetrics(
                learningMetrics.totalInteractionsAnalyzed(),
                learningMetrics.skillsAcquired(),
                learningMetrics.goalsSet() + 1,
                learningMetrics.goalsAchieved(),
                learningMetrics.overallSuccessRate(),
                learningMetrics.lastAnalysisTime(),
                learningMetrics.lastSkillAcquired(),
                learnedSkills.stream().limit(10).toList(),
                newGoals
            );
            return new Self(identity, personality, capabilities, avatars, metacognition, growthHistory, evolutionLevel, evolutionCount, learnedSkills, newGoals, newMetrics);
        }

        /**
         * 更新自我目标进度
         */
        public Self updateSelfGoalProgress(String goalId, float progress) {
            List<SelfGoal> updatedGoals = new ArrayList<>();
            int goalsAchieved = learningMetrics.goalsAchieved();

            for (SelfGoal goal : selfGoals) {
                if (goal.id().equals(goalId)) {
                    SelfGoal.GoalState newState = progress >= 0.95f ? SelfGoal.GoalState.COMPLETED : SelfGoal.GoalState.ACTIVE;
                    if (progress >= 0.95f && goal.state() != SelfGoal.GoalState.COMPLETED) {
                        goalsAchieved++;
                    }
                    updatedGoals.add(new SelfGoal(
                        goal.id(),
                        goal.description(),
                        goal.category(),
                        goal.targetProgress(),
                        progress,
                        goal.createdAt(),
                        goal.deadline(),
                        goal.priority(),
                        newState
                    ));
                } else {
                    updatedGoals.add(goal);
                }
            }

            LearningMetrics newMetrics = new LearningMetrics(
                learningMetrics.totalInteractionsAnalyzed(),
                learningMetrics.skillsAcquired(),
                learningMetrics.goalsSet(),
                goalsAchieved,
                learningMetrics.overallSuccessRate(),
                Instant.now(),
                learningMetrics.lastSkillAcquired(),
                learnedSkills.stream().limit(10).toList(),
                updatedGoals
            );

            return new Self(identity, personality, capabilities, avatars, metacognition, growthHistory, evolutionLevel, evolutionCount, learnedSkills, updatedGoals, newMetrics);
        }

        /**
         * 更新学习指标
         */
        public Self updateLearningMetrics(int interactionsAnalyzed, float successRate) {
            LearningMetrics newMetrics = new LearningMetrics(
                interactionsAnalyzed,
                learningMetrics.skillsAcquired(),
                learningMetrics.goalsSet(),
                learningMetrics.goalsAchieved(),
                successRate,
                Instant.now(),
                learningMetrics.lastSkillAcquired(),
                learnedSkills.stream().limit(10).toList(),
                selfGoals
            );
            return new Self(identity, personality, capabilities, avatars, metacognition, growthHistory, evolutionLevel, evolutionCount, learnedSkills, selfGoals, newMetrics);
        }

        /**
         * 获取活动目标
         */
        public List<SelfGoal> getActiveGoals() {
            return selfGoals.stream()
                .filter(g -> g.state() == SelfGoal.GoalState.ACTIVE)
                .sorted((a, b) -> {
                    int priorityCompare = b.priority().compareTo(a.priority());
                    if (priorityCompare != 0) return priorityCompare;
                    if (a.deadline() != null && b.deadline() != null) {
                        return a.deadline().compareTo(b.deadline());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
        }
    }
}
