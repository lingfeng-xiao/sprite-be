package com.openclaw.digitalbeings.boot.config;

import org.neo4j.driver.Driver;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(Driver.class)
public class Neo4jHealthIndicator implements HealthIndicator {
    private final Driver driver;

    public Neo4jHealthIndicator(Driver driver) {
        this.driver = driver;
    }

    @Override
    public Health health() {
        try (var session = driver.session()) {
            session.run("RETURN 1").consume();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
