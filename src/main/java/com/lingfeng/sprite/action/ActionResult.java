package com.lingfeng.sprite.action;

import java.time.Instant;

/**
 * 动作执行结果
 */
public record ActionResult(
        boolean success,
        String message,
        Instant timestamp,
        Object data
) {
    public static ActionResult success(String message) {
        return new ActionResult(true, message, Instant.now(), null);
    }

    public static ActionResult success(String message, Object data) {
        return new ActionResult(true, message, Instant.now(), data);
    }

    public static ActionResult failure(String message) {
        return new ActionResult(false, message, Instant.now(), null);
    }

    public static ActionResult failure(String message, Object data) {
        return new ActionResult(false, message, Instant.now(), data);
    }
}
