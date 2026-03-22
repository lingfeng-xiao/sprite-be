package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 世界模型构建引擎 - 感知 → 主人理解 + 世界知识
 *
 * ## 核心能力
 *
 * 1. **主人情感更新**：从感知中推断主人当前情感状态
 * 2. **习惯学习**：从重复行为中学习主人习惯
 * 3. **偏好推断**：从行动结果中推断主人偏好
 * 4. **事实构建**：将感知转化为世界知识
 */
public class WorldBuilder {
    private final List<BehaviorObservation> behaviorObservations = new ArrayList<>();
    private PerceptionSystem.PresenceStatus lastPresence = null;
    private int presenceStreak = 0;

    /**
     * 从感知构建世界模型
     */
    public WorldUpdateResult build(
        PerceptionSystem.Perception perception,
        WorldModel.World currentWorld
    ) {
        WorldModel.World world = currentWorld;

        // 1. 更新主人情感状态
        world = updateEmotionalState(perception, world);

        // 2. 学习习惯
        List<HabitLearning> habitLearnings = learnHabits(perception, world);

        // 3. 推断偏好
        List<PreferenceInference> preferenceInferences = inferPreferences(perception, world);

        // 4. 构建世界知识
        world = buildWorldKnowledge(perception, world);

        // 5. 更新交互历史
        world = updateInteractionHistory(perception, world);

        return new WorldUpdateResult(
            world,
            habitLearnings,
            preferenceInferences,
            habitLearnings.size() + preferenceInferences.size()
        );
    }

    private WorldModel.World updateEmotionalState(
        PerceptionSystem.Perception perception,
        WorldModel.World world
    ) {
        PerceptionSystem.PresenceStatus currentPresence = perception.user() != null ?
            perception.user().presence() : null;

        WorldModel.Mood mood = WorldModel.Mood.NEUTRAL;
        if (currentPresence != null) {
            switch (currentPresence) {
                case ACTIVE: mood = WorldModel.Mood.CALM; break;
                case IDLE: mood = WorldModel.Mood.NEUTRAL; break;
                case AWAY: mood = WorldModel.Mood.TIRED; break;
                default: mood = WorldModel.Mood.NEUTRAL;
            }
        }

        WorldModel.Mood contextMood = mood;
        if (perception.environment() != null) {
            switch (perception.environment().context()) {
                case WORK: contextMood = WorldModel.Mood.CALM; break;
                case LEISURE: contextMood = WorldModel.Mood.HAPPY; break;
                case SLEEP: contextMood = WorldModel.Mood.TIRED; break;
                default: contextMood = mood;
            }
        }

        WorldModel.Mood systemMood = contextMood;
        if (perception.platform() != null) {
            if (perception.platform().battery() != null &&
                perception.platform().battery().chargePercent() < 20) {
                systemMood = WorldModel.Mood.ANXIOUS;
            } else if (perception.platform().memory() != null &&
                perception.platform().memory().usedPercent() > 80) {
                systemMood = WorldModel.Mood.CONFUSED;
            } else if (perception.platform().network() != null &&
                !perception.platform().network().isConnected()) {
                systemMood = WorldModel.Mood.FRUSTRATED;
            }
        }

        if (currentPresence == lastPresence) {
            presenceStreak++;
        } else {
            presenceStreak = 0;
        }
        lastPresence = currentPresence;

        WorldModel.Mood finalMood = presenceStreak > 3 ? systemMood : mood;

        float intensity = 0.3f;
        switch (finalMood) {
            case ANXIOUS: intensity = 0.7f; break;
            case FRUSTRATED: intensity = 0.6f; break;
            case CONFUSED: intensity = 0.5f; break;
            case TIRED: intensity = 0.4f; break;
            default: intensity = 0.3f;
        }

        return world.updateEmotionalState(finalMood, intensity, perception.generateFeelings());
    }

