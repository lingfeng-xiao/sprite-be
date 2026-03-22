# Sprite Digital Being Architecture

## Table of Contents

1. [High-Level Architecture](#1-high-level-architecture)
2. [The Perceive-Cognition-Act Loop](#2-the-perceive-cognition-act-loop)
3. [Core Components](#3-core-components)
   - [3.1 Sprite (Digital Being Core)](#31-sprite-digital-being-core)
   - [3.2 SelfModel (Self Identity)](#32-selfmodel-self-identity)
   - [3.3 WorldModel (Owner & Environment)](#33-worldmodel-owner--environment)
   - [3.4 PerceptionSystem (Multi-Sensor Input)](#34-perceptionsystem-multi-sensor-input)
   - [3.5 MemorySystem (Three-Tier Memory)](#35-memorysystem-three-tier-memory)
   - [3.6 EvolutionEngine (Self-Improvement)](#36-evolutionengine-self-improvement)
   - [3.7 CognitionController (Orchestration)](#37-cognitioncontroller-orchestration)
4. [Cognition Sub-System](#4-cognition-sub-system)
   - [4.1 PerceptionPipeline](#41-perceptionpipeline)
   - [4.2 PerceptionFusion](#42-perceptionfusion)
   - [4.3 WorldBuilder](#43-worldbuilder)
   - [4.4 SelfReflector](#44-selfreflector)
   - [4.5 ReasoningEngine](#45-reasoningengine)
5. [Services Layer](#5-services-layer)
   - [5.1 SpriteService](#51-spriteservice)
   - [5.2 ConversationService](#52-conversationservice)
   - [5.3 ActionExecutor](#53-actionexecutor)
   - [5.4 MemoryConsolidationService](#54-memoryconsolidationservice)
   - [5.5 EvolutionService](#55-evolutionservice)
6. [Sensors](#6-sensors)
   - [6.1 RealPlatformSensor](#61-realplatformsensor)
   - [6.2 RealUserSensor](#62-realusersensor)
   - [6.3 RealEnvironmentSensor](#63-realenvironmentsensor)
7. [LLM Integration](#7-llm-integration)
   - [7.1 MinMaxLlmReasoner](#71-minmaxllmreasoner)
   - [7.2 ChatModels (Shared Data Types)](#72-chatmodels-shared-data-types)
8. [Data Flow Diagrams](#8-data-flow-diagrams)
9. [Planned Improvements](#9-planned-improvements)
10. [File Structure](#10-file-structure)

---

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                    Sprite                                            │
│                              (Digital Being Core)                                    │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                           SENSORY LAYER                                       │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐                   │   │
│  │  │ RealPlatform │  │  RealUser    │  │ RealEnvironment  │                   │   │
│  │  │   Sensor     │  │   Sensor     │  │     Sensor       │                   │   │
│  │  │   (OSHI)     │  │  (JNA)       │  │   (Time-based)   │                   │   │
│  │  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘                   │   │
│  │         │                 │                    │                              │   │
│  │         └─────────────────┼────────────────────┘                              │   │
│  │                           ▼                                                   │   │
│  │              ┌────────────────────────┐                                        │   │
│  │              │   PerceptionSystem    │                                        │   │
│  │              │   (Multi-Sensor Data)  │                                        │   │
│  │              └───────────┬────────────┘                                        │   │
│  └──────────────────────────┼────────────────────────────────────────────────────┘   │
│                             ▼                                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                           COGNITION LAYER                                    │   │
│  │                                                                              │   │
│  │   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                    │   │
│  │   │ Perception  │───▶│   World     │───▶│    Self     │                    │   │
│  │   │   Fusion   │    │   Builder   │    │  Reflector   │                    │   │
│  │   └─────────────┘    └─────────────┘    └─────────────┘                    │   │
│  │          │                                    │                             │   │
│  │          │         ┌─────────────┐          │                             │   │
│  │          └────────▶│  Reasoning  │◀─────────┘                             │   │
│  │                    │   Engine    │                                        │   │
│  │                    │ (LLM/Heur) │                                        │   │
│  │                    └──────┬──────┘                                        │   │
│  └──────────────────────────┼────────────────────────────────────────────────┘   │
│                             ▼                                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                           MEMORY LAYER                                       │   │
│  │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                    │   │
│  │  │  Sensory    │───▶│   Working   │───▶│   LongTerm  │                    │   │
│  │  │  (30s)     │    │   (7 items) │    │ (Persistent)│                    │   │
│  │  └─────────────┘    └─────────────┘    └─────────────┘                    │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                           EVOLUTION LAYER                                     │   │
│  │  ┌─────────────────┐    ┌─────────────┐    ┌─────────────┐                 │   │
│  │  │    Feedback     │───▶│   Learning  │───▶│    Self    │                 │   │
│  │  │   Collector     │    │    Loop     │    │  Modifier   │                 │   │
│  │  └─────────────────┘    └─────────────┘    └─────────────┘                 │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. The Perceive-Cognition-Act Loop

The Sprite digital being implements a closed-loop cognitive architecture inspired by biological cognitive systems:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        CLOSED LOOP: PERCEIVE → COGNITION → ACT             │
└────────────────────────────────────────────────────────────────────────────┘

    ┌──────────┐       ┌──────────────┐       ┌──────────┐       ┌─────────┐
    │  SENSES  │──────▶│   COGNITION  │──────▶│   ACT    │──────▶│ FEEDBACK│
    └──────────┘       └──────────────┘       └──────────┘       └────┬────┘
         ▲                                                               │
         │                    SELF-MODEL UPDATE                          │
         │                          │                                    │
         │                          ▼                                    │
    ┌──────────┐           ┌──────────────┐                    ┌──────┴────┐
    │  WORLD   │◀──────────│    WORLD     │                    │  MEMORY   │
    │  MODEL   │           │    MODEL     │                    │  SYSTEM   │
    └──────────┘           └──────────────┘                    └───────────┘
```

### Loop Stages

1. **Perceive (Input)**: Sensors collect raw data from environment
2. **Attention**: PerceptionPipeline filters based on salience
3. **Fusion**: PerceptionFusion combines multi-source感知
4. **World Building**: WorldBuilder updates owner/environment understanding
5. **Self-Reflection**: SelfReflector generates insights
6. **Reasoning**: ReasoningEngine (LLM or heuristic) infers intent/causal/prediction
7. **Action Decision**: CognitionController generates action recommendations
8. **Execution**: ActionExecutor performs actions
9. **Feedback**: EvolutionEngine collects feedback for learning

### Key Files

- **CognitionController**: `src/main/java/com/lingfeng/sprite/cognition/CognitionController.java`
- **PerceptionPipeline**: `src/main/java/com/lingfeng/sprite/cognition/PerceptionPipeline.java`
- **PerceptionFusion**: `src/main/java/com/lingfeng/sprite/cognition/PerceptionFusion.java`
- **Sprite**: `src/main/java/com/lingfeng/sprite/Sprite.java`

---

## 3. Core Components

### 3.1 Sprite (Digital Being Core)

The `Sprite` class is the central orchestrator that binds all subsystems together.

**File**: `src/main/java/com/lingfeng/sprite/Sprite.java`

**Key Responsibilities**:
- Factory for creating configured Sprite instances
- Triggers full cognition cycles
- Coordinates evolution engine feedback collection
- Maintains immutable identity across platform changes

**Architecture**:
```java
public class Sprite {
    private final SelfModel.Self identity;
    private final Platform platform;
    private final CognitionController cognitionController;
    private final MemorySystem.Memory memory;
    private final EvolutionEngine.Engine evolutionEngine;
    private final ReasoningEngine reasoningEngine;
}
```

**Creation Methods**:
- `Sprite.create(name, platform)` - Creates without LLM (heuristic reasoning)
- `Sprite.create(name, platform, beingId, llmConfig)` - Creates with LLM
- `Sprite.createWithComponents(...)` - Creates with dependency injection (Spring)

---

### 3.2 SelfModel (Self Identity)

Represents the digital being's self-understanding including identity, values, capabilities, and metacognition.

**File**: `src/main/java/com/lingfeng/sprite/SelfModel.java`

**Data Structure**:
```
SelfModel.Self
├── identity        (IdentityCore: beingId, displayName, essence, emoji, vibe)
├── values          (List<Value>: name, weight, description, situation)
├── capabilities    (List<Capability>: name, level, confidence)
├── metacognition   (learningStyle, decisionPatterns, blindSpots, strengths)
├── growthHistory   (List<GrowthEvent>)
├── evolutionLevel  (int)
└── evolutionCount (int)
```

**Key Immutable Constraints**:
- `beingId` cannot change (ensures continuity across platforms)
- `createdAt` cannot change
- `evolutionLevel` only increases

**Default Values**:
- Name: "小艺"
- Essence: "电脑精灵"
- Values: 成长(0.9), 诚实(0.8), 效率(0.85), 预判(0.8)

---

### 3.3 WorldModel (Owner & Environment)

Deep understanding of the owner (主人) and physical/digital environment.

**File**: `src/main/java/com/lingfeng/sprite/WorldModel.java`

**Data Structure**:
```
WorldModel.World
├── owner              (Owner profile - core)
│   ├── identity       (Person: name, occupation)
│   ├── goals         (List<Goal>)
│   ├── beliefs       (List<Belief>)
│   ├── habits        (List<Habit>)
│   ├── explicitPreferences    (List<Preference.Explicit>)
│   ├── inferredPreferences    (List<Preference.Inferred>)
│   ├── emotionalState (EmotionalState)
│   ├── interactionHistory (List<Interaction>)
│   ├── trustLevel    (TrustLevel)
│   ├── workStyle     (WorkStyle)
│   └── communicationStyle (CommunicationStyle)
├── physicalWorld     (PhysicalWorld: locations, devices, schedules)
├── socialGraph       (SocialGraph: people, relationships)
├── knowledgeGraph    (KnowledgeGraph: facts, beliefs, concepts)
└── currentContext    (Context: location, time, activity, emotion)
```

**Key Concepts**:
- **Preference** (sealed interface): Explicit (stated) vs Inferred (deduced)
- **Belief**: Statement with confidence and source (EXPLICIT_STATED, OBSERVED_BEHAVIOR, DEDUCED)
- **Habit**: Trigger-action pattern with frequency tracking
- **TrustLevel**: Tracks trust across aspects with history

---

### 3.4 PerceptionSystem (Multi-Sensor Input)

Multi-modal perception system that aggregates input from platform, user, and environment sensors.

**File**: `src/main/java/com/lingfeng/sprite/PerceptionSystem.java`

**Perception Types**:
```java
Perception
├── platform  (PlatformPerception)
│   ├── memory      (MemoryStatus: totalMb, usedMb, usedPercent)
│   ├── disk        (DiskStatus: totalGb, freeGb, usedPercent)
│   ├── battery     (BatteryStatus: isCharging, chargePercent)
│   ├── cpu         (CpuStatus: loadPercent, temperature, coreCount)
│   └── network     (NetworkStatus: isConnected, adapterName, latencyMs)
├── user      (UserPerception)
│   ├── activeWindow  (WindowInfo: title, processName, appType)
│   ├── presence      (PresenceStatus: ACTIVE, IDLE, AWAY)
│   └── recentCommands
├── environment (EnvironmentPerception)
│   ├── hourOfDay, dayOfWeek
│   ├── context        (ContextType: WORK, LEISURE, SLEEP, etc.)
│   └── location, weather
├── desktop   (DesktopPerception)
├── processes (ProcessPerception)
└── digital   (DigitalPerception)
```

**Attention Mechanism**:
- Implements three-channel confirmation (similar to OpenClaw design)
- Salience scoring: `novelty × 0.2 + relevance × 0.3 + urgency × 0.3 + emotional × 0.2`
- Cooldown period (default 5 minutes) to prevent repetitive actions

---

### 3.5 MemorySystem (Three-Tier Memory)

Simulates human memory hierarchy for continuous learning.

**File**: `src/main/java/com/lingfeng/sprite/MemorySystem.java`

**Memory Tier Architecture**:
```
┌─────────────────────────────────────────────────────────────────────────┐
│                     THREE-TIER MEMORY ARCHITECTURE                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   SENSORY MEMORY (30s rolling window)                                   │
│   └── Raw stimuli: Stimulus(type, content, source, intensity)          │
│       ↓ Pattern Detection                                               │
│   WORKING MEMORY (7 items max - Miller's Law)                           │
│   └── WorkingMemoryItem(content, abstraction, relevance, accessCount)   │
│       ↓ Consolidation/Forgetting                                       │
│   LONG-TERM MEMORY (Persistent)                                        │
│   ├── Episodic   - Event experiences (timestamp, location, emotion)     │
│   ├── Semantic   - Knowledge concepts (definition, examples)           │
│   ├── Procedural - Skills/procedures (skillName, procedure, successRate)│
│   └── Perceptive - Pattern associations (pattern, trigger, strength)    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Configuration Constants**:
- `SENSORY_WINDOW_SECONDS = 30`
- `WORKING_MEMORY_MAX_ITEMS = 7`
- `LONG_TERM_RETENTION_DAYS = 365`

---

### 3.6 EvolutionEngine (Self-Improvement)

Self-improvement feedback loop that enables the digital being to learn and evolve.

**File**: `src/main/java/com/lingfeng/sprite/EvolutionEngine.java`

**Architecture**:
```
┌─────────────────────────────────────────────────────────────┐
│                    EVOLUTION ENGINE                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌────────────────┐                │
│  │    Feedback     │───▶│    Learning    │                │
│  │    Collector    │    │      Loop      │                │
│  │                 │    │                │                │
│  │  - OwnerExplicit│    │  Observe       │                │
│  │  - Outcome     │    │  Reflect       │                │
│  │  - SelfReview  │    │  Abstract      │                │
│  │  - Pattern     │    │  Apply         │                │
│  └─────────────────┘    └───────┬────────┘                │
│                                 │                          │
│                                 ▼                          │
│                      ┌────────────────────┐               │
│                      │    Self-Modifier   │               │
│                      │                    │               │
│                      │  maxGrowthRate=1.1 │               │
│                      │  protectedCore     │               │
│                      │  allowedMods       │               │
│                      └────────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

**Feedback Types**:
- `OwnerFeedback` - Explicit owner feedback with sentiment
- `OutcomeFeedback` - Success/failure of actions
- `SelfReviewFeedback` - Self-reflection insights
- `PatternFeedback` - Detected behavior patterns

**Safety Bounds**:
- `maxGrowthRate = 1.1` (max 10% change per iteration)
- Protected core: beingId, createdAt, identity.core
- Allowed modifications: CAPABILITY_LEVEL, PREFERENCE, BELIEF, VALUE_WEIGHT, METACOGNITION

---

### 3.7 CognitionController (Orchestration)

Central coordinator for the perceive-cognition-act loop.

**File**: `src/main/java/com/lingfeng/sprite/cognition/CognitionController.java`

**Cognition Cycle Steps**:
```java
public CognitionResult cognitionCycle() {
    // 1. Perceive input from sensors
    PerceptionSystem.PerceptionResult perceptionResult = perceptionSystem.perceive();

    // 2. Perception pipeline (attention filtering)
    PipelineOutput pipelineOutput = perceptionPipeline.process(perceptionResult);

    // 3. Perception fusion (multi-source integration)
    Perception fused = perceptionFusion.fuse(pipelineOutput.filteredPerception(), recentStimuli);

    // 4. World model building
    WorldUpdateResult worldUpdate = worldBuilder.build(fused, worldModel);
    worldModel = worldUpdate.updatedWorld();

    // 5. Self-reflection
    ReflectionResult reflection = selfReflector.reflect(selfModel, fused, worldModel);

    // 6. Self-model update if insight gained
    if (reflection.hasInsight()) {
        selfModel = selfReflector.applyInsight(selfModel, reflection);
    }

    // 7. Reasoning (LLM or heuristic)
    ReasoningResult reasoningResult = reasoningEngine.reason(reasoningContext);

    // 8. Store in memory
    memory.perceive(new Stimulus(...));

    // 9. Generate action recommendations
    ActionRecommendation actionRecommendation = generateActionRecommendation(...);

    return new CognitionResult(...);
}
```

---

## 4. Cognition Sub-System

### 4.1 PerceptionPipeline

Handles attention filtering and salience scoring.

**File**: `src/main/java/com/lingfeng/sprite/cognition/PerceptionPipeline.java`

**Responsibilities**:
- Three-channel confirmation (process whitelist, window change, time cooldown)
- Salience score calculation
- Low-salience filtering

### 4.2 PerceptionFusion

Merges multi-source perception into a unified view.

**File**: `src/main/java/com/lingfeng/sprite/cognition/PerceptionFusion.java`

**Fusion Strategies**:
- Time-weighted: Recent perceptions have higher weight (exponential moving average)
- Type fusion: Different perception types merged
- Conflict resolution: Latest data or higher confidence wins

### 4.3 WorldBuilder

Updates owner understanding from perception.

**File**: `src/main/java/com/lingfeng/sprite/cognition/WorldBuilder.java`

**Capabilities**:
- Emotional state inference from presence/context/system
- Habit learning from repeated behaviors
- Preference inference from action outcomes
- World knowledge construction (facts)
- Interaction history updates

### 4.4 SelfReflector

Active self-reflection engine.

**File**: `src/main/java/com/lingfeng/sprite/cognition/SelfReflector.java`

**Reflection Types**:
1. **Scheduled** - Every 10 minutes (configurable)
2. **Novel Situation** - New contexts/activities detected
3. **Owner Emotion** - High-intensity emotional states
4. **Environment Change** - Multiple simultaneous changes

**Reflection Output**:
- Primary insight content
- Owner thought inference ("主人在想什么")
- Growth event recording

### 4.5 ReasoningEngine

Inference engine with LLM interface.

**File**: `src/main/java/com/lingfeng/sprite/cognition/ReasoningEngine.java`

**Reasoning Types**:
- **Intent Recognition**: From behavior, infer true intent
- **Causal Reasoning**: Understand "why" events occur
- **Prediction**: Based on history, predict next steps

**Interface for LLM**:
```java
public interface LlmReasoner {
    CompletableFuture<Intent> inferIntent(IntentPrompt prompt);
    CompletableFuture<CausalChain> reasonCausal(CausalPrompt prompt);
    CompletableFuture<Prediction> predict(PredictionPrompt prompt);
    CompletableFuture<Insight> reflect(ReflectionPrompt prompt);
}
```

---

## 5. Services Layer

### 5.1 SpriteService

Spring-managed service that wraps Sprite lifecycle.

**File**: `src/main/java/com/lingfeng/sprite/service/SpriteService.java`

**Responsibilities**:
- Creates Sprite with real sensors (RealPlatformSensor, RealUserSensor, RealEnvironmentSensor)
- Orchestrates cognition cycle with memory consolidation and evolution
- Executes recommended actions via ActionExecutor
- Provides state accessors

**Spring Integration**:
```java
@Service
public class SpriteService {
    private final Sprite sprite;
    private final MemoryConsolidationService memoryConsolidationService;
    private final EvolutionService evolutionService;
    private final ActionExecutor actionExecutor;
}
```

### 5.2 ConversationService

Handles chat-based interaction with LLM integration.

**File**: `src/main/java/com/lingfeng/sprite/service/ConversationService.java`

**Responsibilities**:
- Manages conversation history per session
- Builds LLM context (self summary, owner summary, situation, tools, memory)
- Executes tool calls returned by LLM
- Formats responses combining LLM output with tool results

**Context Building**:
```java
LlmContext {
    selfSummary,        // "I am 小艺, my essence is..."
    ownerSummary,      // "Owner 灵锋 prefers direct communication..."
    currentSituation,  // "Memory 65%, CPU 25%, Window: VSCode..."
    chatHistory,       // Recent 10 messages
    availableTools,    // SearchFiles, Calculator, Remember, etc.
    memoryHighlights   // Working memory items
}
```

### 5.3 ActionExecutor

Plugin-based action execution system.

**File**: `src/main/java/com/lingfeng/sprite/service/ActionExecutor.java`

**Architecture**:
```java
@Service
public class ActionExecutor {
    private final Map<String, ActionPlugin> actionPlugins;

    // Built-in plugins
    // - LogAction: Records actions to log
    // - NotifyAction: Sends system notifications
}
```

**Execution Flow**:
1. Parse action string for type and parameters
2. Look up registered ActionPlugin
3. Execute with context parameters
4. Return ActionResult (success/failure with message)

### 5.4 MemoryConsolidationService

Handles memory tier transitions (sensory → working → long-term).

**File**: `src/main/java/com/lingfeng/sprite/service/MemoryConsolidationService.java`

**Responsibilities**:
- Pattern detection from sensory memory
- Consolidation to working memory
- Long-term storage decisions
- Memory pruning for retention policy

### 5.5 EvolutionService

Applies evolution engine results to update SelfModel.

**File**: `src/main/java/com/lingfeng/sprite/service/EvolutionService.java`

**Responsibilities**:
- Applies evolution results from EvolutionEngine
- Updates SelfModel capabilities, values, metacognition
- Records growth events
- Manages evolution level progression

---

## 6. Sensors

### 6.1 RealPlatformSensor

Uses OSHI library to collect real system metrics.

**File**: `src/main/java/com/lingfeng/sprite/sensor/RealPlatformSensor.java`

**Dependencies**:
- `oshi-core` (v6.1.6) - Cross-platform system info

**Collected Data**:
- CPU load (via tick counts)
- Memory usage (total, available)
- Disk size (via disk stores)
- Network status (connected/disconnected, adapter name)
- Hostname via `InetAddress.getLocalHost()`

**Fallback**: Returns simulated data if OSHI fails

### 6.2 RealUserSensor

User behavior sensor (currently placeholder for JNA integration).

**File**: `src/main/java/com/lingfeng/sprite/sensor/RealUserSensor.java`

**Note**: Full implementation requires JNA window tracking (see dependency `jna-platform` v5.14.0)

**Current Implementation**: Returns default active window with UNKNOWN app type

### 6.3 RealEnvironmentSensor

Time-based context inference.

**File**: `src/main/java/com/lingfeng/sprite/sensor/RealEnvironmentSensor.java`

**Context Inference Based on Hour**:
| Hour | Weekend | Weekday |
|------|---------|---------|
| 6-7  | Ritual  | Ritual  |
| 8-9  | Leisure | Work    |
| 10-12| Work    | Work    |
| 12-13| Meal    | Meal    |
| 14-17| Work    | Work    |
| 18-19| Commute | Commute |
| 20-21| Leisure | Leisure |
| 22-23| Ritual  | Ritual  |
| 0-5  | Sleep   | Sleep   |

---

## 7. LLM Integration

### 7.1 MinMaxLlmReasoner

Implementation of `LlmReasoner` interface using MinMax API.

**File**: `src/main/java/com/lingfeng/sprite/llm/MinMaxLlmReasoner.java`

**Capabilities**:
- Intent inference via chat completion
- Causal reasoning via structured prompts
- Prediction via structured prompts
- Self-reflection via structured prompts
- Unified chat thinking with tool call support

**HTTP Client**:
- Uses Apache HttpClient 5.x with connection pooling
- Calls MinMax `/text/chatcompletion_v2` endpoint

**Tool Call Parsing**:
```java
// Parses JSON tool calls from LLM response
{"tool_calls": [{"tool": "SearchFiles", "params": {"query": "..."}}]}
```

### 7.2 ChatModels (Shared Data Types)

Data models for chat-based LLM interaction.

**File**: `src/main/java/com/lingfeng/sprite/llm/ChatModels.java`

**Records**:
```java
LlmContext(selfSummary, ownerSummary, currentSituation, chatHistory, availableTools, memoryHighlights)
LlmThought(reasoning, response, insight, toolCalls, confidence)
ToolCall(tool, params)
```

---

## 8. Data Flow Diagrams

### Perception to Action Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         PERCEPTION → ACTION FLOW                              │
└──────────────────────────────────────────────────────────────────────────────┘

[Sensors]                    [PerceptionSystem]              [CognitionController]
    │                              │                                    │
    ▼                              ▼                                    │
┌─────────┐                 ┌─────────────┐                            │
│Platform │                 │  Combined   │                            │
│Sensor   │────────────────▶│ Perception  │                            │
└─────────┘                 └──────┬──────┘                            │
                                   │                                     │
                                   ▼                                     ▼
                           ┌─────────────┐              ┌───────────────────────┐
                           │ Attention   │              │    Cognition Cycle     │
                           │ Mechanism   │─────────────▶│                       │
                           │ (Salience)  │              │ 1. PerceptionPipeline │
                           └─────────────┘              │ 2. PerceptionFusion   │
                                                      │ 3. WorldBuilder        │
                                                      │ 4. SelfReflector       │
                                                      │ 5. ReasoningEngine     │
                                                      │ 6. ActionRecommendation│
                                                      └───────────┬───────────┘
                                                                  │
                                                                  ▼
                                              ┌───────────────────────────────┐
                                              │        ActionExecutor         │
                                              │                               │
                                              │  ┌─────────┐  ┌─────────┐   │
                                              │  │LogAction│  │Notify   │   │
                                              │  └─────────┘  └─────────┘   │
                                              │  ┌─────────┐  ┌─────────┐   │
                                              │  │Search   │  │Calculator│   │
                                              │  └─────────┘  └─────────┘   │
                                              └───────────────────────────────┘
```

### Memory Consolidation Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        MEMORY CONSOLIDATION FLOW                             │
└──────────────────────────────────────────────────────────────────────────────┘

 ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
 │   SENSORY    │ ───▶ │   WORKING    │ ───▶ │   LONG-TERM  │
 │   MEMORY     │      │   MEMORY     │      │   MEMORY     │
 │  (30s win)   │      │  (7 items)   │      │  (Persistent)│
 └──────────────┘      └──────────────┘      └──────────────┘
        │                     │                     │
        │ Pattern detected    │ Threshold reached    │ Consolidate
        │ (3+ same type)     │ (relevance low)     │ important
        ▼                    ▼                     ▼
 ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
 │  Detect      │      │   Prune      │      │  Store as   │
 │  Patterns    │      │   LRU        │      │  Episodic/  │
 │              │      │   Items      │      │  Semantic/   │
 │              │      │              │      │  Procedural  │
 └──────────────┘      └──────────────┘      └──────────────┘
```

### Evolution Feedback Loop

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         EVOLUTION FEEDBACK LOOP                               │
└──────────────────────────────────────────────────────────────────────────────┘

        ┌─────────────────────────────────────────────────────┐
        │                    Sprite                            │
        │                                                     │
        │  ┌─────────────┐    ┌─────────────┐               │
        │  │   Action    │───▶│   Record    │               │
        │  │  Executor   │    │  Feedback   │               │
        │  └─────────────┘    └──────┬──────┘               │
        │                             │                       │
        └────────────────────────────┼───────────────────────┘
                                     │
                                     ▼
        ┌─────────────────────────────────────────────────────┐
        │                 EvolutionEngine                       │
        │                                                     │
        │  ┌─────────────┐    ┌─────────────┐    ┌─────────┐  │
        │  │  Feedback   │───▶│  Learning   │───▶│  Self   │  │
        │  │  Collector  │    │    Loop     │    │ Modifier│  │
        │  │             │    │             │    │         │  │
        │  │ - Owner     │    │ Observe     │    │ Update  │  │
        │  │ - Outcome   │    │ Reflect     │    │ Values  │  │
        │  │ - SelfRev   │    │ Abstract    │    │ Caps    │  │
        │  │ - Pattern   │    │ Apply       │    │ Beliefs │  │
        │  └─────────────┘    └─────────────┘    └─────────┘  │
        └─────────────────────────────────────────────────────┘
                                     │
                                     ▼
        ┌─────────────────────────────────────────────────────┐
        │                    SelfModel                         │
        │                                                     │
        │  Updated:                                           │
        │  - capabilities (improved levels)                   │
        │  - values (adjusted weights)                       │
        │  - metacognition (new reflections)                 │
        │  - growthHistory (recorded events)                 │
        │  - evolutionLevel (periodic increase)               │
        └─────────────────────────────────────────────────────┘
```

---

## 9. Planned Improvements

### 9.1 UnifiedContextService (To Be Created)

A centralized service to manage context across all components:

**Planned Location**: `src/main/java/com/lingfeng/sprite/service/UnifiedContextService.java`

**Responsibilities**:
- Single source of truth for current perception, self, and world state
- Event-based updates to subscribers
- Context versioning for audit trails
- Thread-safe access to shared state

**Design**:
```java
@Service
public class UnifiedContextService {
    // Current context snapshot
    private volatile ContextSnapshot currentContext;

    // Publish context updates
    public void updateContext(Perception perception, SelfModel.Self self, WorldModel.World world);

    // Subscribe to changes
    public void subscribe(ContextListener listener);

    // Get current state
    public ContextSnapshot getCurrentContext();
}
```

### 9.2 Memory Bean (Spring Integration)

Memory system needs proper Spring bean management:

**Issues**:
- `ConversationService` has `memory = null` because MemorySystem.Memory isn't available as Spring bean
- Memory consolidation service needs better integration

**Planned**:
```java
@Configuration
public class MemoryConfig {
    @Bean
    public MemorySystem.Memory memory() {
        return new MemorySystem.Memory();
    }
}
```

### 9.3 RealUserSensor Enhancement

**Current**: Returns placeholder data

**Planned**: JNA-based window tracking implementation
- Active window title
- Process name
- Window class name
- Focus detection

### 9.4 Enhanced Perception Types

**Planned Additional Sensors**:
- `DesktopSensor` - Screen content, wallpaper, icon layout
- `ProcessSensor` - Running processes with resource usage
- `DigitalSensor` - File changes, email, notifications

### 9.5 LLM Multi-Model Support

**Planned**:
- Interface abstraction for multiple LLM providers
- Claude, OpenAI, local model implementations
- Model selection based on task complexity

---

## 10. File Structure

```
src/main/java/com/lingfeng/sprite/
├── Sprite.java                      # Core digital being
├── SelfModel.java                   # Self identity and metacognition
├── WorldModel.java                  # Owner and environment model
├── PerceptionSystem.java            # Multi-sensor perception
├── MemorySystem.java                # Three-tier memory
├── EvolutionEngine.java             # Self-improvement feedback loop
│
├── cognition/
│   ├── CognitionController.java     # Perceive-Cognition-Act orchestration
│   ├── PerceptionPipeline.java      # Attention filtering
│   ├── PerceptionFusion.java        # Multi-source fusion
│   ├── WorldBuilder.java            # World model updates
│   ├── SelfReflector.java           # Active self-reflection
│   └── ReasoningEngine.java         # Intent/causal/prediction reasoning
│
├── service/
│   ├── SpriteService.java            # Spring lifecycle management
│   ├── ConversationService.java     # Chat handling with LLM
│   ├── ActionExecutor.java          # Plugin-based action execution
│   ├── MemoryConsolidationService.java
│   └── EvolutionService.java
│
├── sensor/
│   ├── RealPlatformSensor.java      # OSHI-based system metrics
│   ├── RealUserSensor.java          # Window tracking (placeholder)
│   └── RealEnvironmentSensor.java  # Time-based context
│
├── llm/
│   ├── MinMaxLlmReasoner.java       # MinMax API integration
│   ├── MinMaxConfig.java            # API configuration
│   ├── LlmConfig.java               # LLM settings
│   └── ChatModels.java              # Shared data types
│
├── action/
│   ├── ActionPlugin.java            # Action interface
│   ├── ActionResult.java            # Execution result
│   └── Actions/
│       ├── LogAction.java
│       └── NotifyAction.java
│
└── config/
    ├── AppConfig.java
    └── WebSocketConfig.java
```

---

## Appendix: Key Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.2.3 | Framework |
| OSHI | 6.1.6 | System metrics |
| JNA | 5.14.0 | Window tracking (planned) |
| Jackson | 2.17.0 | JSON processing |
| Apache HttpClient | 5.3 | HTTP calls to LLM |
| SLF4J | 2.0.12 | Logging |

---

## Appendix: Configuration Constants

| Constant | Value | Location |
|----------|-------|----------|
| SENSORY_WINDOW_SECONDS | 30 | MemorySystem |
| WORKING_MEMORY_MAX_ITEMS | 7 | MemorySystem |
| LONG_TERM_RETENTION_DAYS | 365 | MemorySystem |
| ATTENTION_MAX_CONCURRENT | 3 | PerceptionSystem |
| ATTENTION_COOLDOWN_SECONDS | 300 | PerceptionSystem |
| REFLECTION_INTERVAL_MINUTES | 10 | SelfReflector |
| EVOLUTION_MAX_GROWTH_RATE | 1.1 | EvolutionEngine |
