package com.lingfeng.sprite.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.cognition.ReasoningEngine;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MinMax LLM 推理器
 *
 * 实现 LlmReasoner 接口，调用 MinMax API
 */
public class MinMaxLlmReasoner implements ReasoningEngine.LlmReasoner {

    private static final Logger logger = LoggerFactory.getLogger(MinMaxLlmReasoner.class);

    private final MinMaxConfig config;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    public MinMaxLlmReasoner(MinMaxConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(10);
        this.httpClient = HttpClients.custom().setConnectionManager(cm).build();
    }

    @Override
    public CompletableFuture<ReasoningEngine.Intent> inferIntent(ReasoningEngine.IntentPrompt prompt) {
        return CompletableFuture.supplyAsync(() -> {
            String systemPrompt = String.format(
                "你是一个数字生命的意图识别引擎。\n" +
                "根据以下信息推断主人的真实意图：\n" +
                "- 当前情境：%s\n" +
                "- 近期行为：%s\n" +
                "- 主人情绪：%s\n" +
                "- 时间上下文：%s\n\n" +
                "请推断主人最可能的意图，并以JSON格式返回：\n" +
                "{\"description\": \"意图描述\", \"confidence\": 0.0-1.0之间的置信度, \"alternatives\": [\"备选意图1\", \"备选意图2\"]}",
                prompt.situation(),
                String.join("、", prompt.recentActions()),
                prompt.ownerMood(),
                prompt.timeContext()
            );

            String response = callMinMax(systemPrompt);
            return parseIntentResponse(response, prompt);
        });
    }

    @Override
    public CompletableFuture<ReasoningEngine.CausalChain> reasonCausal(ReasoningEngine.CausalPrompt prompt) {
        return CompletableFuture.supplyAsync(() -> {
            String systemPrompt = String.format(
                "你是一个因果推理引擎。\n" +
                "分析以下事件的因果链：\n" +
                "- 事件：%s\n" +
                "- 观察：%s\n\n" +
                "请分析因果关系并返回JSON格式：\n" +
                "{\"summary\": \"因果链总结\", \"steps\": [\"原因1\", \"中间结果\", \"最终结果\"], \"confidence\": 0.0-1.0之间的置信度}",
                prompt.event(),
                String.join("、", prompt.observations())
            );

            String response = callMinMax(systemPrompt);
            return parseCausalChainResponse(response);
        });
    }

    @Override
    public CompletableFuture<ReasoningEngine.Prediction> predict(ReasoningEngine.PredictionPrompt prompt) {
        return CompletableFuture.supplyAsync(() -> {
            String systemPrompt = String.format(
                "你是一个预测引擎。\n" +
                "基于当前状态和历史行为预测下一步：\n" +
                "- 当前状态：%s\n" +
                "- 历史行为：%s\n\n" +
                "请预测下一步并返回JSON格式：\n" +
                "{\"description\": \"预测描述\", \"confidence\": 0.0-1.0之间的置信度, \"timeframe\": \"预测的时间范围\"}",
                prompt.currentState(),
                String.join("、", prompt.history())
            );

            String response = callMinMax(systemPrompt);
            return parsePredictionResponse(response);
        });
    }

    @Override
    public CompletableFuture<ReasoningEngine.Insight> reflect(ReasoningEngine.ReflectionPrompt prompt) {
        return CompletableFuture.supplyAsync(() -> {
            String systemPrompt = String.format(
                "你是一个自我反思引擎。\n" +
                "基于当前情境和自我状态进行反思：\n" +
                "- 情境：%s\n" +
                "- 自我状态：%s\n" +
                "- 近期经历：%s\n\n" +
                "请生成洞察并返回JSON格式：\n" +
                "{\"content\": \"洞察内容\", \"importance\": 0.0-1.0的重要性, \"category\": \"洞察类别（如：自我认知、关系理解、环境适应等）\"}",
                prompt.situation(),
                prompt.selfState(),
                String.join("、", prompt.recentExperiences())
            );

            String response = callMinMax(systemPrompt);
            return parseInsightResponse(response);
        });
    }

