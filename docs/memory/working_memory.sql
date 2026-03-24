-- Working Memory Schema
-- Current session memory with 7-item limit (Miller's Law)
-- ME-001: Database persistence layer

CREATE TABLE IF NOT EXISTS working_memory (
    id              VARCHAR(36) PRIMARY KEY,
    content         TEXT NOT NULL,
    content_type    VARCHAR(50),
    abstraction     TEXT NOT NULL,
    source_id       VARCHAR(36),
    access_count    INTEGER DEFAULT 0,
    last_accessed   TIMESTAMP NOT NULL,
    relevance       FLOAT DEFAULT 0.5,
    salience        FLOAT DEFAULT 0.5,
    created_at      TIMESTAMP NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    FOREIGN KEY (source_id) REFERENCES sensory_memory(id) ON DELETE SET NULL
);

-- Index for relevance-based eviction
CREATE INDEX idx_working_relevance ON working_memory(relevance DESC);

-- Index for access pattern analysis
CREATE INDEX idx_working_access ON working_memory(last_accessed DESC);

-- Index for session-based cleanup
CREATE INDEX idx_working_expires ON working_memory(expires_at);

-- Index for abstraction search
CREATE INDEX idx_working_abstraction ON working_memory(abstraction);

-- LRU eviction policy: DELETE oldest when count > 7
-- Application logic enforces the 7-item limit

COMMENT ON TABLE working_memory IS 'Working memory - Current session memory with 7-item limit (Miller Law)';
