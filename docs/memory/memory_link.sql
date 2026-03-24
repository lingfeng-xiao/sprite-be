-- Memory Link Schema
-- Cross-references and relationships between memory entries
-- ME-001: Database persistence layer

-- ==================== Memory Links ====================
-- Links between different memory types for association traversal

CREATE TABLE IF NOT EXISTS memory_links (
    id                  VARCHAR(36) PRIMARY KEY,
    source_type         VARCHAR(50) NOT NULL, -- SENSORY, WORKING, EPISODIC, SEMANTIC, PROCEDURAL, PERCEPTIVE
    source_id           VARCHAR(36) NOT NULL,
    target_type         VARCHAR(50) NOT NULL,
    target_id           VARCHAR(36) NOT NULL,
    link_type           VARCHAR(50) NOT NULL, -- CAUSED_BY, RELATED_TO, REMINDS_OF, EVOLVED_FROM
    strength            FLOAT DEFAULT 0.5,
    context             TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(source_type, source_id, target_type, target_id, link_type)
);

-- Index for source-based traversal
CREATE INDEX idx_links_source ON memory_links(source_type, source_id);

-- Index for target-based traversal
CREATE INDEX idx_links_target ON memory_links(target_type, target_id);

-- Index for link type filtering
CREATE INDEX idx_links_type ON memory_links(link_type);

-- Index for strength-based retrieval
CREATE INDEX idx_links_strength ON memory_links(strength DESC);

-- ==================== Memory Context ====================
-- Shared context fragments for memory grouping

CREATE TABLE IF NOT EXISTS memory_context (
    id                  VARCHAR(36) PRIMARY KEY,
    context_key         VARCHAR(255) NOT NULL,
    context_value       TEXT NOT NULL,
    memory_type         VARCHAR(50) NOT NULL,
    memory_id           VARCHAR(36) NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(memory_type, memory_id, context_key)
);

-- Index for context lookup
CREATE INDEX idx_context_memory ON memory_context(memory_type, memory_id);

-- Index for key-based grouping
CREATE INDEX idx_context_key ON memory_context(context_key);

-- ==================== Stimulus Chain ====================
-- Tracks stimulus processing lineage for audit trail

CREATE TABLE IF NOT EXISTS stimulus_chain (
    id                  VARCHAR(36) PRIMARY KEY,
    original_stimulus_id VARCHAR(36) NOT NULL,
    current_id          VARCHAR(36) NOT NULL,
    stage               VARCHAR(50) NOT NULL, -- RAW, CLEANED, CLASSIFIED, DEDUPLICATED, SUMMARIZED, EMBEDDED
    processing_time_ms  INTEGER,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (original_stimulus_id) REFERENCES sensory_memory(id) ON DELETE CASCADE
);

-- Index for chain traversal
CREATE INDEX idx_chain_original ON stimulus_chain(original_stimulus_id);

-- Index for stage-based filtering
CREATE INDEX idx_chain_stage ON stimulus_chain(stage);

-- Index for performance analysis
CREATE INDEX idx_chain_time ON stimulus_chain(created_at, processing_time_ms);

-- ==================== Consolidation Rules ====================
-- Tracks rules for memory consolidation decisions

CREATE TABLE IF NOT EXISTS consolidation_rules (
    id                  VARCHAR(36) PRIMARY KEY,
    stimulus_type      VARCHAR(50) NOT NULL,
    pattern             TEXT NOT NULL,
    target_memory_type VARCHAR(50) NOT NULL, -- EPISODIC, SEMANTIC, PROCEDURAL, PERCEPTIVE
    salience_threshold  FLOAT DEFAULT 0.5,
    confidence         FLOAT DEFAULT 0.5,
    active             BOOLEAN DEFAULT TRUE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for type-based rule lookup
CREATE INDEX idx_rules_type ON consolidation_rules(stimulus_type, active);

COMMENT ON TABLE memory_links IS 'Cross-references between memory entries';
COMMENT ON TABLE memory_context IS 'Shared context fragments for memory grouping';
COMMENT ON TABLE stimulus_chain IS 'Tracks stimulus processing lineage for audit trail';
COMMENT ON TABLE consolidation_rules IS 'Rules for memory consolidation decisions';
