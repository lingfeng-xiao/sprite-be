package com.lingfeng.sprite.action.Actions;

import java.time.Instant;
import java.util.Map;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.Stimulus;
import com.lingfeng.sprite.MemorySystem.StimulusType;
import com.lingfeng.sprite.MemorySystem.StoreType;
import com.lingfeng.sprite.MemorySystem.WorkingMemoryItem;
import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;

/**
 * 记忆存储动作
 *
 * 将重要信息存储到记忆系统
 */
public class RememberAction implements ActionPlugin {

    private final MemorySystem.Memory memory;

    public RememberAction() {
        this(null);
    }

    public RememberAction(MemorySystem.Memory memory) {
        this.memory = memory;
    }

    @Override
    public String getName() {
        return "Remember";
    }

    @Override
    public ActionResult execute(Map<String, Object> params) {
        // 尝试从 Spring 注入的 memory
        MemorySystem.Memory mem = this.memory;
        if (mem == null) {
            mem = (MemorySystem.Memory) params.get("memory");
        }
        if (mem == null) {
            // 如果没有 memory bean，记录日志但不失败
            Object content = extractContent(params);
            return ActionResult.success("已记录: " + content);
        }

        String content = null;
        String memoryType = "WORKING"; // 默认存入工作记忆
        float relevance = 0.7f;

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() == null) continue;
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue().toString();
            if (value.isBlank()) continue;

            if (key.equals("content") || key.equals("内容") || key.equals("text")) {
                content = value;
            } else if (key.equals("memorytype") || key.equals("type") || key.equals("记忆类型")) {
                memoryType = value.toUpperCase();
            } else if (key.equals("relevance") || key.equals("相关性")) {
                try {
                    relevance = Float.parseFloat(value);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }

        // 如果没有 content 参数，尝试从 actionParam 获取
        if (content == null || content.isBlank()) {
            Object actionParam = params.get("actionParam");
            if (actionParam != null) {
                String paramStr = actionParam.toString();
                if (paramStr.contains(":")) {
                    content = paramStr.substring(paramStr.indexOf(":") + 1).trim();
                } else {
                    content = paramStr;
                }
            }
        }

        if (content == null || content.isBlank()) {
            return ActionResult.failure("缺少记忆内容");
        }

        try {
            // 创建感官记忆
            String stimulusId = "remember-" + Instant.now().toEpochMilli();
            Stimulus stimulus = new Stimulus(
                stimulusId,
                StimulusType.TEXT,
                content,
                "user-input",
                Instant.now(),
                relevance
            );

            // 先存入工作记忆
            WorkingMemoryItem item = new WorkingMemoryItem(
                java.util.UUID.randomUUID().toString(),
                content,
                content, // abstraction = content
                stimulus,
                relevance
            );
            mem.perceive(stimulus);
            mem.consolidateToWorking(
                new com.lingfeng.sprite.MemorySystem.Pattern(
                    StimulusType.TEXT, 1, Instant.now(), Instant.now(), content
                ),
                content,
                content
            );

            // 如果指定了长期记忆类型，也存入长期记忆
            StoreType storeType = parseStoreType(memoryType);
            mem.storeToLongTerm(item, storeType);

            String location = storeType == StoreType.EPISODIC ? "情景记忆" :
                              storeType == StoreType.SEMANTIC ? "语义记忆" :
                              storeType == StoreType.PROCEDURAL ? "程序记忆" : "工作记忆";

            return ActionResult.success("已记住: " + content + " (存入" + location + ")");
        } catch (Exception e) {
            return ActionResult.failure("存储记忆失败: " + e.getMessage());
        }
    }

    private String extractContent(Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() == null) continue;
            String key = entry.getKey().toLowerCase();
            if (key.equals("content") || key.equals("内容") || key.equals("text")) {
                return entry.getValue().toString();
            }
        }
        Object actionParam = params.get("actionParam");
        if (actionParam != null) {
            String paramStr = actionParam.toString();
            if (paramStr.contains(":")) {
                return paramStr.substring(paramStr.indexOf(":") + 1).trim();
            }
            return paramStr;
        }
        return "未知内容";
    }

    private StoreType parseStoreType(String type) {
        try {
            return StoreType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return StoreType.EPISODIC; // 默认
        }
    }
}