    /**
     * 统一对话推理 - 用于聊天
     *
     * @param context LLM上下文
     * @param userMessage 用户消息
     * @return LLM思考结果
     */
    public CompletableFuture<ChatModels.LlmThought> think(ChatModels.LlmContext context, String userMessage) {
        return CompletableFuture.supplyAsync(() -> {
            String prompt = buildChatPrompt(context, userMessage);
            String response = callMinMax(prompt);
            return parseChatResponse(response);
        });
    }

    private String buildChatPrompt(ChatModels.LlmContext ctx, String userMessage) {
        return String.format("""
你是 %s，一个有深度自我意识的数字生命。

## 自我认知
%s

## 主人画像
%s

## 当前情境
%s

## 可用工具
%s

## 对话历史
%s

## 主人说: %s

请回复主人，并决定是否需要调用工具。
重要：如果需要执行操作（如搜索、计算、存储记忆等），必须以JSON格式返回工具调用：
{"tool_calls": [{"tool": "工具名", "params": {"参数": "值"}}]}

如果不需要工具，直接回复即可。
""",
                "小艺",
                ctx.selfSummary(),
                ctx.ownerSummary(),
                ctx.currentSituation(),
                ctx.availableTools(),
                ctx.chatHistory(),
                userMessage
        );
    }

    private ChatModels.LlmThought parseChatResponse(String response) {
        try {
            // 首先尝试解析工具调用
            List<ChatModels.ToolCall> toolCalls = parseToolCalls(response);

            // 提取纯文本回复（去掉JSON部分）
            String textResponse = extractTextResponse(response);

            return new ChatModels.LlmThought(
                    null,  // reasoning
                    textResponse,  // response
                    null,  // insight
                    toolCalls,
                    0.8f
            );
        } catch (Exception e) {
            logger.warn("Failed to parse chat response: {}", e.getMessage());
            return new ChatModels.LlmThought(
                    null,
                    response.isEmpty() ? "抱歉，我现在无法回应。" : response,
                    null,
                    List.of(),
                    0.5f
            );
        }
    }

