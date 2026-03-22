package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.WorldModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 推理引擎 - 意图识别、因果推理、预测
 *
 * ## 架构设计
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │                     ReasoningEngine                          │
 * │              (感知 → 推理 → 意图/因果/预测)                 │
 * ├─────────────────────────────────────────────────────────────┤
 * │                                                             │
 * │   感知输入                                                   │
 * │      │                                                       │
 * │      ▼                                                      │
 * │   ┌──────────────────────────────────────┐                  │
 * │   │         LlmReasoner 接口             │ ← 预留LLM接口   │
 * │   │    (Claude/OpenAI/本地模型)         │                  │
 * │   └──────────────┬───────────────────────┘                  │
 * │                  │                                           │
 * │     ┌────────────┼────────────┐                             │
 * │     ▼            ▼            ▼                              │
 * │  ┌──────┐  ┌────────┐  ┌─────────┐                        │
 * │  │意图   │  │因果     │  │预测     │                        │
 * │  │识别   │  │推理     │  │         │                        │
 * │  └──────┘  └────────┘  └─────────┘                        │
 * │                                                             │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ## 核心能力
 *
 * 1. **意图识别**：从主人行为中推断真实意图
 * 2. **因果推理**：理解"为什么"发生
 * 3. **预测**：基于历史预测下一步
 */
public class ReasoningEngine {

    private final LlmReasoner llmReasoner;
    private final ExecutorService executor;

    public ReasoningEngine(LlmReasoner llmReasoner) {
        this.llmReasoner = llmReasoner;
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * 推理入口
     */
    public ReasoningResult reason(ReasoningContext context) {
        List<ReasoningOutput> results = new ArrayList<>();

        if (llmReasoner != null) {
            // 使用 LLM 进行推理
            try {
                Intent intent = llmReasoner.inferIntent(new IntentPrompt(
                    context.situation(),
                    context.recentActions(),
                    context.ownerMood(),
                    context.timeContext()
                )).get();

                results.add(new ReasoningOutput(
                    ReasoningType.INTENT,
                    intent.description(),
                    intent.confidence(),
                    "基于 " + context.recentActions().size() + " 个近期行为和主人情绪推断"
                ));
            } catch (Exception e) {
                results.add(new ReasoningOutput(
                    ReasoningType.INTENT,
                    "无法推断意图",
                    0f,
                    "LLM调用失败: " + e.getMessage()
                ));
            }

            try {
                CausalChain causalChain = llmReasoner.reasonCausal(new CausalPrompt(
                    context.situation(),
                    context.observations()
                )).get();

                results.add(new ReasoningOutput(
                    ReasoningType.CAUSAL,
                    causalChain.summary(),
                    causalChain.confidence(),
                    "因果链: " + String.join(" → ", causalChain.steps())
                ));
            } catch (Exception e) {
                results.add(new ReasoningOutput(
                    ReasoningType.CAUSAL,
                    "无法进行因果推理",
                    0f,
                    "LLM调用失败: " + e.getMessage()
                ));
            }

            try {
                Prediction prediction = llmReasoner.predict(new PredictionPrompt(
                    context.situation(),
                    context.recentActions()
                )).get();

                results.add(new ReasoningOutput(
                    ReasoningType.PREDICTION,
                    prediction.description(),
                    prediction.confidence(),
                    "基于 " + context.recentActions().size() + " 个历史行为预测"
                ));
            } catch (Exception e) {
                results.add(new ReasoningOutput(
                    ReasoningType.PREDICTION,
                    "无法进行预测",
                    0f,
                    "LLM调用失败: " + e.getMessage()
                ));
            }
        } else {
            // 使用启发式推理
            results.add(heuristicReasoning(context));
        }

        return new ReasoningResult(
            results,
            llmReasoner != null ? 0.8f : 0.5f,
            llmReasoner != null
        );
    }

    /**
     * 启发式推理（无 LLM 时使用）
     */
    private ReasoningOutput heuristicReasoning(ReasoningContext context) {
        StringBuilder content = new StringBuilder();
        content.append("基于当前情境(").append(context.situation()).append(")");
        content.append("和主人情绪(").append(context.ownerMood()).append(")");
        content.append("，主人可能想要");

        String intent;
        if (context.ownerMood().toLowerCase().contains("焦虑")) {
            intent = "解决某个问题或得到安慰";
        } else if (context.ownerMood().toLowerCase().contains("开心")) {
            intent = "分享喜悦或继续当前活动";
        } else if (context.recentActions().size() > 5) {
            intent = "继续当前的模式化行为";
        } else {
            intent = "进行某种互动";
        }
        content.append(intent);

        return new ReasoningOutput(
            ReasoningType.INTENT,
            content.toString(),
            0.4f,
            "启发式推理（非LLM）"
        );
    }

    /**
     * 获取推理类型
     */
    public enum ReasoningType {
        INTENT,
        CAUSAL,
        PREDICTION
    }

    /**
     * 推理上下文
     */
    public record ReasoningContext(
        String situation,
        List<String> recentActions,
        String ownerMood,
        String timeContext,
        List<String> observations
    ) {
        public ReasoningContext {
            recentActions = recentActions != null ? List.copyOf(recentActions) : List.of();
            observations = observations != null ? List.copyOf(observations) : List.of();
        }
    }

    /**
     * 推理结果
     */
    public record ReasoningResult(
        List<ReasoningOutput> outputs,
        float confidence,
        boolean hasLlmSupport
    ) {
        public ReasoningResult {
            outputs = outputs != null ? List.copyOf(outputs) : List.of();
        }
    }

    /**
     * 推理输出
     */
    public record ReasoningOutput(
        ReasoningType type,
        String content,
        float confidence,
        String reasoning
    ) {}

    // ==================== LLM 接口和实现 ====================

    /**
     * LLM 推理接口 - 抽象化，不绑定具体实现
     *
     * 实现可以是：Claude API、OpenAI、本地模型等
     */
    public interface LlmReasoner {
        CompletableFuture<Intent> inferIntent(IntentPrompt prompt);
        CompletableFuture<CausalChain> reasonCausal(CausalPrompt prompt);
        CompletableFuture<Prediction> predict(PredictionPrompt prompt);
        CompletableFuture<Insight> reflect(ReflectionPrompt prompt);
    }

    // ==================== 推理数据类型 ====================

    public record IntentPrompt(
        String situation,
        List<String> recentActions,
        String ownerMood,
        String timeContext
    ) {}

    public record Intent(
        String description,
        float confidence,
        List<Intent> alternatives
    ) {
        public Intent {
            alternatives = alternatives != null ? List.copyOf(alternatives) : List.of();
        }

        public Intent(String description, float confidence) {
            this(description, confidence, List.of());
        }
    }

    public record CausalPrompt(
        String event,
        List<String> observations
    ) {}

    public record CausalChain(
        String summary,
        List<String> steps,
        float confidence
    ) {
        public CausalChain {
            steps = steps != null ? List.copyOf(steps) : List.of();
        }
    }

    public record PredictionPrompt(
        String currentState,
        List<String> history
    ) {}

    public record Prediction(
        String description,
        float confidence,
        String timeframe
    ) {
        public Prediction {
            timeframe = timeframe != null ? timeframe : "";
        }

        public Prediction(String description, float confidence) {
            this(description, confidence, "");
        }
    }

    public record ReflectionPrompt(
        String situation,
        String selfState,
        List<String> recentExperiences
    ) {}

    public record Insight(
        String content,
        float importance,
        String category
    ) {}
}
