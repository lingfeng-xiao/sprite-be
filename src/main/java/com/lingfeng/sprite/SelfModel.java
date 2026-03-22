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
 * ├── values           // 价值观体系
 * ├── capabilities     // 能力认知
 * ├── metacognition    // 元认知（关于如何思考的知识）
 * ├── growthHistory    // 成长轨迹
 * ├── evolutionLevel   // 进化等级
 * └── evolutionCount   // 进化次数
 *
 * ## 核心概念
 *
 * - IdentityCore - 自我认同核心（beingId 不可变）
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
        IDENTITY_DEEPENED
    }

    /**
     * 自我模型完整视图
     */
    public record Self(
        IdentityCore identity,
        List<Value> values,
        List<Capability> capabilities,
        Metacognition metacognition,
        List<GrowthEvent> growthHistory,
        int evolutionLevel,
        int evolutionCount
    ) {
        public Self {
            values = values != null ? List.copyOf(values) : List.of();
            capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
            growthHistory = growthHistory != null ? List.copyOf(growthHistory) : List.of();
        }

        /**
         * 创建默认的 Sprite 自我模型
         */
        public static Self createDefault() {
            return new Self(
                IdentityCore.createDefault(),
                List.of(),
                List.of(),
                new Metacognition(
                    "",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
                ),
                List.of(),
                1,
                0
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
            return new Self(identity, values, capabilities, metacognition, newHistory, evolutionLevel, evolutionCount + 1);
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
                values,
                capabilities,
                metacognition.withReflection(reflection),
                growthHistory,
                evolutionLevel,
                evolutionCount
            );
        }

        /**
         * 获取核心价值观描述
         */
        public String getCoreValuesSummary() {
            return values.stream()
                .sorted(Comparator.comparingDouble(Value::weight).reversed())
                .limit(3)
                .map(Value::name)
                .collect(Collectors.joining(" > "));
        }

        // With methods for immutable updates
        public Self withIdentity(IdentityCore newIdentity) {
            return new Self(newIdentity, values, capabilities, metacognition, growthHistory, evolutionLevel, evolutionCount);
        }

        public Self withCapabilities(List<Capability> newCapabilities) {
            return new Self(identity, values, newCapabilities, metacognition, growthHistory, evolutionLevel, evolutionCount);
        }
    }
}
