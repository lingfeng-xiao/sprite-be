package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.cognition.CognitionController;
import com.lingfeng.sprite.EvolutionEngine;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.Sprite;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.llm.MinMaxConfig;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;
import com.lingfeng.sprite.sensor.RealEnvironmentSensor;
import com.lingfeng.sprite.sensor.RealPlatformSensor;
import com.lingfeng.sprite.sensor.RealUserSensor;
import com.lingfeng.sprite.config.AppConfig;
import com.lingfeng.sprite.llm.MinMaxConfig;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;

/**
 * Sprite 核心服务
 *
 * 封装 Sprite 实例的管理，提供认知循环触发、状态获取、反馈记录等功能
 */
@Service
public class SpriteService {

    private static final Logger logger = LoggerFactory.getLogger(SpriteService.class);

    private final Sprite sprite;
    private final MemorySystem.Memory memory;
    private final EvolutionEngine.Engine evolutionEngine;
    private final MemoryConsolidationService memoryConsolidationService;
    private final EvolutionService evolutionService;
    private final ActionExecutor actionExecutor;
    private final UnifiedContextService unifiedContextService;
    private final AvatarService avatarService;

    public SpriteService(
            AppConfig appConfig,
            MinMaxConfig minMaxConfig,
            MinMaxLlmReasoner minMaxLlmReasoner,
            MemoryConsolidationService memoryConsolidationService,
            EvolutionService evolutionService,
            ActionExecutor actionExecutor,
            MemorySystem.Memory memory,
            UnifiedContextService unifiedContextService,
            AvatarService avatarService
    ) {
        this.memoryConsolidationService = memoryConsolidationService;
        this.evolutionService = evolutionService;
        this.actionExecutor = actionExecutor;
        this.memory = memory;
        this.unifiedContextService = unifiedContextService;
        this.avatarService = avatarService;

        // 加载已保存的长期记忆
        this.memory.load();

        // 创建感知系统（使用真实传感器）
        PerceptionSystem.DeviceType deviceType = PerceptionSystem.DeviceType.PC;
        PerceptionSystem.System perceptionSystem = new PerceptionSystem.System(
                java.util.List.of(
                        new RealPlatformSensor("pc-1", deviceType),
                        new RealUserSensor(),
                        new RealEnvironmentSensor()
                )
        );

        // 创建自我模型
        SelfModel.Self selfModel = SelfModel.Self.createDefault();

        // 注册当前设备分身
        avatarService.registerCurrentDevice();

        // 将分身列表注入到自我模型
        SelfModel.Avatars avatars = new SelfModel.Avatars(avatarService.getAllAvatars());
        selfModel = new SelfModel.Self(
            selfModel.identity(),
            selfModel.personality(),
            selfModel.capabilities(),
            avatars,
            selfModel.metacognition(),
            selfModel.growthHistory(),
            selfModel.evolutionLevel(),
            selfModel.evolutionCount()
        );

        // 创建世界模型（使用配置的owner信息）
        OwnerModel.Owner configuredOwner = new OwnerModel.Owner(
            new OwnerModel.OwnerIdentity(
                appConfig.getOwner().getName() != null ? appConfig.getOwner().getName() : "灵锋",
                appConfig.getOwner().getOccupation(),
                List.of()
            ),
            new OwnerModel.LifeContext(
                appConfig.getOwner().getWorkplace(),
                appConfig.getOwner().getHome(),
                new OwnerModel.Family(List.of()),
                List.of()
            ),
            List.of(), List.of(), List.of(),
            null,
            List.of(), List.of(),
            new OwnerModel.TrustLevel(0.5f),
            null, null,
            new OwnerModel.DigitalFootprint(List.of(), List.of(), List.of()),
            List.of(),
            Instant.now()
        );
        WorldModel.World worldModel = new WorldModel.World(configuredOwner);

        // 创建推理引擎（使用真实 LLM）
        com.lingfeng.sprite.cognition.ReasoningEngine reasoningEngine = null;
        if (appConfig.getLlm().isEnabled()) {
            reasoningEngine = new com.lingfeng.sprite.cognition.ReasoningEngine(
                    minMaxLlmReasoner
            );
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
        this.evolutionEngine = EvolutionEngine.Factory.create();

        // 创建 Sprite 实例
        this.sprite = Sprite.createWithComponents(
                selfModel,
                Sprite.Platform.PC,
                cognitionController,
                this.memory,
                evolutionEngine,
                reasoningEngine
        );

        logger.info("SpriteService initialized with LLM support: {}",
                appConfig.getLlm().isEnabled());
    }

    /**
     * 执行一轮完整认知闭环
     */
    public CognitionController.CognitionResult cognitionCycle() {
        logger.debug("Starting cognition cycle");

        // 更新当前设备的心跳
        avatarService.updateLastSeen(avatarService.getCurrentDeviceId());

        // 执行认知闭环
        CognitionController.CognitionResult result = sprite.cognitionCycle();

        // 记忆整合
        memoryConsolidationService.consolidateIfNeeded(memory);

        // 每轮保存一次记忆（避免丢失）
        memory.save();

        // 应用进化结果
        evolutionService.applyEvolution(sprite);

        // 执行推荐动作
        if (result.actionRecommendation() != null
                && !result.actionRecommendation().recommendations().isEmpty()) {
            for (String action : result.actionRecommendation().recommendations()) {
                actionExecutor.execute(action, java.util.Map.of(
                        "perception", result.perception(),
                        "reflection", result.reflection(),
                        "timestamp", Instant.now()
                ));
            }
        }

        // 更新统一上下文（供 ConversationService 使用）
        unifiedContextService.updateContext(
                result.perception(),
                result.selfModel(),
                result.worldModel(),
                memory,
                sprite.getEvolutionEngine()
        );

        logger.debug("Cognition cycle completed");
        return result;
    }

    /**
     * 获取当前状态
     */
    public Sprite.State getState() {
        return sprite.getState();
    }

    /**
     * 记录反馈
     */
    public void recordFeedback(EvolutionEngine.Feedback.FeedbackSource type, String content,
            String outcome, boolean success, EvolutionEngine.Impact impact) {
        sprite.recordFeedback(type, content, outcome, success, impact);
    }

    /**
     * 获取记忆状态
     */
    public MemorySystem.MemoryStatus getMemoryStatus() {
        return sprite.getMemoryStatus();
    }

    /**
     * 获取进化状态
     */
    public EvolutionEngine.EvolutionStatus getEvolutionStatus() {
        return sprite.getEvolutionStatus();
    }

    /**
     * 获取认知统计
     */
    public CognitionController.CognitionStats getCognitionStats() {
        return sprite.getCognitionStats();
    }

    /**
     * 启动 Sprite
     */
    public void start() {
        sprite.start();
        logger.info("Sprite started");
    }

    /**
     * 停止 Sprite
     */
    public void stop() {
        sprite.stop();
        logger.info("Sprite stopped");
    }
}
