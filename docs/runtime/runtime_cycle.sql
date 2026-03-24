-- Runtime Cycle Table
-- Stores metadata about each cognitive cycle executed by the Sprite runtime

CREATE TABLE IF NOT EXISTS runtime_cycle (
    -- Primary key: globally unique cycle identifier
    cycle_id VARCHAR(64) PRIMARY KEY,

    -- Cycle creation timestamp
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Cycle start timestamp (when it actually began executing)
    started_at TIMESTAMP WITH TIME ZONE,

    -- Cycle end timestamp (when it completed, failed, or was cancelled)
    ended_at TIMESTAMP WITH TIME ZONE,

    -- Current cycle state (see CycleSession enum)
    -- States: CREATED, COLLECTING, REASONING, DECIDING, ACTING, CONSOLIDATING, COMPLETED, FAILED, CANCELLED
    session_state VARCHAR(20) NOT NULL DEFAULT 'CREATED',

    -- Cycle priority (higher = more important)
    priority INT NOT NULL DEFAULT 0,

    -- Request key for deduplication (maps similar requests to same cycle)
    request_key VARCHAR(128),

    -- Parent cycle ID (for sub-cycles), nullable
    parent_cycle_id VARCHAR(64),

    -- Foreign key to parent cycle if exists
    CONSTRAINT fk_parent_cycle FOREIGN KEY (parent_cycle_id)
        REFERENCES runtime_cycle(cycle_id) ON DELETE SET NULL,

    -- Index for querying by state
    CONSTRAINT idx_session_state CHECK (session_state IN (
        'CREATED', 'COLLECTING', 'REASONING', 'DECIDING',
        'ACTING', 'CONSOLIDATING', 'COMPLETED', 'FAILED', 'CANCELLED'
    ))
);

-- Index for deduplication lookups
CREATE INDEX IF NOT EXISTS idx_runtime_cycle_request_key
    ON runtime_cycle(request_key)
    WHERE request_key IS NOT NULL;

-- Index for finding cycles by parent
CREATE INDEX IF NOT EXISTS idx_runtime_cycle_parent
    ON runtime_cycle(parent_cycle_id);

-- Index for time-based queries
CREATE INDEX IF NOT EXISTS idx_runtime_cycle_created_at
    ON runtime_cycle(created_at DESC);

-- Index for finding active cycles
CREATE INDEX IF NOT EXISTS idx_runtime_cycle_active
    ON runtime_cycle(session_state)
    WHERE session_state NOT IN ('COMPLETED', 'FAILED', 'CANCELLED');

-- Cycle Phase Transition Table (optional, for detailed tracing)
CREATE TABLE IF NOT EXISTS runtime_cycle_phase (
    -- Primary key
    phase_id BIGSERIAL PRIMARY KEY,

    -- Reference to the cycle
    cycle_id VARCHAR(64) NOT NULL,

    -- Foreign key to cycle
    CONSTRAINT fk_cycle FOREIGN KEY (cycle_id)
        REFERENCES runtime_cycle(cycle_id) ON DELETE CASCADE,

    -- Phase name
    phase VARCHAR(20) NOT NULL,

    -- When the phase started
    phase_started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- When the phase ended
    phase_ended_at TIMESTAMP WITH TIME ZONE,

    -- Duration in milliseconds
    duration_ms BIGINT,

    -- Phase metadata (JSON for flexibility)
    metadata JSONB,

    -- Index for cycle phase history
    CONSTRAINT idx_cycle_phase_cycle_id FOREIGN KEY (cycle_id)
        REFERENCES runtime_cycle(cycle_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cycle_phase_cycle_id
    ON runtime_cycle_phase(cycle_id);

-- Snapshot Table (for RuntimeSnapshot persistence)
CREATE TABLE IF NOT EXISTS runtime_snapshot (
    -- Primary key
    snapshot_id BIGSERIAL PRIMARY KEY,

    -- Snapshot timestamp
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Runtime status at snapshot time
    status VARCHAR(20) NOT NULL,

    -- Current cycle ID if any
    current_cycle_id VARCHAR(64),

    -- Total cycles executed up to this point
    total_cycles_executed BIGINT NOT NULL DEFAULT 0,

    -- Total cycles failed up to this point
    total_cycles_failed BIGINT NOT NULL DEFAULT 0,

    -- Uptime in milliseconds at snapshot time
    uptime_ms BIGINT NOT NULL DEFAULT 0,

    -- Active cycle count at snapshot time
    active_cycle_count INT NOT NULL DEFAULT 0,

    -- Health metrics (JSON)
    health_metrics JSONB,

    -- Resource usage (JSON)
    resource_usage JSONB,

    -- Index for time-based queries
    CONSTRAINT idx_snapshot_captured_at FOREIGN KEY (captured_at)
        REFERENCES runtime_snapshot(captured_at) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_runtime_snapshot_captured_at
    ON runtime_snapshot(captured_at DESC);