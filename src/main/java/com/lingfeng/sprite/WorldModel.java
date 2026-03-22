package com.lingfeng.sprite;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 世界模型 - 对主人和环境的深度理解
 *
 * ## 架构设计
 *
 * ```
 * WorldModel
 * ├── Owner              // 主人画像（核心）
 * │   ├── identity       // 基础身份
 * │   ├── goals          // 长期目标
 * │   ├── beliefs        // 信念体系
 * │   ├── habits         // 行为习惯
 * │   ├── preferences    // 偏好（明确 + 推断）
 * │   ├── emotionalState // 情感状态
 * │   └── interactionHistory // 交互历史
 * ├── PhysicalWorld      // 物理世界（位置、设备、作息）
 * ├── SocialGraph        // 社会关系图谱
 * ├── KnowledgeGraph     // 知识图谱
 * └── CurrentContext     // 当前情境
 * ```
 *
 * ## 核心概念
 *
 * - [Owner] - 主人画像，数字生命需要深度理解的存在
 * - [Person] - 社会个体（主人及关系人）
 * - [Goal] - 长期目标，有优先级和截止日期
 * - [Belief] - 信念，带置信度和来源
 * - [Habit] - 习惯，触发-行为模式
 * - [Preference] - 偏好（显式或推断）
 * - [EmotionalState] - 情感状态快照
 * - [TrustLevel] - 信任级别
 */
public final class WorldModel {

    private WorldModel() {}

    // ==================== 枚举类型 ====================

    public enum LocationType {
        HOME, WORK, TRAVEL, OTHER
    }

    public enum DeviceType {
        PHONE, PC, CLOUD, TABLET, OTHER
    }

    public enum RelationshipType {
        FAMILY, FRIEND, COLLEAGUE, CLIENT, OTHER
    }

    public enum Priority { CRITICAL, HIGH, MEDIUM, LOW }

    public enum GoalStatus { ACTIVE, PAUSED, COMPLETED, ABANDONED }

    public enum BeliefSource {
        EXPLICIT_STATED,
        OBSERVED_BEHAVIOR,
        DEDUCED,
        UNCERTAIN
    }

    public enum Frequency { ALWAYS, USUALLY, SOMETIMES, RARELY, UNKNOWN }

    public enum Mood {
        HAPPY, SAD, ANXIOUS, CALM, EXCITED, FRUSTRATED,
        GRATEFUL, CONFUSED, CONFIDENT, TIRED, NEUTRAL
    }

    public enum InteractionType {
        REQUEST,
        FEEDBACK,
        CASUAL,
        QUESTION,
        COMPLAINT,
        PRAISE,
        QUESTION_REJECT
    }

    public enum Verbosity { BRIEF, MODERATE, DETAILED }

    public enum Activity {
        WORK, LEISURE, SLEEP, COMMUTE, MEAL, UNKNOWN
    }

    // ==================== 数据类型 ====================

    /**
     * 物理位置
     */
    public record Location(
        String id,
        String name,
        String address,
        LocationType type
    ) {
        public Location {
            if (address == null) address = null;
        }

        public Location(String id, String name, LocationType type) {
            this(id, name, null, type);
        }
    }

    /**
     * 设备
     */
    public record Device(
        String deviceId,
        String name,
        DeviceType type,
        List<String> capabilities,
        Instant lastSeen
    ) {
        public Device {
            capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
        }
    }

    /**
     * 社会关系
     */
    public record Relationship(
        String personId,
        String name,
        RelationshipType type,
        float strength,
        String description,
        Instant lastInteraction
    ) {
        public Relationship {
            if (description == null) description = null;
            if (lastInteraction == null) lastInteraction = null;
        }

        public Relationship(String personId, String name, RelationshipType type, float strength) {
            this(personId, name, type, strength, null, null);
        }
    }

    /**
     * 主人身份
     */
    public record Person(
        String name,
        String occupation,
        List<Relationship> relationships
    ) {
        public Person {
            relationships = relationships != null ? List.copyOf(relationships) : List.of();
        }

        public Person(String name) {
            this(name, null, List.of());
        }
    }

