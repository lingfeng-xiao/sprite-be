package com.lingfeng.sprite;

import com.lingfeng.sprite.cognition.CognitionController;
import com.lingfeng.sprite.cognition.ReasoningEngine;
import com.lingfeng.sprite.llm.MinMaxConfig;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;
import com.lingfeng.sprite.sensor.RealEnvironmentSensor;
import com.lingfeng.sprite.sensor.RealPlatformSensor;
import com.lingfeng.sprite.sensor.RealUserSensor;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprite - 数字生命的完整闭环实现
 *
 * ## 架构设计
 *
 * ```
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                           Sprite (数字生命)                                  │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │
 * │    ┌─────────────────────────────────────────────────────────────┐       │
 * │    │                    感知层 (Perception)                       │       │
 * │    │   RealPlatformSensor ── RealUserSensor ── RealEnvironmentSensor       │       │
 * │    └─────────────────────────────────────────────────────────────┘       │
 * │                              │                                          │
 * │                              ▼                                          │
 * │    ┌─────────────────────────────────────────────────────────────┐       │
 * │    │                    认知层 (Cognition)                        │       │
 * │    │   ┌──────────┐    ┌──────────┐    ┌──────────────┐       │       │
 * │    │   │Perception│───→│ World    │───→│ Self         │       │       │
 * │    │   │  Fusion  │    │ Builder  │    │ Reflector    │       │       │
 * │    │   └──────────┘    └──────────┘    └──────────────┘       │       │
 * │    │                           ▼                                │       │
 * │    │              ┌─────────────────────┐                       │       │
 * │    │              │  Reasoning Engine  │ ← LLM推理引擎          │       │
 * │    │              │  (意图 + 因果 + 预测)│   MinMax集成         │       │
 * │    │              └─────────────────────┘                       │       │
 * │    └─────────────────────────────────────────────────────────────┘       │
 * │                              │                                          │
 * │                              ▼                                          │
 * │    ┌─────────────────────────────────────────────────────────────┐       │
 * │    │                    记忆层 (Memory)                            │       │
 * │    │   Sensory (30s) → Working (7) → LongTerm (Persistent)      │       │
 * │    └─────────────────────────────────────────────────────────────┘       │
 * │                              │                                          │
 * │                              ▼                                          │
 * │    ┌─────────────────────────────────────────────────────────────┐       │
 * │    │                    进化层 (Evolution)                        │       │
 * │    │   FeedbackCollector → LearningLoop → SelfModifier           │       │
 * │    └─────────────────────────────────────────────────────────────┘       │
 * └─────────────────────────────────────────────────────────────────────────┘
 * ```
 *
 * ## 核心特性
 *
 * 1. **感知→认知闭环**：每轮感知都会经过注意力过滤、感知融合、世界构建、自我反思
 * 2. **主动反思**：定时自问"我是谁/主人在想什么"，而非被动响应
 * 3. **世界模型构建**：从感知中实时构建对主人和环境的理解
 * 4. **三层记忆**：感官记忆(30s) → 工作记忆(7项) → 长期记忆(持久化)
 * 5. **LLM 推理**：集成 MinMax LLM，支持意图识别、因果推理、预测
 */
public class Sprite {
    private final SelfModel.Self identity;
    private final Platform platform;
    private final CognitionController cognitionController;
    private final MemorySystem.Memory memory;
    private final EvolutionEngine.Engine evolutionEngine;
    private final ReasoningEngine reasoningEngine;
    private boolean isRunning = false;
    private Instant lastCycleTime = null;

    /**
     * 平台类型
     */
    public enum Platform {
        CLOUD,
        PHONE,
        PC
    }

    private Sprite(
        SelfModel.Self identity,
        Platform platform,
        CognitionController cognitionController,
        MemorySystem.Memory memory,
        EvolutionEngine.Engine evolutionEngine,
        ReasoningEngine reasoningEngine
    ) {
        this.identity = identity;
        this.platform = platform;
        this.cognitionController = cognitionController;
        this.memory = memory;
        this.evolutionEngine = evolutionEngine;
        this.reasoningEngine = reasoningEngine;
    }

    /**
     * 创建 Sprite 实例（无 LLM，使用启发式推理）
     */
    public static Sprite create(String name, Platform platform) {
        return create(name, platform, UUID.randomUUID().toString(), null);
    }

    /**
     * 创建 Sprite 实例（无 LLM，使用启发式推理）
     */
    public static Sprite create(
        String name,
        Platform platform,
        String beingId
    ) {
        return create(name, platform, beingId, null);
    }

