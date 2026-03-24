-- Sensory Memory Schema
-- 30-second rolling window of raw stimuli
-- ME-001: Database persistence layer

CREATE TABLE IF NOT EXISTS sensory_memory (
    id              VARCHAR(36) PRIMARY KEY,
    source          VARCHAR(255) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    content         TEXT NOT NULL,
    content_type    VARCHAR(50),
    timestamp       TIMESTAMP NOT NULL,
    salience        FLOAT DEFAULT 0.5,
    raw_ref         VARCHAR(512),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP NOT NULL
);

-- Index for time-based expiration cleanup
CREATE INDEX idx_sensory_expires ON sensory_memory(expires_at);

-- Index for type-based queries
CREATE INDEX idx_sensory_type ON sensory_memory(type);

-- Index for source-based queries
CREATE INDEX idx_sensory_source ON sensory_memory(source);

-- Index for timestamp ordering
CREATE INDEX idx_sensory_timestamp ON sensory_memory(timestamp DESC);

-- TTL: Auto-delete rows where expires_at < NOW()
-- PostgreSQL: Use pg_cron or a scheduled job
-- MySQL: Use Event Scheduler
-- SQLite: Manual cleanup via application

COMMENT ON TABLE sensory_memory IS 'Sensory memory - 30-second rolling window of raw stimuli for pattern detection';