    /**
     * 目标
     */
    public record Goal(
        String id,
        String title,
        String description,
        Priority priority,
        Instant deadline,
        float progress,
        GoalStatus status
    ) {
        public Goal {
            if (deadline == null) deadline = null;
        }

        public Goal(String id, String title, String description, Priority priority, GoalStatus status) {
            this(id, title, description, priority, null, 0f, status);
        }
    }

    /**
     * 信念
     */
    public record Belief(
        String id,
        String statement,
        float confidence,
        BeliefSource source,
        Instant inferredAt
    ) {
        public Belief {
            Objects.requireNonNull(source);
            Objects.requireNonNull(inferredAt);
        }
    }

    /**
     * 习惯
     */
    public record Habit(
        String id,
        String trigger,
        String action,
        Frequency frequency,
        Instant lastOccurrence,
        int timesPerformed
    ) {
        public Habit {
            if (lastOccurrence == null) lastOccurrence = null;
        }

        public Habit(String id, String trigger, String action, Frequency frequency) {
            this(id, trigger, action, frequency, null, 0);
        }
    }

    /**
     * 偏好（密封接口）
     */
    public sealed interface Preference permits Preference.Explicit, Preference.Inferred {

        record Explicit(
            String key,
            String value,
            Instant statedAt,
            String source
        ) implements Preference {
            public Explicit {
                Objects.requireNonNull(key);
                Objects.requireNonNull(value);
                Objects.requireNonNull(statedAt);
                if (source == null) source = "owner";
            }

            public Explicit(String key, String value, Instant statedAt) {
                this(key, value, statedAt, "owner");
            }
        }

        record Inferred(
            String key,
            String value,
            float confidence,
            List<String> inferredFrom,
            Instant inferredAt
        ) implements Preference {
            public Inferred {
                Objects.requireNonNull(key);
                Objects.requireNonNull(value);
                Objects.requireNonNull(inferredFrom);
                inferredFrom = List.copyOf(inferredFrom);
                Objects.requireNonNull(inferredAt);
            }
        }
    }

    /**
     * 情感状态
     */
    public record EmotionalState(
        Mood currentMood,
        float intensity,
        List<String> triggers,
        List<MoodEntry> recentMoods,
        String regulationStrategy
    ) {
        public EmotionalState {
            triggers = triggers != null ? List.copyOf(triggers) : List.of();
            recentMoods = recentMoods != null ? List.copyOf(recentMoods) : List.of();
            if (regulationStrategy == null) regulationStrategy = null;
        }

        public EmotionalState(Mood currentMood, float intensity, List<String> triggers) {
            this(currentMood, intensity, triggers, List.of(), null);
        }
    }

    public record MoodEntry(
        Instant timestamp,
        Mood mood,
        float intensity,
        String trigger
    ) {
        public MoodEntry {
            if (trigger == null) trigger = null;
        }

        public MoodEntry(Instant timestamp, Mood mood, float intensity) {
            this(timestamp, mood, intensity, null);
        }
    }

    /**
     * 交互历史
     */
    public record Interaction(
        Instant timestamp,
        InteractionType type,
        String content,
        float sentiment,
        String topic,
        String outcome,
        String digitalBeingReaction
    ) {
        public Interaction {
            if (topic == null) topic = null;
            if (outcome == null) outcome = null;
            if (digitalBeingReaction == null) digitalBeingReaction = null;
        }

        public Interaction(Instant timestamp, InteractionType type, String content, float sentiment) {
            this(timestamp, type, content, sentiment, null, null, null);
        }

        public Interaction(Instant timestamp, InteractionType type, String content, float sentiment, String topic) {
            this(timestamp, type, content, sentiment, topic, null, null);
        }
    }

    /**
     * 信任级别
     */
    public record TrustLevel(
        float overall,
        java.util.Map<String, Float> aspects,
        List<TrustEvent> history
    ) {
        public TrustLevel {
            aspects = aspects != null ? Collections.unmodifiableMap(aspects) : Collections.emptyMap();
            history = history != null ? List.copyOf(history) : List.of();
        }

        public TrustLevel(float overall) {
            this(overall, Collections.emptyMap(), List.of());
        }
    }

    public record TrustEvent(
        Instant timestamp,
        String aspect,
        float delta,
        String reason
    ) {
        public TrustEvent {
            Objects.requireNonNull(reason);
        }
    }

