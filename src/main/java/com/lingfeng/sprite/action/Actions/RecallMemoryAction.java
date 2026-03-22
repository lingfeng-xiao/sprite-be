package com.lingfeng.sprite.action.Actions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.Memory;
import com.lingfeng.sprite.MemorySystem.RecallResult;
import com.lingfeng.sprite.MemorySystem.WorkingMemoryItem;
import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;

/**
 * 记忆检索动作
 *
 * 从记忆系统搜索相关内容
 */
public class RecallMemoryAction implements ActionPlugin {

    private final Memory memory;

    public RecallMemoryAction() {
        this(null);
    }

    public RecallMemoryAction(Memory memory) {
        this.memory = memory;
    }

    @Override
    public String getName() {
        return "RecallMemory";
    }

    @Override
    public ActionResult execute(Map<String, Object> params) {
        // 尝试从 Spring 注入的 memory
        Memory mem = this.memory;
        if (mem == null) {
            mem = (Memory) params.get("memory");
        }
        if (mem == null) {
            return ActionResult.failure("记忆系统不可用");
        }

        String query = null;

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() == null) continue;
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue().toString();
            if (value.isBlank()) continue;

            if (key.equals("query") || key.equals("搜索") || key.equals("关键词")) {
                query = value;
            }
        }

        // 如果没有 query 参数，尝试从 actionParam 获取
        if (query == null || query.isBlank()) {
            Object actionParam = params.get("actionParam");
            if (actionParam != null) {
                String paramStr = actionParam.toString();
                if (paramStr.contains(":")) {
                    query = paramStr.substring(paramStr.indexOf(":") + 1).trim();
                } else {
                    query = paramStr;
                }
            }
        }

        if (query == null || query.isBlank()) {
            return ActionResult.failure("缺少搜索关键词");
        }

        try {
            RecallResult result = mem.recall(query);

            StringBuilder sb = new StringBuilder();

            // 工作记忆
            List<WorkingMemoryItem> working = result.workingItems();
            if (!working.isEmpty()) {
                sb.append("【工作记忆】\n");
                for (WorkingMemoryItem item : working.stream().limit(5).toList()) {
                    sb.append("- ").append(item.content()).append("\n");
                }
            }

            // 情景记忆
            var episodic = result.episodic();
            if (!episodic.isEmpty()) {
                sb.append("【情景记忆】\n");
                for (var entry : episodic.stream().limit(5).toList()) {
                    sb.append("- [").append(entry.timestamp()).append("] ")
                      .append(entry.experience());
                    if (entry.lesson() != null) {
                        sb.append(" → ").append(entry.lesson());
                    }
                    sb.append("\n");
                }
            }

            // 语义记忆
            var semantic = result.semantic();
            if (!semantic.isEmpty()) {
                sb.append("【语义记忆】\n");
                for (var entry : semantic.stream().limit(5).toList()) {
                    sb.append("- ").append(entry.concept()).append(": ")
                      .append(entry.definition()).append("\n");
                }
            }

            // 程序记忆 - 从 LongTermMemory 单独获取
            var procedural = mem.getLongTerm().recallProcedural(query);
            if (procedural != null) {
                sb.append("【程序记忆】\n");
                sb.append("- ").append(procedural.skillName()).append(": ")
                  .append(procedural.procedure()).append("\n");
            }

            if (sb.isEmpty()) {
                return ActionResult.success("未找到与 '" + query + "' 相关的记忆");
            }

            sb.insert(0, "搜索 '" + query + "' 的结果:\n\n");
            return ActionResult.success(sb.toString());
        } catch (Exception e) {
            return ActionResult.failure("检索记忆失败: " + e.getMessage());
        }
    }
}