    private List<HabitLearning> learnHabits(
        PerceptionSystem.Perception perception,
        WorldModel.World world
    ) {
        List<HabitLearning> learnings = new ArrayList<>();

        PerceptionSystem.WindowInfo window = perception.user() != null ?
            perception.user().activeWindow() : null;
        if (window != null) {
            BehaviorObservation observation = new BehaviorObservation(
                Instant.now(),
                "active_window",
                window.title(),
                perception.environment() != null ? perception.environment().context().name() : "UNKNOWN"
            );
            behaviorObservations.add(observation);

            List<BehaviorObservation> recentObs = new ArrayList<>();
            for (BehaviorObservation obs : behaviorObservations) {
                if (obs.type().equals("active_window") && obs.value().equals(window.title())) {
                    recentObs.add(obs);
                }
            }
            if (recentObs.size() > 5) {
                recentObs = recentObs.subList(recentObs.size() - 5, recentObs.size());
            }

            if (recentObs.size() >= 3) {
                WorldModel.Habit existingHabit = null;
                for (WorldModel.Habit h : world.owner().habits()) {
                    if (h.trigger().toLowerCase().contains(window.title().toLowerCase())) {
                        existingHabit = h;
                        break;
                    }
                }

                if (existingHabit == null) {
                    WorldModel.Habit habit = new WorldModel.Habit(
                        UUID.randomUUID().toString(),
                        "打开 " + window.title(),
                        window.processName(),
                        WorldModel.Frequency.USUALLY,
                        Instant.now(),
                        recentObs.size()
                    );
                    learnings.add(new HabitLearning(
                        habit,
                        recentObs.size() / 10f,
                        "检测到 " + recentObs.size() + " 次重复行为"
                    ));
                }
            }
        }

        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
        behaviorObservations.removeIf(obs -> obs.timestamp().isBefore(cutoff));

        return learnings;
    }

    private List<PreferenceInference> inferPreferences(
        PerceptionSystem.Perception perception,
        WorldModel.World world
    ) {
        List<PreferenceInference> inferences = new ArrayList<>();

        PerceptionSystem.WindowInfo window = perception.user() != null ?
            perception.user().activeWindow() : null;
        if (window != null) {
            String appType = window.appType().name();
            WorldModel.Preference.Inferred existingPref = null;
            for (WorldModel.Preference p : world.owner().inferredPreferences()) {
                if (p instanceof WorldModel.Preference.Inferred &&
                    ((WorldModel.Preference.Inferred) p).key().equals("preferred_app_type")) {
                    existingPref = (WorldModel.Preference.Inferred) p;
                    break;
                }
            }

            if (existingPref == null) {
                inferences.add(new PreferenceInference(
                    "preferred_app_type",
                    appType,
                    0.5f,
                    List.of("当前使用: " + window.title()),
                    "基于当前应用使用推断"
                ));
            } else if (existingPref.value().equals(appType)) {
                float newConfidence = Math.min(existingPref.confidence() + 0.1f, 0.9f);
                List<String> newEvidence = new ArrayList<>(existingPref.inferredFrom());
                newEvidence.add("当前使用: " + window.title());
                inferences.add(new PreferenceInference(
                    "preferred_app_type",
                    appType,
                    newConfidence,
                    newEvidence,
                    "强化推断：连续使用相同类型应用"
                ));
            }
        }

        PerceptionSystem.EnvironmentPerception env = perception.environment();
        if (env != null) {
            WorldModel.Preference.Inferred existingHours = null;
            for (WorldModel.Preference p : world.owner().inferredPreferences()) {
                if (p instanceof WorldModel.Preference.Inferred &&
                    ((WorldModel.Preference.Inferred) p).key().equals("active_hours")) {
                    existingHours = (WorldModel.Preference.Inferred) p;
                    break;
                }
            }

            if (existingHours == null && perception.user() != null &&
                perception.user().presence() == PerceptionSystem.PresenceStatus.ACTIVE) {
                inferences.add(new PreferenceInference(
                    "active_hours",
                    String.valueOf(env.hourOfDay()),
                    0.4f,
                    List.of("在 " + env.hourOfDay() + " 点检测到活跃"),
                    "基于检测到的活跃时间推断"
                ));
            }
        }

        return inferences;
    }

