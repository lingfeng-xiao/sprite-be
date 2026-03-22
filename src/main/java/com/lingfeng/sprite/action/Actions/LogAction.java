package com.lingfeng.sprite.action.Actions;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;
import com.lingfeng.sprite.cognition.SelfReflector;

/**
 * 日志动作 - 将感知结果记录到日志
 */
public class LogAction implements ActionPlugin {

    private static final Logger logger = LoggerFactory.getLogger(LogAction.class);

    @Override
    public String getName() {
        return "LogAction";
    }

    @Override
    public ActionResult execute(Map<String, Object> params) {
        try {
            String actionParam = (String) params.get("actionParam");
            PerceptionSystem.Perception perception = (PerceptionSystem.Perception) params.get("perception");
            SelfReflector.ReflectionResult reflection = (SelfReflector.ReflectionResult) params.get("reflection");
            Instant timestamp = (Instant) params.get("timestamp");

            StringBuilder logMessage = new StringBuilder();
            logMessage.append("=== Sprite Action Log ===\n");
            logMessage.append("Time: ").append(timestamp).append("\n");
            logMessage.append("Action: ").append(actionParam).append("\n");

            if (perception != null) {
                logMessage.append("\n--- Perception ---\n");
                logMessage.append("Feelings: ").append(perception.generateFeelings()).append("\n");
                if (perception.platform() != null) {
                    logMessage.append("Platform: ").append(perception.platform().hostname()).append("\n");
                }
            }

            if (reflection != null) {
                logMessage.append("\n--- Reflection ---\n");
                logMessage.append("Has Insight: ").append(reflection.hasInsight()).append("\n");
                if (reflection.hasInsight()) {
                    logMessage.append("Insight: ").append(reflection.insight()).append("\n");
                }
            }

            logger.info(logMessage.toString());

            return ActionResult.success("Logged: " + actionParam);
        } catch (Exception e) {
            logger.error("LogAction failed: {}", e.getMessage());
            return ActionResult.failure("LogAction failed: " + e.getMessage());
        }
    }
}