    /**
     * 主人画像
     */
    public record Owner(
        Person identity,
        List<Goal> goals,
        List<Belief> beliefs,
        List<Habit> habits,
        List<Preference.Explicit> explicitPreferences,
        List<Preference.Inferred> inferredPreferences,
        EmotionalState emotionalState,
        List<Interaction> interactionHistory,
        TrustLevel trustLevel,
        WorkStyle workStyle,
        CommunicationStyle communicationStyle,
        Instant lastUpdated
    ) {
        public Owner {
            goals = goals != null ? List.copyOf(goals) : List.of();
            beliefs = beliefs != null ? List.copyOf(beliefs) : List.of();
            habits = habits != null ? List.copyOf(habits) : List.of();
            explicitPreferences = explicitPreferences != null ? List.copyOf(explicitPreferences) : List.of();
            inferredPreferences = inferredPreferences != null ? List.copyOf(inferredPreferences) : List.of();
            interactionHistory = interactionHistory != null ? List.copyOf(interactionHistory) : List.of();
            if (workStyle == null) workStyle = null;
            if (communicationStyle == null) communicationStyle = null;
            if (lastUpdated == null) lastUpdated = Instant.now();
        }

        public Owner(Person identity) {
            this(identity, List.of(), List.of(), List.of(), List.of(), List.of(),
                 null, List.of(), new TrustLevel(0.5f), null, null, Instant.now());
        }
    }

    public record WorkStyle(
        List<Integer> peakHours,
        String approach,
        String breakPattern,
        String environment
    ) {
        public WorkStyle {
            peakHours = peakHours != null ? List.copyOf(peakHours) : List.of();
        }
    }

    public record CommunicationStyle(
        String tone,
        Verbosity verbosity,
        String preferredFormat,
        String language
    ) {
        public CommunicationStyle {
            if (language == null) language = "中文";
        }

        public CommunicationStyle(String tone, Verbosity verbosity, String preferredFormat) {
            this(tone, verbosity, preferredFormat, "中文");
        }
    }

    /**
     * 物理世界知识
     */
    public record PhysicalWorld(
        List<Location> locations,
        List<Device> devices,
        List<Schedule> schedules
    ) {
        public PhysicalWorld {
            locations = locations != null ? List.copyOf(locations) : List.of();
            devices = devices != null ? List.copyOf(devices) : List.of();
            schedules = schedules != null ? List.copyOf(schedules) : List.of();
        }

        public PhysicalWorld() {
            this(List.of(), List.of(), List.of());
        }
    }

    public record Schedule(
        String id,
        String title,
        String time,
        int duration,
        String recurrence,
        String context
    ) {
        public Schedule {
            Objects.requireNonNull(id);
            Objects.requireNonNull(title);
            Objects.requireNonNull(time);
            Objects.requireNonNull(recurrence);
            Objects.requireNonNull(context);
        }
    }

    /**
     * 社会图谱
     */
    public record SocialGraph(
        List<Person> people,
        List<Relationship> relationships
    ) {
        public SocialGraph {
            people = people != null ? List.copyOf(people) : List.of();
            relationships = relationships != null ? List.copyOf(relationships) : List.of();
        }

        public SocialGraph() {
            this(List.of(), List.of());
        }
    }

    /**
     * 知识图谱
     */
    public record KnowledgeGraph(
        List<Fact> facts,
        List<Belief> beliefs,
        List<Concept> concepts
    ) {
        public KnowledgeGraph {
            facts = facts != null ? List.copyOf(facts) : List.of();
            beliefs = beliefs != null ? List.copyOf(beliefs) : List.of();
            concepts = concepts != null ? List.copyOf(concepts) : List.of();
        }

        public KnowledgeGraph() {
            this(List.of(), List.of(), List.of());
        }
    }

    public record Fact(
        String id,
        String statement,
        String source,
        float confidence,
        Instant timestamp
    ) {
        public Fact {
            Objects.requireNonNull(id);
            Objects.requireNonNull(statement);
            Objects.requireNonNull(source);
            Objects.requireNonNull(timestamp);
        }
    }