    private WorldModel.World buildWorldKnowledge(
        PerceptionSystem.Perception perception,
        WorldModel.World world
    ) {
        WorldModel.World updated = world;

        WorldModel.EmotionalState emotion = updated.owner().emotionalState();
        if (emotion != null) {
            WorldModel.Fact fact = new WorldModel.Fact(
                UUID.randomUUID().toString(),
                "主人当前情绪: " + emotion.currentMood().name() + " (强度: " + emotion.intensity() + ")",
                "perception_system",
                0.8f,
                Instant.now()
            );
            updated = new WorldModel.World(
                updated.owner(),
                updated.physicalWorld(),
                updated.socialGraph(),
                new WorldModel.KnowledgeGraph(
                    concat(updated.knowledgeGraph().facts(), List.of(fact)),
                    updated.knowledgeGraph().beliefs(),
                    updated.knowledgeGraph().concepts()
                ),
                updated.currentContext()
            );
        }

        if (perception.platform() != null && perception.platform().memory() != null &&
            perception.platform().memory().usedPercent() > 80) {
            WorldModel.Fact fact = new WorldModel.Fact(
                UUID.randomUUID().toString(),
                "系统内存使用率较高: " + perception.platform().memory().usedPercent() + "%",
                "platform_sensor",
                0.95f,
                Instant.now()
            );
            updated = new WorldModel.World(
                updated.owner(),
                updated.physicalWorld(),
                updated.socialGraph(),
                new WorldModel.KnowledgeGraph(
                    concat(updated.knowledgeGraph().facts(), List.of(fact)),
                    updated.knowledgeGraph().beliefs(),
                    updated.knowledgeGraph().concepts()
                ),
                updated.currentContext()
            );
        }

        if (updated.knowledgeGraph().facts().size() > 100) {
            List<WorldModel.Fact> last100 = updated.knowledgeGraph().facts().size() > 100 ?
                updated.knowledgeGraph().facts().subList(
                    updated.knowledgeGraph().facts().size() - 100,
                    updated.knowledgeGraph().facts().size()
                ) : updated.knowledgeGraph().facts();
            updated = new WorldModel.World(
                updated.owner(),
                updated.physicalWorld(),
                updated.socialGraph(),
                new WorldModel.KnowledgeGraph(
                    new ArrayList<>(last100),
                    updated.knowledgeGraph().beliefs(),
                    updated.knowledgeGraph().concepts()
                ),
                updated.currentContext()
            );
        }

        return updated;
    }

    private WorldModel.World updateInteractionHistory(
        PerceptionSystem.Perception perception,
        WorldModel.World world
    ) {
        if (!isSignificantChange(perception, world)) {
            return world;
        }

        WorldModel.InteractionType interactionType = classifyInteraction(perception);
        float sentiment = calculateSentiment(perception);

        return world.recordInteraction(interactionType, describePerception(perception), sentiment, null);
    }

    private boolean isSignificantChange(
        PerceptionSystem.Perception perception,
        WorldModel.World world
    ) {
        WorldModel.Mood lastEmotion = world.owner().emotionalState() != null ?
            world.owner().emotionalState().currentMood() : null;

        WorldModel.Mood currentEmotion = WorldModel.Mood.NEUTRAL;
        if (perception.user() != null) {
            switch (perception.user().presence()) {
                case ACTIVE: currentEmotion = WorldModel.Mood.CALM; break;
                case IDLE: currentEmotion = WorldModel.Mood.NEUTRAL; break;
                case AWAY: currentEmotion = WorldModel.Mood.TIRED; break;
                default: currentEmotion = WorldModel.Mood.NEUTRAL;
            }
        }

        return lastEmotion != currentEmotion;
    }

    private WorldModel.InteractionType classifyInteraction(PerceptionSystem.Perception perception) {
        if (perception.user() == null) return WorldModel.InteractionType.QUESTION;
        switch (perception.user().presence()) {
            case ACTIVE: return WorldModel.InteractionType.REQUEST;
            case IDLE: return WorldModel.InteractionType.CASUAL;
            case AWAY: return WorldModel.InteractionType.CASUAL;
            default: return WorldModel.InteractionType.QUESTION;
        }
    }

    private float calculateSentiment(PerceptionSystem.Perception perception) {
        if (perception.user() == null) return 0.0f;
        switch (perception.user().presence()) {
            case ACTIVE: return 0.3f;
            case IDLE: return 0.0f;
            case AWAY: return -0.2f;
            default: return 0.0f;
        }
    }

    private String describePerception(PerceptionSystem.Perception perception) {
        List<String> parts = new ArrayList<>();

        if (perception.user() != null && perception.user().activeWindow() != null) {
            parts.add("使用 " + perception.user().activeWindow().title());
        }
        if (perception.environment() != null) {
            parts.add("环境: " + perception.environment().context().name());
        }
        if (perception.user() != null) {
            parts.add("状态: " + perception.user().presence().name());
        }

        return String.join(", ", parts);
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> result = new ArrayList<>(a);
        result.addAll(b);
        return result;
    }

    public record BehaviorObservation(
        Instant timestamp,
        String type,
        String value,
        String context
    ) {}

    public record HabitLearning(
        WorldModel.Habit habit,
        float confidence,
        String reason
    ) {}

    public record PreferenceInference(
        String key,
        String value,
        float confidence,
        List<String> evidence,
        String reasoning
    ) {}

    public record WorldUpdateResult(
        WorldModel.World updatedWorld,
        List<HabitLearning> habitLearnings,
        List<PreferenceInference> preferenceInferences,
        int factsAdded
    ) {}
}
