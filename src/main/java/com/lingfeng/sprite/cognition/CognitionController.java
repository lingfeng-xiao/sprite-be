package com.lingfeng.sprite.cognition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.cognition.ReasoningEngine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 认知控制器 - 感知→认知闭环的核心协调器
 */
public class CognitionController {
    private static final Logger logger = LoggerFactory.getLogger(CognitionController.class);

    private final PerceptionSystem.System perceptionSystem;
    private final MemorySystem.Memory memory;
    private SelfModel.Self selfModel;
    private WorldModel.World worldModel;
    private final ReasoningEngine reasoningEngine;
    private final PerceptionPipeline perceptionPipeline = new PerceptionPipeline();
    private final PerceptionFusion perceptionFusion = new PerceptionFusion();
    private final WorldBuilder worldBuilder = new WorldBuilder();
    private final SelfReflector selfReflector = new SelfReflector();
    private final DecisionEngine decisionEngine;
    private final MemoryRetrievalService memoryRetrievalService;
    private Instant lastCycleTime = Instant.now();
    private int cycleCount = 0;

    public CognitionController(
        PerceptionSystem.System perceptionSystem,
        MemorySystem.Memory memory,
        SelfModel.Self initialSelfModel,
        WorldModel.World initialWorldModel,
        ReasoningEngine reasoningEngine
    ) {
        this.perceptionSystem = perceptionSystem;
        this.memory = memory;
        this.selfModel = initialSelfModel;
        this.worldModel = initialWorldModel;
        this.reasoningEngine = reasoningEngine;
        this.decisionEngine = new DecisionEngine(initialWorldModel);
        this.memoryRetrievalService = new MemoryRetrievalService(memory);
    }

    /**
     * 执行一轮完整认知闭环
     */
    public CognitionResult cognitionCycle() {
        // 1. 感知输入
        PerceptionSystem.PerceptionResult perceptionResult = perceptionSystem.perceive();

        // 2. 感知管道处理（注意力过滤）
        PerceptionPipeline.PipelineOutput pipelineOutput = perceptionPipeline.process(perceptionResult);

        // 3. 感知融合
        List<MemorySystem.Stimulus> recentStimuli = memory.getSensory().getRecentStimuli();
        List<MemorySystem.Stimulus> last10 = recentStimuli.size() > 10 ?
            recentStimuli.subList(recentStimuli.size() - 10, recentStimuli.size()) :
            recentStimuli;
        PerceptionSystem.Perception fused = perceptionFusion.fuse(
            pipelineOutput.filteredPerception(),
            new ArrayList<>(last10)
        );

        // 4. 世界模型构建
        WorldBuilder.WorldUpdateResult worldUpdate = worldBuilder.build(fused, worldModel);
        worldModel = worldUpdate.updatedWorld();

        // 5. 自我反思
        SelfReflector.ReflectionResult reflection = selfReflector.reflect(selfModel, fused, worldModel);

        // 6. 检查是否需要更新自我模型
        if (reflection.hasInsight()) {
            selfModel = selfReflector.applyInsight(selfModel, reflection);
        }

        // 7. 推理（使用 LLM 或启发式）
        ReasoningEngine.ReasoningResult reasoningResult = null;
        if (reasoningEngine != null) {
            List<String> recentActions = memory.getWorking().getAll().stream()
                .map(item -> item.content().toString())
                .toList();
            if (recentActions.size() > 5) {
                recentActions = recentActions.subList(recentActions.size() - 5, recentActions.size());
            }

            String mood = worldModel.owner().emotionalState() != null ?
                worldModel.owner().emotionalState().currentMood().name() : "平静";

            List<String> observations = memory.getSensory().getRecentStimuli().stream()
                .map(s -> s.content().toString())
                .toList();

            // 7a. 检索相关长期记忆
            WorldModel.Context worldContext = worldModel.currentContext();
            OwnerModel.Mood ownerMood = worldModel.owner().emotionalState() != null ?
                worldModel.owner().emotionalState().currentMood() : null;
            MemoryRetrievalService.RetrievalContext retrievalContext =
                memoryRetrievalService.retrieve(worldContext, ownerMood);
            String memoryHighlights = memoryRetrievalService.buildMemoryContextString(retrievalContext);

            ReasoningEngine.ReasoningContext reasoningContext = new ReasoningEngine.ReasoningContext(
                fused.generateFeelings().isEmpty() ? "未知情境" : fused.generateFeelings().get(0),
                new ArrayList<>(recentActions),
                mood,
                java.time.LocalDateTime.now().toString(),
                new ArrayList<>(observations),
                memoryHighlights
            );
            reasoningResult = reasoningEngine.reason(reasoningContext);
        }

        // 8. 存入记忆
        memory.perceive(new MemorySystem.Stimulus(
            UUID.randomUUID().toString(),
            MemorySystem.StimulusType.SYSTEM,
            fused,
            "cognition",
            Instant.now(),
            pipelineOutput.salienceScore().overall()
        ));

        // 9. 生成行动建议（使用决策引擎）
        DecisionEngine.DecisionResult decisionResult = decisionEngine.decide(
            reasoningResult,
            pipelineOutput.salienceScore(),
            selfModel
        );

        // 同时保留旧的 ActionRecommendation 用于日志
        ActionRecommendation actionRecommendation = generateActionRecommendation(
            fused,
            reflection,
            pipelineOutput.salienceScore(),
            reasoningResult
        );

        cycleCount++;
        lastCycleTime = Instant.now();

        return new CognitionResult(
            fused,
            selfModel,
            worldModel,
            reflection,
            reasoningResult,
            actionRecommendation,
            pipelineOutput.salienceScore(),
            pipelineOutput.isSignificant(),
            decisionResult
        );
    }