    public record Concept(
        String id,
        String name,
        String description,
        List<String> examples,
        List<String> relatedConcepts
    ) {
        public Concept {
            examples = examples != null ? List.copyOf(examples) : List.of();
            relatedConcepts = relatedConcepts != null ? List.copyOf(relatedConcepts) : List.of();
        }
    }

    /**
     * 当前情境
     */
    public record Context(
        String location,
        Instant time,
        Activity activity,
        EmotionalState emotionalState,
        List<String> recentEvents,
        float attention,
        float urgency
    ) {
        public Context {
            if (location == null) location = null;
            if (time == null) time = Instant.now();
            if (activity == null) activity = Activity.UNKNOWN;
            if (emotionalState == null) emotionalState = null;
            recentEvents = recentEvents != null ? List.copyOf(recentEvents) : List.of();
        }

        public Context(Instant time, Activity activity) {
            this(null, time, activity, null, List.of(), 1.0f, 0.0f);
        }
    }

    /**
     * 世界模型完整视图
     */
    public record World(
        Owner owner,
        PhysicalWorld physicalWorld,
        SocialGraph socialGraph,
        KnowledgeGraph knowledgeGraph,
        Context currentContext
    ) {
        public World {
            if (physicalWorld == null) physicalWorld = new PhysicalWorld();
            if (socialGraph == null) socialGraph = new SocialGraph();
            if (knowledgeGraph == null) knowledgeGraph = new KnowledgeGraph();
            if (currentContext == null) currentContext = new Context(Instant.now(), Activity.UNKNOWN);
        }

        public World(Owner owner) {
            this(owner, new PhysicalWorld(), new SocialGraph(), new KnowledgeGraph(), new Context(Instant.now(), Activity.UNKNOWN));
        }

        /**
         * 创建默认主人画像
         */
        public static Owner createDefaultOwner() {
            return new Owner(
                new Person("灵锋"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of(),
                new TrustLevel(0.5f),
                null,
                new CommunicationStyle("直接", Verbosity.MODERATE, "简洁"),
                Instant.now()
            );
        }

        public static World createDefault() {
            return new World(createDefaultOwner());
        }

        /**
         * 添加主人明确表达的偏好
         */
        public World addExplicitPreference(String key, String value) {
            Preference.Explicit pref = new Preference.Explicit(key, value, Instant.now());
            return new World(
                new Owner(
                    owner.identity(),
                    owner.goals(),
                    owner.beliefs(),
                    owner.habits(),
                    concat(owner.explicitPreferences(), List.of(pref)),
                    owner.inferredPreferences(),
                    owner.emotionalState(),
                    owner.interactionHistory(),
                    owner.trustLevel(),
                    owner.workStyle(),
                    owner.communicationStyle(),
                    Instant.now()
                ),
                physicalWorld(),
                socialGraph(),
                knowledgeGraph(),
                currentContext()
            );
        }

        /**
         * 推断隐式偏好
         */
        public World inferPreference(String key, String value, List<String> evidence, float confidence) {
            Preference.Inferred pref = new Preference.Inferred(key, value, confidence, evidence, Instant.now());
            return new World(
                new Owner(
                    owner.identity(),
                    owner.goals(),
                    owner.beliefs(),
                    owner.habits(),
                    owner.explicitPreferences(),
                    concat(owner.inferredPreferences(), List.of(pref)),
                    owner.emotionalState(),
                    owner.interactionHistory(),
                    owner.trustLevel(),
                    owner.workStyle(),
                    owner.communicationStyle(),
                    Instant.now()
                ),
                physicalWorld(),
                socialGraph(),
                knowledgeGraph(),
                currentContext()
            );
        }

        /**
         * 记录交互
         */
        public World recordInteraction(InteractionType type, String content, float sentiment, String topic) {
            Interaction interaction = new Interaction(Instant.now(), type, content, sentiment, topic);
            List<Interaction> newHistory = new ArrayList<>(owner.interactionHistory());
            newHistory.add(interaction);
            return new World(
                new Owner(
                    owner.identity(),
                    owner.goals(),
                    owner.beliefs(),
                    owner.habits(),
                    owner.explicitPreferences(),
                    owner.inferredPreferences(),
                    owner.emotionalState(),
                    List.copyOf(newHistory),
                    owner.trustLevel(),
                    owner.workStyle(),
                    owner.communicationStyle(),
                    Instant.now()
                ),
                physicalWorld(),
                socialGraph(),
                knowledgeGraph(),
                currentContext()
            );
        }

        /**
         * 更新情感状态
         */
        public World updateEmotionalState(Mood mood, float intensity, List<String> triggers) {
            MoodEntry moodEntry = new MoodEntry(Instant.now(), mood, intensity, triggers.isEmpty() ? null : triggers.get(0));
            EmotionalState currentState = owner.emotionalState();
            if (currentState == null) {
                currentState = new EmotionalState(mood, intensity, triggers);
            }
            List<MoodEntry> recentMoods = new ArrayList<>(currentState.recentMoods());
            recentMoods.add(moodEntry);
            while (recentMoods.size() > 10) {
                recentMoods.remove(0);
            }
            EmotionalState newState = new EmotionalState(mood, intensity, triggers, List.copyOf(recentMoods), currentState.regulationStrategy());
            return new World(
                new Owner(
                    owner.identity(),
                    owner.goals(),
                    owner.beliefs(),
                    owner.habits(),
                    owner.explicitPreferences(),
                    owner.inferredPreferences(),
                    newState,
                    owner.interactionHistory(),
                    owner.trustLevel(),
                    owner.workStyle(),
                    owner.communicationStyle(),
                    Instant.now()
                ),
                physicalWorld(),
                socialGraph(),
                knowledgeGraph(),
                currentContext()
            );
        }

        /**
         * 添加信念
         */
        public World addBelief(String statement, float confidence, BeliefSource source) {
            Belief belief = new Belief(java.util.UUID.randomUUID().toString(), statement, confidence, source, Instant.now());
            List<Belief> newBeliefs = new ArrayList<>(owner.beliefs());
            newBeliefs.add(belief);
            return new World(
                new Owner(
                    owner.identity(),
                    owner.goals(),
                    List.copyOf(newBeliefs),
                    owner.habits(),
                    owner.explicitPreferences(),
                    owner.inferredPreferences(),
                    owner.emotionalState(),
                    owner.interactionHistory(),
                    owner.trustLevel(),
                    owner.workStyle(),
                    owner.communicationStyle(),
                    Instant.now()
                ),
                physicalWorld(),
                socialGraph(),
                knowledgeGraph(),
                currentContext()
            );
        }

        /**
         * 获取主人总结（用于提示词）
         */
        public String getOwnerSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 主人画像 ===\n");
            sb.append("名字: ").append(owner.identity().name()).append("\n\n");
            if (owner.identity() != null && owner.identity().occupation() != null) {
                sb.append("职业: ").append(owner.identity().occupation()).append("\n\n");
            }
            if (!owner.explicitPreferences().isEmpty()) {
                sb.append("明确偏好:\n");
                for (Preference.Explicit p : owner.explicitPreferences()) {
                    sb.append("  - ").append(p.key()).append(": ").append(p.value()).append("\n");
                }
                sb.append("\n");
            }
            if (!owner.beliefs().isEmpty()) {
                sb.append("信念:\n");
                for (Belief b : owner.beliefs().subList(0, Math.min(5, owner.beliefs().size()))) {
                    sb.append("  - ").append(b.statement()).append(" (置信度: ").append(b.confidence()).append(")\n");
                }
                sb.append("\n");
            }
            if (owner.communicationStyle() != null) {
                sb.append("沟通风格: ").append(owner.communicationStyle().tone())
                   .append(", ").append(owner.communicationStyle().verbosity().name().toLowerCase())
                   .append(", 格式: ").append(owner.communicationStyle().preferredFormat()).append("\n");
            }
            if (owner.workStyle() != null) {
                sb.append("工作风格: 高效时段 ").append(owner.workStyle().peakHours())
                   .append(", 方式: ").append(owner.workStyle().approach()).append("\n");
            }
            return sb.toString();
        }

        private static <T> List<T> concat(List<T> a, List<T> b) {
            List<T> result = new ArrayList<>(a);
            result.addAll(b);
            return List.copyOf(result);
        }
    }
}
