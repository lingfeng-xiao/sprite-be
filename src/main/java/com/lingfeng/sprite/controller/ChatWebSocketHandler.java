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
import com.lingfeng.sprite.service.ConversationService;

/**
 * Chat WebSocket 处理器
 *
 * 提供实时对话功能
 *
 * 消息格式 (接收):
 * {
 *   "type": "chat",
 *   "content": "用户消息"
 * }
 *
 * 消息格式 (发送):
 * {
 *   "type": "response" | "error" | "typing",
 *   "content": "回复内容",
 *   "actions": ["动作列表"]
 * }
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ConversationService conversationService, ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        logger.info("Chat WebSocket connection established: {}", session.getId());

        sendMessage(session, new ChatMessage("connected", "连接成功", null));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            logger.debug("Received chat message from {}: {}", session.getId(), payload);

            ChatRequest request = objectMapper.readValue(payload, ChatRequest.class);

            switch (request.type()) {
                case "chat" -> handleChat(session, request.content());
                case "ping" -> sendMessage(session, new ChatMessage("pong", "pong", null));
                default -> sendError(session, "Unknown request type: " + request.type());
            }
        } catch (Exception e) {
            logger.error("Error handling chat message: {}", e.getMessage());
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleChat(WebSocketSession session, String content) {
        if (content == null || content.isBlank()) {
            sendError(session, "Message content is empty");
            return;
        }

        logger.info("Processing chat message: {}", content);

        // 异步处理，不阻塞 WebSocket
        new Thread(() -> {
            try {
                // 发送打字状态
                sendMessage(session, new ChatMessage("typing", null, null));

                // 调用对话服务
                ConversationService.ConversationResponse response = conversationService.chat(content, session.getId());

                // 发送回复
                sendMessage(session, new ChatMessage(
                        "response",
                        response.response(),
                        response.actions()
                ));

            } catch (Exception e) {
                logger.error("Error processing chat: {}", e.getMessage(), e);
                sendError(session, "处理消息时出错: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        logger.info("Chat WebSocket connection closed: {} - {}", session.getId(), status);
    }

    private void sendMessage(WebSocketSession session, ChatMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            logger.error("Error sending chat message: {}", e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, new ChatMessage("error", error, null));
    }

    // WebSocket 消息格式
    public record ChatMessage(String type, String content, java.util.List<String> actions) {}

    public record ChatRequest(String type, String content) {}
}
