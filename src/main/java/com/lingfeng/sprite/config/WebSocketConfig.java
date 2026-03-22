package com.lingfeng.sprite.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.lingfeng.sprite.controller.ChatWebSocketHandler;
import com.lingfeng.sprite.controller.SpriteWebSocketHandler;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SpriteWebSocketHandler spriteWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(SpriteWebSocketHandler spriteWebSocketHandler, ChatWebSocketHandler chatWebSocketHandler) {
        this.spriteWebSocketHandler = spriteWebSocketHandler;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(spriteWebSocketHandler, "/ws/sprite")
                .setAllowedOrigins("*");
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("*");
    }
}