    /**
     * 创建 Sprite 实例
     */
    public static Sprite create(
        String name,
        Platform platform,
        String beingId,
        MinMaxConfig llmConfig
    ) {
        // 创建感知系统
        PerceptionSystem.DeviceType deviceType = switch (platform) {
            case PC -> PerceptionSystem.DeviceType.PC;
            case PHONE -> PerceptionSystem.DeviceType.PHONE;
            case CLOUD -> PerceptionSystem.DeviceType.CLOUD;
        };

        PerceptionSystem.System perceptionSystem = new PerceptionSystem.System(
            java.util.List.of(
                new RealPlatformSensor("device-" + platform.name().toLowerCase(), deviceType),
                new RealUserSensor(),
                new RealEnvironmentSensor()
            )
        );

        // 创建记忆系统
        MemorySystem.Memory memory = new MemorySystem.Memory();

        // 创建自我模型
        SelfModel.Self selfModel = SelfModel.Self.createDefault();
        selfModel = new SelfModel.Self(
            new SelfModel.IdentityCore(
                beingId,
                name,
                selfModel.identity().essence(),
                selfModel.identity().emoji(),
                selfModel.identity().vibe(),
                selfModel.identity().createdAt(),
                selfModel.identity().continuityChain()
            ),
            selfModel.personality(),
            selfModel.capabilities(),
            selfModel.avatars(),
            selfModel.metacognition(),
            selfModel.growthHistory(),
            selfModel.evolutionLevel(),
            selfModel.evolutionCount()
        );

        // 创建世界模型
        WorldModel.World worldModel = WorldModel.World.createDefault();

        // 创建推理引擎（如果提供了 LLM 配置）
        ReasoningEngine reasoningEngine = null;
        if (llmConfig != null) {
            reasoningEngine = new ReasoningEngine(new MinMaxLlmReasoner(llmConfig));
        }

        // 创建认知控制器
        CognitionController cognitionController = new CognitionController(
            perceptionSystem,
            memory,
            selfModel,
            worldModel,
            reasoningEngine
        );

        // 创建进化引擎
        EvolutionEngine.Engine evolutionEngine = EvolutionEngine.Factory.create();

        return new Sprite(
            selfModel,
            platform,
            cognitionController,
            memory,
            evolutionEngine,
            reasoningEngine
        );
    }

    /**
     * 使用自定义组件创建 Sprite 实例（用于 Spring Boot 注入）
     */
    public static Sprite createWithComponents(
        SelfModel.Self selfModel,
        Platform platform,
        CognitionController cognitionController,
        MemorySystem.Memory memory,
        EvolutionEngine.Engine evolutionEngine,
        ReasoningEngine reasoningEngine
    ) {
        return new Sprite(
            selfModel,
            platform,
            cognitionController,
            memory,
            evolutionEngine,
            reasoningEngine
        );
    }

    /**
     * 执行一轮完整认知闭环
     */
    public CognitionController.CognitionResult cognitionCycle() {
        CognitionController.CognitionResult result = cognitionController.cognitionCycle();
        lastCycleTime = Instant.now();
        return result;
    }

    /**
     * 记录行动反馈（用于进化引擎）
     */
    public void recordFeedback(
        EvolutionEngine.Feedback.FeedbackSource type,
        String content,
        String outcome,
        boolean success,
        EvolutionEngine.Impact impact
    ) {
        EvolutionEngine.Feedback feedback;
        switch (type) {
            case OWNER_EXPLICIT -> {
                feedback = new EvolutionEngine.Feedback.OwnerFeedback(
                    Instant.now(),
                    content,
                    success ? 0.5f : -0.5f,
                    null
                );
                break;
            }
            case OUTCOME_SUCCESS, OUTCOME_FAILURE -> {
                feedback = new EvolutionEngine.Feedback.OutcomeFeedback(
                    Instant.now(),
                    content,
                    success,
                    outcome,
                    impact
                );
                break;
            }
            case SELF_REVIEW -> {
                feedback = new EvolutionEngine.Feedback.SelfReviewFeedback(
                    Instant.now(),
                    content,
                    outcome,
                    null
                );
                break;
            }
            case PATTERN_DETECTED -> {
                feedback = new EvolutionEngine.Feedback.PatternFeedback(
                    Instant.now(),
                    content,
                    1,
                    outcome
                );
                break;
            }
            default -> {
                return;
            }
        }
        evolutionEngine.collectFeedback(feedback);
    }

    /**
     * 执行进化（触发学习循环）
     */
    public EvolutionEngine.EvolutionResult evolve() {
        CognitionController.CognitionState cognitionState = cognitionController.getCurrentState();
        return evolutionEngine.evolve(cognitionState.selfModel(), cognitionState.worldModel());
    }

    /**
     * 获取当前状态
     */
    public State getState() {
        CognitionController.CognitionState cognitionState = cognitionController.getCurrentState();
        return new State(
            new SelfModel.Self(
                identity.identity(),
                cognitionState.selfModel().personality(),
                cognitionState.selfModel().capabilities(),
                cognitionState.selfModel().avatars(),
                cognitionState.selfModel().metacognition(),
                cognitionState.selfModel().growthHistory(),
                cognitionState.selfModel().evolutionLevel(),
                cognitionState.selfModel().evolutionCount()
            ),
            platform,
            cognitionState.worldModel(),
            cognitionState.memoryStatus(),
            lastCycleTime,
            isRunning,
            reasoningEngine != null
        );
    }

    /**
     * 获取认知统计
     */
    public CognitionController.CognitionStats getCognitionStats() {
        return cognitionController.getStats();
    }

    /**
     * 获取记忆状态
     */
    public MemorySystem.MemoryStatus getMemoryStatus() {
        return memory.getStatus();
    }

    /**
     * 获取进化状态
     */
    public EvolutionEngine.EvolutionStatus getEvolutionStatus() {
        return evolutionEngine.getStatus();
    }

    /**
     * 更新自我模型（用于进化引擎应用）
     */
    public void updateSelfModel(SelfModel.Self newSelfModel) {
        cognitionController.updateSelfModel(newSelfModel);
    }

    /**
     * 启动 Sprite
     */
    public void start() {
        isRunning = true;
    }

    /**
     * 停止 Sprite
     */
    public void stop() {
        isRunning = false;
    }

    public SelfModel.Self getIdentity() {
        return identity;
    }

    public Platform getPlatform() {
        return platform;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public EvolutionEngine.Engine getEvolutionEngine() {
        return evolutionEngine;
    }

    public record State(
        SelfModel.Self identity,
        Platform platform,
        WorldModel.World worldModel,
        MemorySystem.MemoryStatus memoryStatus,
        Instant lastCycleTime,
        boolean isRunning,
        boolean hasLlmSupport
    ) {}
}
