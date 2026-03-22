package com.lingfeng.sprite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Sprite - 数字生命的 Spring Boot 应用入口
 *
 * 完整闭环: 感知 → 认知 → 行动
 *
 * REST API:
 * - POST /api/sprite/cycle - 手动触发一轮认知
 * - GET /api/sprite/state - 获取当前状态
 * - POST /api/sprite/feedback - 提交反馈
 * - GET /api/sprite/memory - 获取记忆状态
 *
 * WebSocket:
 * - /ws/sprite - WebSocket 连接端点
 */
@SpringBootApplication
@EnableWebSocket
public class SpriteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpriteApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
