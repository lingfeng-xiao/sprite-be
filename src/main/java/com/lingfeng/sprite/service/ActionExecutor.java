package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;
import com.lingfeng.sprite.action.Actions.LogAction;
import com.lingfeng.sprite.action.Actions.NotifyAction;

/**
 * 动作执行器
 *
 * 负责执行认知循环产生的动作建议
 */
@Service
public class ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ActionExecutor.class);

    private final Map<String, ActionPlugin> actionPlugins;

    public ActionExecutor() {
        // 注册内置动作
        this.actionPlugins = Map.of(
                "LogAction", new LogAction(),
                "NotifyAction", new NotifyAction()
        );
        logger.info("ActionExecutor initialized with {} plugins", actionPlugins.size());
    }

    /**
     * 执行动作
     *
     * @param actionString 动作描述字符串
     * @param context 执行上下文
     * @return 执行结果
     */
    public ActionResult execute(String actionString, Map<String, Object> context) {
        if (actionString == null || actionString.isBlank()) {
            return ActionResult.failure("Empty action string");
        }

        // 解析动作类型
        String actionType = parseActionType(actionString);
        String actionParam = parseActionParam(actionString);

        ActionPlugin plugin = actionPlugins.get(actionType);
        if (plugin == null) {
            // 如果没有找到插件，记录日志动作
            logger.debug("No plugin for action type '{}', using LogAction", actionType);
            plugin = actionPlugins.get("LogAction");
            actionParam = actionString;
        }

        try {
            Map<String, Object> params = buildParams(actionParam, context);
            ActionResult result = plugin.execute(params);
            logger.debug("Action '{}' executed: {}", actionType, result.success() ? "success" : "failure");
            return result;
        } catch (Exception e) {
            logger.error("Error executing action '{}': {}", actionType, e.getMessage());
            return ActionResult.failure(e.getMessage());
        }
    }

    /**
     * 从动作字符串解析动作类型
     */
    private String parseActionType(String actionString) {
        // 格式: "意图推断: xxx" -> "意图推断"
        // 或 "关注: xxx" -> "关注"
        int colonIndex = actionString.indexOf(':');
        if (colonIndex > 0) {
            return actionString.substring(0, colonIndex).trim();
        }
        return actionString.split(" ")[0];
    }

    /**
     * 从动作字符串解析动作参数
     */
    private String parseActionParam(String actionString) {
        int colonIndex = actionString.indexOf(':');
        if (colonIndex > 0 && colonIndex < actionString.length() - 1) {
            return actionString.substring(colonIndex + 1).trim();
        }
        return actionString;
    }

    /**
     * 构建动作参数
     */
    private Map<String, Object> buildParams(String actionParam, Map<String, Object> context) {
        java.util.HashMap<String, Object> params = new java.util.HashMap<>(context);
        params.put("actionParam", actionParam);
        params.put("timestamp", Instant.now());
        return params;
    }

    /**
     * 注册动作插件
     */
    public void registerPlugin(String name, ActionPlugin plugin) {
        actionPlugins.put(name, plugin);
        logger.info("Registered action plugin: {}", name);
    }

    /**
     * 执行工具（按名称）
     *
     * @param toolName 工具名称
     * @param params 参数
     * @return 执行结果
     */
    public ActionResult executeTool(String toolName, Map<String, Object> params) {
        if (toolName == null || toolName.isBlank()) {
            return ActionResult.failure("Empty tool name");
        }

        ActionPlugin plugin = actionPlugins.get(toolName);
        if (plugin == null) {
            // 尝试模糊匹配
            for (String key : actionPlugins.keySet()) {
                if (key.toLowerCase().contains(toolName.toLowerCase())) {
                    plugin = actionPlugins.get(key);
                    break;
                }
            }
        }

        if (plugin == null) {
            logger.warn("No plugin found for tool: {}", toolName);
            return ActionResult.failure("未知工具: " + toolName);
        }

        try {
            Map<String, Object> fullParams = new java.util.HashMap<>(params);
            fullParams.put("timestamp", Instant.now());
            ActionResult result = plugin.execute(fullParams);
            logger.debug("Tool '{}' executed: {}", toolName, result.success() ? "success" : "failure");
            return result;
        } catch (Exception e) {
            logger.error("Error executing tool '{}': {}", toolName, e.getMessage());
            return ActionResult.failure(e.getMessage());
        }
    }
}
