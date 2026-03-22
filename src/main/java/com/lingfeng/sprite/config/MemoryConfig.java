package com.lingfeng.sprite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.Memory;

/**
 * 记忆系统配置
 *
 * 将 MemorySystem.Memory 作为 Spring Bean 管理，
 * 以便在其他服务中注入使用
 */
@Configuration
public class MemoryConfig {

    /**
     * 创建记忆系统 Bean
     *
     * 注意：MemorySystem.Memory 是无状态的容器，
     * 实际的记忆数据由其内部的 SensoryMemory, WorkingMemory, LongTermMemory 管理
     */
    @Bean
    public Memory memory() {
        return new Memory();
    }
}
