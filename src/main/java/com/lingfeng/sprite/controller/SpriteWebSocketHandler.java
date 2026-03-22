package com.lingfeng.sprite.controller;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.cognition.CognitionController;
import com.lingfeng.sprite.service.SpriteService;

/**
 * Sprite WebSocket 处理器
 *
 * 提供实时认知循环交互
 *
 * 消息格式 (发送):
 * {
 *   "type": "cycle" | "state" | "feedback",
 *   "data": { ... }
 * }
 *
 * 消息格式 (接收):
 * {
 *   "type": "result" | "error" | "state",
 *   "data": { ... }
 * }
 */
@Component
public class SpriteWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpriteWebSocketHandler.class);

    private final SpriteService spriteService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public SpriteWebSocketHandler(SpriteService spriteService, ObjectMapper objectMapper) {
        this.spriteService = spriteService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        logger.info("WebSocket connection established: {}", session.getId());

        // 发送连接成功消息
        sendMessage(session, new WebSocketMessage("connected", Map.of(
                "sessionId", session.getId(),
                "timestamp", Instant.now().toString()
        )));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            logger.debug("Received message from {}: {}", session.getId(), payload);

            WebSocketRequest request = objectMapper.readValue(payload, WebSocketRequest.class);

            switch (request.type()) {
                case "cycle" -> handleCycle(session);
                case "state" -> handleState(session);
                case "feedback" -> handleFeedback(session, request);
                default -> sendError(session, "Unknown request type: " + request.type());
            }
        } catch (Exception e) {
            logger.error("Error handling message: {}", e.getMessage());
            sendError(session, "Error: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        logger.info("WebSocket connection closed: {} - {}", session.getId(), status);
    }

    private void handleCycle(WebSocketSession session) {
        CognitionController.CognitionResult result = spriteService.cognitionCycle();

        sendMessage(session, new WebSocketMessage("cycle_result", Map.of(
                "perception", result.perception() != null ? result.perception().generateFeelings() : "none",
                "reflection", result.reflection() != null ? result.reflection().insight() : "none",
                "actionRecommendation", result.actionRecommendation() != null ?
                        result.actionRecommendation().recommendations() : "none",
                "timestamp", Instant.now().toString()
        )));
    }

    private void handleState(WebSocketSession session) {
        var state = spriteService.getState();

        sendMessage(session, new WebSocketMessage("state", Map.of(
                "isRunning", state.isRunning(),
                "hasLlmSupport", state.hasLlmSupport(),
                "lastCycleTime", state.lastCycleTime() != null ?
                        state.lastCycleTime().toString() : "never",
                "memoryStatus", state.memoryStatus() != null ?
                        state.memoryStatus().toString() : "unknown",
                "timestamp", Instant.now().toString()
        )));
    }

    private void handleFeedback(WebSocketSession session, WebSocketRequest request) {
        // 解析反馈请求并处理
        sendMessage(session, new WebSocketMessage("feedback_received", Map.of(
                "timestamp", Instant.now().toString()
        )));
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            logger.error("Error sending message: {}", e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, new WebSocketMessage("error", Map.of("error", error)));
    }

    // WebSocket 消息格式
    public record WebSocketMessage(String type, Map<String, Object> data) {}
    public record WebSocketRequest(String type, Map<String, Object> data) {}
}
