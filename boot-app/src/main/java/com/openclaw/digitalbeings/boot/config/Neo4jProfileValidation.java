package com.openclaw.digitalbeings.boot.config;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import jakarta.annotation.PostConstruct;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("neo4j")
@ConditionalOnBean(Driver.class)
public class Neo4jProfileValidation {
    private static final Logger log = LoggerFactory.getLogger(Neo4jProfileValidation.class);

    private final BeingStore beingStore;

    public Neo4jProfileValidation(BeingStore beingStore) {
        this.beingStore = beingStore;
    }

    @PostConstruct
    public void validateNeo4jProfile() {
        if (beingStore instanceof InMemoryBeingStore) {
            throw new IllegalStateException(
                "FATAL: neo4j profile is active but BeingStore is InMemoryBeingStore. " +
                "This indicates a configuration error. " +
                "Check your profile activation and bean overrides."
            );
        }
        log.info("Neo4j profile validation passed. Using: {}", beingStore.getClass().getName());
    }
}
