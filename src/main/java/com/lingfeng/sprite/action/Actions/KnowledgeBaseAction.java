package com.lingfeng.sprite.action.Actions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;

/**
 * S7-3: 知识库动作插件
 *
 * 查询知识库获取相关信息
 *
 * 参数:
 * - query: 查询文本
 * - topK: 返回的最相似结果数量 (默认3)
 * - threshold: 相似度阈值 (默认0.7)
 */
public class KnowledgeBaseAction implements ActionPlugin {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseAction.class);

    // 知识库存储 (实际实现时应该连接到向量数据库或知识图谱)
    private static final Map<String, KnowledgeEntry> knowledgeBase = new ConcurrentHashMap<>();

    static {
        // 初始化一些示例知识条目
        initializeSampleKnowledge();
    }

    /**
     * 知识库条目
     */
    private static class KnowledgeEntry {
        final String id;
        final String question;
        final String answer;
        final List<String> tags;
        final long timestamp;

        KnowledgeEntry(String id, String question, String answer, List<String> tags) {
            this.id = id;
            this.question = question;
            this.answer = answer;
            this.tags = tags;
            this.timestamp = System.currentTimeMillis();
        }
    }

    @Override
    public String getName() {
        return "KnowledgeBaseAction";
    }

    @Override
    public ActionResult execute(Map<String, Object> params) {
        try {
            String actionParam = (String) params.get("actionParam");
            Instant timestamp = (Instant) params.get("timestamp");

            // 解析参数
            String query = actionParam != null ? actionParam : getString(params, "query", "");
            int topK = getInt(params, "topK", 3);
            float threshold = getFloat(params, "threshold", 0.7f);

            if (query == null || query.isEmpty()) {
                return ActionResult.failure("Query cannot be empty");
            }

            logger.info("=== Knowledge Base Query ===");
            logger.info("Time: {}", timestamp);
            logger.info("Query: {}", query);
            logger.info("TopK: {}, Threshold: {}", topK, threshold);
            logger.info("===========================");

            // 执行相似度搜索
            List<QueryResult> results = searchKnowledgeBase(query, topK, threshold);

            if (results.isEmpty()) {
                return ActionResult.success("No relevant knowledge found for query: " + query);
            }

            // 构建结果
            StringBuilder response = new StringBuilder();
            response.append("Found ").append(results.size()).append(" relevant knowledge entries:\n\n");

            for (int i = 0; i < results.size(); i++) {
                QueryResult result = results.get(i);
                response.append("【").append(i + 1).append("】").append(result.entry.question).append("\n");
                response.append("   答案: ").append(result.entry.answer).append("\n");
                response.append("   相似度: ").append(String.format("%.2f", result.similarity * 100)).append("%\n");
                if (result.entry.tags != null && !result.entry.tags.isEmpty()) {
                    response.append("   标签: ").append(String.join(", ", result.entry.tags)).append("\n");
                }
                response.append("\n");
            }

            return ActionResult.success(response.toString(), results);

        } catch (Exception e) {
            logger.error("KnowledgeBaseAction failed: {}", e.getMessage());
            return ActionResult.failure("KnowledgeBaseAction failed: " + e.getMessage());
        }
    }

    /**
     * 搜索知识库
     */
    private List<QueryResult> searchKnowledgeBase(String query, int topK, float threshold) {
        List<QueryResult> results = new ArrayList<>();

        for (KnowledgeEntry entry : knowledgeBase.values()) {
            float similarity = calculateSimilarity(query, entry.question);

            if (similarity >= threshold) {
                results.add(new QueryResult(entry, similarity));
            }
        }

        // 按相似度排序并取前topK
        results.sort(Comparator.comparingDouble((QueryResult r) -> r.similarity).reversed());
        if (results.size() > topK) {
            results = results.subList(0, topK);
        }

        return results;
    }

    /**
     * 计算文本相似度 (简单的词袋模型 + 余弦相似度)
     * 实际实现时应该使用向量嵌入 + 余弦相似度
     */
    private float calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0f;
        if (text1.isEmpty() && text2.isEmpty()) return 1f;
        if (text1.isEmpty() || text2.isEmpty()) return 0f;

        // 简单的词袋模型相似度
        List<String> words1 = List.of(text1.toLowerCase().split("[\\s,.!?;:]+"));
        List<String> words2 = List.of(text2.toLowerCase().split("[\\s,.!?;:]+"));

        // 计算词集合
        java.util.Set<String> set1 = new java.util.HashSet<>(words1);
        java.util.Set<String> set2 = new java.util.HashSet<>(words2);

        // 计算交集
        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);

        // 计算并集
        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);

        // Jaccard相似度
        if (union.isEmpty()) return 0f;
        return (float) intersection.size() / union.size();
    }

    /**
     * 添加知识条目到知识库
     */
    public static void addKnowledge(String question, String answer, List<String> tags) {
        String id = java.util.UUID.randomUUID().toString();
        knowledgeBase.put(id, new KnowledgeEntry(id, question, answer, tags));
    }

    /**
     * 添加知识条目 (简化版本)
     */
    public static void addKnowledge(String question, String answer) {
        addKnowledge(question, answer, List.of());
    }

    /**
     * 清空知识库
     */
    public static void clearKnowledgeBase() {
        knowledgeBase.clear();
    }

    /**
     * 初始化示例知识
     */
    private static void initializeSampleKnowledge() {
        addKnowledge(
            "Sprite是什么？",
            "Sprite是一个数字生命项目，旨在创建一个具有感知、认知和行动能力的智能助手。",
            List.of("项目介绍", "数字生命")
        );

        addKnowledge(
            "如何配置SMTP邮件发送？",
            "在EmailAction中配置SMTP_HOST、SMTP_PORT、USERNAME、PASSWORD等参数即可。",
            List.of("配置", "邮件", "SMTP")
        );

        addKnowledge(
            "如何添加新的动作插件？",
            "实现ActionPlugin接口，创建xxxAction类，然后在ActionExecutor中注册即可。",
            List.of("开发", "动作插件")
        );

        addKnowledge(
            "决策引擎如何工作？",
            "决策引擎接收推理结果和上下文，综合考虑显著性、情绪、时间、记忆等多维度评分，生成可执行的动作。",
            List.of("决策", "认知引擎")
        );

        addKnowledge(
            "记忆系统如何工作？",
            "记忆系统分为感觉记忆、工作记忆和长期记忆，通过检索和存储机制实现上下文感知。",
            List.of("记忆", "认知系统")
        );
    }

    /**
     * 查询结果
     */
    private record QueryResult(KnowledgeEntry entry, float similarity) {}

    /**
     * 从参数中获取字符串
     */
    private String getString(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    /**
     * 从参数中获取整数
     */
    private int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 从参数中获取浮点数
     */
    private float getFloat(Map<String, Object> params, String key, float defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
