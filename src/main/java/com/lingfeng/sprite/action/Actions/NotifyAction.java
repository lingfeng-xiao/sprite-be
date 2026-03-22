package com.lingfeng.sprite.action.Actions;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;

/**
 * 通知动作 - 发送系统通知
 *
 * 注意: 在实际运行时需要根据平台选择合适的通知实现
 */
public class NotifyAction implements ActionPlugin {

    private static final Logger logger = LoggerFactory.getLogger(NotifyAction.class);

    @Override
    public String getName() {
        return "NotifyAction";
    }

    @Override
    public ActionResult execute(Map<String, Object> params) {
        try {
            String actionParam = (String) params.get("actionParam");
            Instant timestamp = (Instant) params.get("timestamp");

            // 在实际实现中，这里会调用系统通知 API
            // 例如: Windows ToastNotification, macOS NSUserNotification 等
            logger.info("=== Notification ===");
            logger.info("Time: {}", timestamp);
            logger.info("Message: {}", actionParam);
            logger.info("===================");

            return ActionResult.success("Notification sent: " + actionParam);
        } catch (Exception e) {
            logger.error("NotifyAction failed: {}", e.getMessage());
            return ActionResult.failure("NotifyAction failed: " + e.getMessage());
        }
    }
}
