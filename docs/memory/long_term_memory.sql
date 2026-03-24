-- Long-Term Memory Schema
-- Persistent storage for episodic, semantic, procedural, and perceptive memories
-- ME-001: Database persistence layer

-- ==================== Episodic Memory ====================
-- Event experiences with temporal and contextual markers

CREATE TABLE IF NOT EXISTS episodic_memory (
    id              VARCHAR(36) PRIMARY KEY,
    timestamp       TIMESTAMP NOT NULL,
    location        VARCHAR(255),
    people          TEXT, -- JSON array
    experience      TEXT NOT NULL,
    emotion         VARCHAR(100),
    outcome         TEXT,
    lesson          TEXT,
    salience        FLOAT DEFAULT 0.5,
    embedding       TEXT, -- Vector embedding for semantic search
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for temporal queries
CREATE INDEX idx_episodic_timestamp ON episodic_memory(timestamp DESC);

-- Index for emotion-based recall
CREATE INDEX idx_episodic_emotion ON episodic_memory(emotion);

-- Index for location-based recall
CREATE INDEX idx_episodic_location ON episodic_memory(location);

-- Index for semantic search (requires pg_vector or similar)
-- CREATE INDEX idx_episodic_embedding ON episodic_memory USING ivfflat(embedding vector_cosine_ops);

-- ==================== Semantic Memory ====================
-- Knowledge concepts with definitions and relationships

CREATE TABLE IF NOT EXISTS semantic_memory (
    id              VARCHAR(36) PRIMARY KEY,
    concept         VARCHAR(255) NOT NULL UNIQUE,
    definition      TEXT NOT NULL,
    examples        TEXT, -- JSON array
    related_concepts TEXT, -- JSON array
    confidence      FLOAT DEFAULT 0.5,
    embedding       TEXT, -- Vector embedding for concept similarity
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed   TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for concept lookup
CREATE INDEX idx_semantic_concept ON semantic_memory(concept);

-- Index for confidence-based retrieval
CREATE INDEX idx_semantic_confidence ON semantic_memory(confidence DESC);

-- ==================== Procedural Memory ====================
-- Skills and procedures with mastery tracking

CREATE TABLE IF NOT EXISTS procedural_memory (
    id              VARCHAR(36) PRIMARY KEY,
    skill_name      VARCHAR(255) NOT NULL UNIQUE,
    procedure       TEXT NOT NULL,
    level           VARCHAR(50) DEFAULT 'BASIC',
    last_practiced  TIMESTAMP,
    times_performed INTEGER DEFAULT 0,
    success_rate    FLOAT DEFAULT 0.5,
    embedding       TEXT, -- Vector embedding for procedure similarity
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for skill lookup
CREATE INDEX idx_procedural_skill ON procedural_memory(skill_name);

-- Index for mastery level sorting
CREATE INDEX idx_procedural_level ON procedural_memory(level, success_rate DESC);

-- ==================== Perceptive Memory ====================
-- Pattern associations for automatic recall

CREATE TABLE IF NOT EXISTS perceptive_memory (
    id              VARCHAR(36) PRIMARY KEY,
    pattern         VARCHAR(255) NOT NULL,
    association     TEXT NOT NULL,
    trigger         VARCHAR(255) NOT NULL,
    strength        FLOAT DEFAULT 0.5,
    times_triggered INTEGER DEFAULT 0,
    embedding       TEXT, -- Vector embedding for pattern matching
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(pattern, trigger)
);

-- Index for pattern lookup
CREATE INDEX idx_perceptive_pattern ON perceptive_memory(pattern);

-- Index for trigger-based recall
CREATE INDEX idx_perceptive_trigger ON perceptive_memory(trigger);

-- Index for strength-based retrieval
CREATE INDEX idx_perceptive_strength ON perceptive_memory(strength DESC);

COMMENT ON TABLE episodic_memory IS 'Episodic memory - Event experiences with temporal and contextual markers';
COMMENT ON TABLE semantic_memory IS 'Semantic memory - Knowledge concepts with definitions and relationships';
COMMENT ON TABLE procedural_memory IS 'Procedural memory - Skills and procedures with mastery tracking';
COMMENT ON TABLE perceptive_memory IS 'Perceptive memory - Pattern associations for automatic recall';
