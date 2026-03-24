# Memory System Design

## Overview

Three-layer memory architecture implementing human memory hierarchy with database persistence.

## Architecture

```
Sensory Memory (30s rolling window)
    |  Pattern Detection
    v
Working Memory (7-item limit, Miller's Law)
    |  Consolidation
    v
Long-Term Memory (persistent)
    |-- Episodic (events)
    |-- Semantic (concepts)
    |-- Procedural (skills)
    |-- Perceptive (patterns)
```

## Task References

- **ME-001**: Three-layer memory with database persistence
- **ME-003**: Memory write pipeline implementation
- **PE-001**: Stimulus model standardization

## Components

### Stimulus Model (`Stimulus.java`)
- `id`: Unique identifier
- `source`: Origin of the stimulus
- `type`: StimulusType enum (VISUAL, AUDITORY, TEXT, COMMAND, EMOTIONAL, SYSTEM, ENVIRONMENT)
- `content`: Raw content
- `timestamp`: When it occurred
- `salience`: Importance score (0.0-1.0)
- `rawRef`: Reference to original raw data
- `contextFragments`: Associated context metadata

### Memory Classes

#### SensoryMemory
- 30-second rolling window
- Pattern detection for consolidation
- Time-based expiration

#### WorkingMemory
- 7-item limit (Miller's Law)
- Relevance-based eviction
- Access frequency tracking
- Consolidation from sensory patterns

#### LongTermMemory
- Four memory types: episodic, semantic, procedural, perceptive
- Retention policy (365 days default)
- Query and retrieval operations

### MemoryWritePipeline
Stages:
1. **Cleaning**: Normalize content, remove noise
2. **Classification**: Determine salience based on type
3. **Deduplication**: Detect similar recent stimuli
4. **Summarization**: Compress if content exceeds threshold
5. **Write**: Store in sensory memory, promote to working if significant
6. **Embedding**: Generate vector representation (placeholder)

### MemoryRepository
Repository pattern for database persistence:
- CRUD operations for all memory types
- Query methods with pattern matching
- Expiration management
- Memory link tracking

## Database Schema

See SQL files in `docs/memory/`:
- `sensory_memory.sql`
- `working_memory.sql`
- `long_term_memory.sql`
- `memory_link.sql`

## Configuration Constants

| Constant | Value | Description |
|----------|-------|-------------|
| SENSORY_WINDOW_SECONDS | 30 | Sensory memory retention |
| WORKING_MEMORY_MAX_ITEMS | 7 | Working memory capacity |
| LONG_TERM_RETENTION_DAYS | 365 | Long-term memory retention |
