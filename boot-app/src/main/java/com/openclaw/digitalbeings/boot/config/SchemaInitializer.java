package com.openclaw.digitalbeings.boot.config;

import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.MigrationState;
import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import jakarta.annotation.PostConstruct;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Profile("neo4j")
@ConditionalOnBean(Driver.class)
public class SchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final Driver driver;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public SchemaInitializer(Driver driver) {
        this.driver = driver;
    }

    @PostConstruct
    public void initializeSchema() {
        log.info("Initializing Neo4j schema migrations...");
        try {
            var config = MigrationsConfig.builder()
                    .withLocationsToScan("classpath:/neo4j/migrations")
                    .build();

            var migrations = new Migrations(config, driver);

            MigrationChain chain = migrations.info();
            Collection<MigrationChain.Element> elements = chain.getElements();
            long pendingCount = elements.stream()
                    .filter(e -> e.getState() == MigrationState.PENDING)
                    .count();

            log.info("Found {} pending migrations", pendingCount);

            if (pendingCount > 0) {
                migrations.apply();
                log.info("Applied {} migration(s)", pendingCount);
            }

            initialized.set(true);
            log.info("Schema initialization complete.");
        } catch (Exception e) {
            log.error("Schema initialization failed", e);
            throw new RuntimeException("Schema initialization failed", e);
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }
}