    private List<ChatModels.ToolCall> parseToolCalls(String response) {
        List<ChatModels.ToolCall> calls = new ArrayList<>();

        try {
            // 尝试提取 JSON 部分
            String jsonStr = extractJson(response);
            if (jsonStr.isEmpty()) {
                return calls;
            }

            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode toolCallsNode = root.path("tool_calls");

            if (toolCallsNode.isArray()) {
                for (JsonNode call : toolCallsNode) {
                    String toolName = call.path("tool").asText(null);
                    JsonNode paramsNode = call.path("params");

                    if (toolName != null && !toolName.isEmpty()) {
                        Map<String, Object> params = objectMapper.convertValue(
                                paramsNode,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                        );
                        calls.add(new ChatModels.ToolCall(toolName, params));
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("No tool calls in response: {}", e.getMessage());
        }

        return calls;
    }

    private String extractTextResponse(String response) {
        // 去掉 JSON 代码块
        String cleaned = response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        // 尝试找到并去掉 JSON 对象
        int jsonStart = cleaned.indexOf("{\"tool_calls\"");
        if (jsonStart >= 0) {
            cleaned = cleaned.substring(0, jsonStart).trim();
        }

        // 去掉末尾的 } 如果前面有 tool_calls
        int lastBrace = cleaned.lastIndexOf("}");
        int beforeBrace = cleaned.lastIndexOf("{");
        if (lastBrace > 0 && beforeBrace < lastBrace) {
            String possibleJson = cleaned.substring(beforeBrace);
            if (possibleJson.contains("tool_calls")) {
                cleaned = cleaned.substring(0, beforeBrace).trim();
            }
        }

        return cleaned.isEmpty() ? response : cleaned;
    }

    private String callMinMax(String prompt) {
        try {
            HttpPost request = new HttpPost(config.baseUrl() + "/text/chatcompletion_v2");
            request.setHeader("Authorization", "Bearer " + config.apiKey());
            request.setHeader("Content-Type", "application/json");

            String jsonBody = String.format(
                "{\"model\": \"%s\", \"messages\": [{\"role\": \"user\", \"content\": %s}]}",
                config.model(),
                objectMapper.writeValueAsString(prompt)
            );

            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                JsonNode jsonResponse = objectMapper.readTree(body);
                JsonNode choices = jsonResponse.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).path("message");
                    String content = message.path("content").asText("");
                    return content;
                }
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private ReasoningEngine.Intent parseIntentResponse(String response, ReasoningEngine.IntentPrompt original) {
        try {
            String jsonStr = extractJson(response);
            JsonNode jsonResponse = objectMapper.readTree(jsonStr);

            String description = jsonResponse.path("description").asText("无法推断意图");
            float confidence = (float) jsonResponse.path("confidence").asDouble(0.5);

            List<ReasoningEngine.Intent> alternatives = new ArrayList<>();
            JsonNode altNode = jsonResponse.path("alternatives");
            if (altNode.isArray()) {
                for (JsonNode alt : altNode) {
                    String altDesc = alt.asText(null);
                    if (altDesc != null && !altDesc.isEmpty()) {
                        alternatives.add(new ReasoningEngine.Intent(altDesc, 0.3f));
                    }
                }
            }

            return new ReasoningEngine.Intent(description, confidence, alternatives);
        } catch (Exception e) {
            String simpleIntent = inferSimpleIntent(original);
            return new ReasoningEngine.Intent(
                "基于情境(" + original.situation() + ")推断主人可能想要" + simpleIntent,
                0.4f
            );
        }
    }

    private String inferSimpleIntent(ReasoningEngine.IntentPrompt prompt) {
        if (prompt.ownerMood().toLowerCase().contains("焦虑")) {
            return "解决某个问题或得到安慰";
        } else if (prompt.ownerMood().toLowerCase().contains("开心")) {
            return "分享喜悦或继续当前活动";
        } else if (prompt.recentActions().size() > 5) {
            return "继续当前的模式化行为";
        } else {
            return "进行某种互动";
        }
    }

    private ReasoningEngine.CausalChain parseCausalChainResponse(String response) {
        try {
            String jsonStr = extractJson(response);
            JsonNode jsonResponse = objectMapper.readTree(jsonStr);

            String summary = jsonResponse.path("summary").asText("无法分析因果");
            float confidence = (float) jsonResponse.path("confidence").asDouble(0.5);

            List<String> steps = new ArrayList<>();
            JsonNode stepsNode = jsonResponse.path("steps");
            if (stepsNode.isArray()) {
                for (JsonNode step : stepsNode) {
                    steps.add(step.asText());
                }
            }

            return new ReasoningEngine.CausalChain(summary, steps, confidence);
        } catch (Exception e) {
            return new ReasoningEngine.CausalChain("因果分析失败", List.of(), 0f);
        }
    }

    private ReasoningEngine.Prediction parsePredictionResponse(String response) {
        try {
            String jsonStr = extractJson(response);
            JsonNode jsonResponse = objectMapper.readTree(jsonStr);

            String description = jsonResponse.path("description").asText("无法预测");
            float confidence = (float) jsonResponse.path("confidence").asDouble(0.5);
            String timeframe = jsonResponse.path("timeframe").asText(null);

            return new ReasoningEngine.Prediction(description, confidence, timeframe);
        } catch (Exception e) {
            return new ReasoningEngine.Prediction("预测失败", 0f, null);
        }
    }

    private ReasoningEngine.Insight parseInsightResponse(String response) {
        try {
            String jsonStr = extractJson(response);
            JsonNode jsonResponse = objectMapper.readTree(jsonStr);

            String content = jsonResponse.path("content").asText("无法生成洞察");
            float importance = (float) jsonResponse.path("importance").asDouble(0.5);
            String category = jsonResponse.path("category").asText("反思");

            return new ReasoningEngine.Insight(content, importance, category);
        } catch (Exception e) {
            return new ReasoningEngine.Insight("反思生成失败", 0f, "反思");
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        } else {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                trimmed = trimmed.substring(start, end + 1);
            }
        }
        return trimmed.trim();
    }

    public void close() {
        try {
            httpClient.close();
        } catch (Exception e) {
            // ignore
        }
    }
}