    private ActionRecommendation generateActionRecommendation(
        PerceptionSystem.Perception perception,
        SelfReflector.ReflectionResult reflection,
        PerceptionSystem.SalienceScore salienceScore,
        ReasoningEngine.ReasoningResult reasoningResult
    ) {
        List<String> recommendations = new ArrayList<>();
        int priority = 0;

        if (salienceScore.overall() > 0.7) {
            String feeling = perception.generateFeelings().isEmpty() ? "新情况" : perception.generateFeelings().get(0);
            recommendations.add("关注: " + feeling);
            priority = 1;
        }

        if (reflection.hasInsight()) {
            recommendations.add("反思: " + reflection.insight());
            priority = Math.max(priority, 2);
        }

        if (reasoningResult != null && reasoningResult.hasLlmSupport()) {
            for (ReasoningEngine.ReasoningOutput output : reasoningResult.outputs()) {
                switch (output.type()) {
                    case INTENT -> {
                        recommendations.add("意图推断: " + output.content());
                        priority = Math.max(priority, 2);
                        break;
                    }
                    case PREDICTION -> {
                        recommendations.add("预测: " + output.content());
                        break;
                    }
                    case CAUSAL -> {
                        recommendations.add("因果分析: " + output.content());
                        break;
                    }
                }
            }
        }

        if (worldModel.owner().emotionalState() != null) {
            OwnerModel.EmotionalState emotion = worldModel.owner().emotionalState();
            if (emotion.intensity() > 0.7) {
                recommendations.add("注意主人情绪: " + emotion.currentMood());
                priority = Math.max(priority, 1);
            }
        }

        StringBuilder reason = new StringBuilder();
        reason.append("基于感知显著性(").append(salienceScore.overall()).append(")");
        if (reflection.hasInsight()) reason.append("和反思洞察");
        if (reasoningResult != null && reasoningResult.hasLlmSupport()) reason.append("和LLM推理");

        return new ActionRecommendation(recommendations, priority, reason.toString());
    }

    /**
     * 获取当前状态
     */
    public CognitionState getCurrentState() {
        return new CognitionState(
            selfModel,
            worldModel,
            lastCycleTime,
            memory.getStatus()
        );
    }

    /**
     * 获取认知统计
     */
    public CognitionStats getStats() {
        MemorySystem.MemoryStats memStats = memory.getLongTerm().getStats();
        return new CognitionStats(
            cycleCount,
            perceptionPipeline.getAverageSalience(),
            selfReflector.getReflectionCount(),
            worldModel.knowledgeGraph().facts().size(),
            memStats.episodicCount() + memStats.semanticCount() + memStats.proceduralCount()
        );
    }

    /**
     * 更新自我模型（用于进化引擎应用）
     */
    public void updateSelfModel(SelfModel.Self newSelfModel) {
        if (newSelfModel != null) {
            logger.info("Updating self model: evolutionLevel={}, evolutionCount={}",
                    newSelfModel.evolutionLevel(), newSelfModel.evolutionCount());
            this.selfModel = newSelfModel;
        }
    }

    public record CognitionState(
        SelfModel.Self selfModel,
        WorldModel.World worldModel,
        Instant lastCycleTime,
        MemorySystem.MemoryStatus memoryStatus
    ) {}

    public record CognitionStats(
        int totalCycles,
        float avgSalience,
        int reflectionCount,
        int worldModelFacts,
        int longTermMemories
    ) {}

    public record ActionRecommendation(
        List<String> recommendations,
        int priority,
        String reason
    ) {}

    public record CognitionResult(
        PerceptionSystem.Perception perception,
        SelfModel.Self selfModel,
        WorldModel.World worldModel,
        SelfReflector.ReflectionResult reflection,
        ReasoningEngine.ReasoningResult reasoningResult,
        ActionRecommendation actionRecommendation,
        PerceptionSystem.SalienceScore salienceScore,
        boolean isSignificant,
        DecisionEngine.DecisionResult decisionResult
    ) {}
}
