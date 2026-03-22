package com.openclaw.digitalbeings.boot.config;

import org.neo4j.driver.Driver;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(Driver.class)
public class SchemaInitHealthIndicator implements HealthIndicator {
    private final SchemaInitializer schemaInitializer;

    public SchemaInitHealthIndicator(SchemaInitializer schemaInitializer) {
        this.schemaInitializer = schemaInitializer;
    }

    @Override
    public Health health() {
        if (schemaInitializer.isInitialized()) {
            return Health.up()
                    .withDetail("schema", "initialized")
                    .build();
        } else {
            return Health.down()
                    .withDetail("schema", "not initialized")
                    .build();
        }
    }
}
