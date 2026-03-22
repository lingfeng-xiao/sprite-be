package com.openclaw.digitalbeings.boot.config;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("memory")
public class MemoryBeingStoreConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MemoryBeingStoreConfiguration.class);

    @Bean
    BeingStore beingStore() {
        return new InMemoryBeingStore();
    }

    @PostConstruct
    public void validateMemoryProfile() {
        log.warn("==============================================");
        log.warn("WARNING: Running with memory profile (dev only)");
        log.warn("InMemoryBeingStore is not durable for production");
        log.warn("Please use --spring.profiles.active=neo4j for production");
        log.warn("==============================================");
    }
}
