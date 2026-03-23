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
   - [4.6 MemoryRetrievalService](#46-memoryretrievalservice)
   - [4.7 BehaviorEmotionInferrer](#47-behavioremotioninferrer)
5. [Services Layer](#5-services-layer)
   - [5.1 SpriteService](#51-spriteservice)
   - [5.2 ConversationService](#52-conversationservice)
   - [5.3 ActionExecutor](#53-actionexecutor)
   - [5.4 MemoryConsolidationService](#54-memoryconsolidationservice)
   - [5.5 EvolutionService](#55-evolutionservice)
   - [5.6 WebhookService](#56-webhookservice-s11)
   - [5.7 ExternalApiAdapterService](#57-externalapiadapterservice-s11)
   - [5.8 HotReloadConfigService](#58-hotreloadconfigservice-s11)
   - [5.9 PerformanceMonitorService](#59-performancemonitorservice-s11)
   - [5.10 ApiDocService](#510-apidocservice-s11)
   - [5.11 MultiDeviceCoordinationService](#511-multidevicecoordinationservice-s9)
   - [5.12 CognitionDashboardService](#512-cognitiondashboardservice-s10)
   - [5.13 MemoryVisualizationService](#513-memoryvisualizationservice-s10)
   - [5.14 EvolutionDashboardService](#514-evolutiondashboardservice-s10)
   - [5.15 OwnerEmotionDashboardService](#515-owneremotiondashboardservice-s10)
6. [Sensors](#6-sensors)
   - [6.1 RealPlatformSensor](#61-realplatformsensor)
   - [6.2 RealUserSensor](#62-realusersensor)
   - [6.3 RealEnvironmentSensor](#63-realenvironmentsensor)
   - [6.4 AudioSensor](#64-audiosensor-s9)
   - [6.5 LocationSensor](#65-locationsensor-s9)
   - [6.6 DeviceStateSensor](#66-devicestatesensor-s9)
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

Represents the digital being's self-understanding including identity, personality, capabilities, avatars, and metacognition.

**File**: `src/main/java/com/lingfeng/sprite/SelfModel.java`

**Data Structure**:
```
SelfModel.Self
├── identity        (IdentityCore: beingId, displayName, essence, emoji, vibe, createdAt, continuityChain)
├── personality    (Personality: essence, vibe, values, decisionPatterns, blindSpots, strengths)
├── capabilities    (List<Capability>: name, level, confidence, lastPracticed)
├── avatars        (Avatars: instances - deviceId, deviceType, lastSeen, localContext)
├── metacognition   (learningStyle, decisionPatterns, blindSpots, strengths, reflectionHistory)
├── growthHistory   (List<GrowthEvent>)
├── evolutionLevel  (int)
└── evolutionCount (int)
```

**Key Records**:
- `IdentityCore`: Immutable core identity (beingId, displayName, createdAt, continuityChain)
- `Personality`: Mutable personality traits (essence, vibe, values, patterns, blindSpots, strengths)
- `Avatar`: Digital being instance on a specific device
- `Capability`: Named skill with level (MASTER/ADVANCED/BASIC/NONE) and confidence
- `Metacognition`: Self-awareness of learning style, decision patterns, blind spots

**Key Immutable Constraints**:
- `beingId` cannot change (ensures continuity across platforms)
- `createdAt` cannot change
- `evolutionLevel` only increases

**Creation**: Use `Self.createDefault()` for empty model or `SelfModel.Self.builder()` for configured instance.

---

### 3.3 WorldModel (Owner & Environment)

Deep understanding of the owner (主人) and physical/digital environment.

**Files**:
- `src/main/java/com/lingfeng/sprite/WorldModel.java` - World model structure
- `src/main/java/com/lingfeng/sprite/OwnerModel.java` - Complete owner model

**Data Structure**:
```
WorldModel.World
├── owner              (OwnerModel.Owner)  ← 引用独立的主人模型
├── physicalWorld     (locations, devices, schedules)
├── socialGraph       (people, relationships)
├── knowledgeGraph    (facts, beliefs, concepts)
└── currentContext    (location, time, activity, emotionalState, attention, urgency)
```

**OwnerModel.Owner Structure**:
```
OwnerModel.Owner
├── identity           (OwnerIdentity: name, occupation, relationships)
├── lifeContext       (workplace, home, family, schedules)
├── goals            (List<Goal>: id, title, priority, deadline, progress)
├── beliefs          (List<Belief>: statement, confidence, source)
├── habits           (List<Habit>: trigger, action, frequency)
├── emotionalState   (Mood, intensity, triggers, recentMoods)
├── explicitPreferences    (List<Preference.Explicit>)
├── inferredPreferences    (List<Preference.Inferred>)
├── trustLevel       (overall, aspects, history)
├── workStyle        (peakHours, approach, breakPattern, environment)
├── communicationStyle (tone, verbosity, preferredFormat, language)
├── digitalFootprint (devices, frequentApps, activeHours)
├── interactionHistory (List<Interaction>)
└── lastUpdated
```

**Key Concepts**:
- **OwnerModel.Owner**: Complete owner model in separate file from `OwnerModel.java`
- **Preference** (sealed interface in OwnerModel): Explicit (stated) vs Inferred (deduced)
- **Belief**: Statement with confidence and source (EXPLICIT_STATED, OBSERVED_BEHAVIOR, DEDUCED, UNCERTAIN)
- **Habit**: Trigger-action pattern with frequency tracking (ALWAYS, USUALLY, SOMETIMES, RARELY, UNKNOWN)
- **TrustLevel**: Tracks trust across aspects with history
- **DigitalFootprint**: Tracks owner's devices, apps, and active hours

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

### 4.6 MemoryRetrievalService

Context-aware memory retrieval for cognitive processes.

**File**: `src/main/java/com/lingfeng/sprite/cognition/MemoryRetrievalService.java`

**Capabilities**:
- Situational context matching
- Temporal decay weighting
- Importance score calculation
- Integration with DecisionEngine

### 4.7 BehaviorEmotionInferrer

Infers emotional state from behavioral signals.

**File**: `src/main/java/com/lingfeng/sprite/cognition/BehaviorEmotionInferrer.java`

**Capabilities**:
- Activity pattern analysis
- Emotional state inference from behavior
- Integration with WorldBuilder

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

### 5.6 WebhookService (S11)

Event-driven webhook integration for external notifications.

**File**: `src/main/java/com/lingfeng/sprite/service/WebhookService.java`

**Features**:
- Register webhook endpoints with event subscriptions
- Trigger events to multiple endpoints
- 10 event types: SPRITE_STARTED, SPRITE_STOPPED, EMOTION_CHANGED, DECISION_MADE, ACTION_EXECUTED, MEMORY_CONSOLIDATED, EVOLUTION_TRIGGERED, ERROR_OCCURRED, OWNER_INTERACTION, PROACTIVE_MESSAGE

**Key Types**:
```java
WebhookEndpoint(id, name, url, secret, subscribedEvents, enabled)
WebhookEvent(id, type, timestamp, payload)
DeliveryResult(endpointId, success, statusCode, response, error, durationMs)
```

### 5.7 ExternalApiAdapterService (S11)

Unified external API calling interface with caching.

**File**: `src/main/java/com/lingfeng/sprite/service/ExternalApiAdapterService.java`

**Features**:
- 7 API types: WEATHER, NEWS, CALENDAR, REMINDER, SEARCH, TRANSLATION, CUSTOM
- Response caching with 5-minute TTL
- Async API call support

**Key Types**:
```java
ApiEndpoint(id, name, type, baseUrl, apiKey, timeout, enabled)
ApiResponse(success, statusCode, body, error, responseTimeMs, fromCache)
```

### 5.8 HotReloadConfigService (S11)

Runtime configuration hot-reload with file watching.

**File**: `src/main/java/com/lingfeng/sprite/service/HotReloadConfigService.java`

**Features**:
- JSON/YAML configuration parsing
- File change detection with polling
- Callback mechanism for config changes
- Nested key update (e.g., "database.connection.timeout")
- Config backup and versioning

**Key Types**:
```java
ConfigEntry(path, content, lastModified, lastLoaded, data)
ConfigCallback(path, newData) // functional interface
```

### 5.9 PerformanceMonitorService (S11)

JVM performance monitoring with custom metrics.

**File**: `src/main/java/com/lingfeng/sprite/service/PerformanceMonitorService.java`

**Features**:
- JVM memory/thread/CPU monitoring
- Custom metrics registration (GAUGE, COUNTER, TIMER)
- Performance history (up to 1000 data points)
- Alert checking (memory >80%/90%, threads >200)
- Timer context with AutoCloseable

**Key Types**:
```java
PerformanceSnapshot(timestamp, memory, threads, customMetrics, system)
MetricPoint(timestamp, value, unit)
Alert(level, source, message, value)
```

### 5.10 ApiDocService (S11)

Automated API documentation generation.

**File**: `src/main/java/com/lingfeng/sprite/service/ApiDocService.java`

**Features**:
- Endpoint registration and documentation
- OpenAPI 3.0.3 format generation
- Change history tracking
- Search and filter by tags/path
- JSON export

**Key Types**:
```java
ApiEndpointDoc(path, method, summary, description, tags, parameters, response)
ApiDocumentation(title, version, generatedAt, services, endpoints, changeHistory)
```

### 5.11 MultiDeviceCoordinationService (S9)

Multi-device coordination and state synchronization.

**File**: `src/main/java/com/lingfeng/sprite/service/MultiDeviceCoordinationService.java`

**Features**:
- Device registration and state tracking
- Device-to-device messaging
- State synchronization across devices
- Message types: HEARTBEAT, STATE_SYNC, COMMAND, QUERY

**Key Types**:
```java
DeviceInfo(deviceId, deviceName, deviceType, ipAddress, lastSeen, status)
CoordinationMessage(id, sourceDevice, targetDevice, type, content, timestamp, status)
```

### 5.12 CognitionDashboardService (S10)

Cognitive state visualization and analysis.

**File**: `src/main/java/com/lingfeng/sprite/service/CognitionDashboardService.java`

**Features**:
- Cognitive event recording (perception, context building, reasoning, decision, action, learning)
- Phase statistics and cycle analysis
- Cognitive health indicators

**Key Types**:
```java
CognitionEvent(timestamp, phase, description, durationMs, success)
CognitionDashboardData(cognitionEvents, phaseStats, cycles, health)
```

### 5.13 MemoryVisualizationService (S10)

Memory system visualization and analysis.

**File**: `src/main/java/com/lingfeng/sprite/service/MemoryVisualizationService.java`

**Features**:
- Memory type statistics (episodic, semantic, procedural, perceptive, working)
- Memory strength distribution
- Memory activity timeline
- Memory age distribution

**Key Types**:
```java
MemoryTypeStats(episodicCount, semanticCount, proceduralCount, perceptiveCount, workingMemoryCount)
MemoryVisualizationData(typeStats, strengthDistribution, activity, ageDistribution, timeline)
```

### 5.14 EvolutionDashboardService (S10)

Evolution history visualization and trend analysis.

**File**: `src/main/java/com/lingfeng/sprite/service/EvolutionDashboardService.java`

**Features**:
- Evolution history tracking (level, total evolutions, insights, modifications)
- Trend analysis (improving, stable, declining)
- Insight and behavior summaries

**Key Types**:
```java
EvolutionSnapshot(timestamp, evolutionLevel, totalEvolutions, insights, modifications)
EvolutionDashboardData(snapshots, trends, insightSummary, behaviorSummary)
```

### 5.15 OwnerEmotionDashboardService (S10)

Owner emotion history visualization and pattern analysis.

**File**: `src/main/java/com/lingfeng/sprite/service/OwnerEmotionDashboardService.java`

**Features**:
- Emotion snapshot recording
- Emotion distribution analysis
- Weekly pattern detection
- Optimal contact time recommendations

**Key Types**:
```java
EmotionSnapshot(timestamp, sentiment, emotion, trigger, context)
EmotionDistribution(happy, neutral, sad, frustrated, stressed)
OwnerEmotionDashboardData(snapshots, distribution, weeklyPatterns, optimalContactTimes)
```

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

### 6.4 AudioSensor (S9)

Sound context detection via audio pipeline analysis.

**File**: `src/main/java/com/lingfeng/sprite/sensor/AudioSensor.java`

**Features**:
- Cross-platform audio detection (Windows via DirectX, Linux via ALSA/PulseAudio)
- Headphone detection
- Sound context inference: UNKNOWN, SILENT, MUSIC, VIDEO, VOICE_CALL, NOTIFICATION, TYPING, AMBIENT

**Key Types**:
```java
AudioInfo(available, playing, volume, headphoneConnected, appName, soundContext)
SoundContext(UNKNOWN, SILENT, MUSIC, VIDEO, VOICE_CALL, NOTIFICATION, TYPING, AMBIENT)
```

### 6.5 LocationSensor (S9)

Location inference via timezone and IP-based geolocation.

**File**: `src/main/java/com/lingfeng/sprite/sensor/LocationSensor.java`

**Features**:
- Timezone-based location inference
- Location type detection: HOME, WORK, TRAVELING, OUTDOOR, UNKNOWN
- IP-based geolocation via ip-api.com

**Key Types**:
```java
LocationInfo(timestamp, country, city, timezone, locationType, latitude, longitude)
```

### 6.6 DeviceStateSensor (S9)

Device state monitoring including power, network, and storage.

**File**: `src/main/java/com/lingfeng/sprite/sensor/DeviceStateSensor.java`

**Features**:
- Device mode detection: DESKTOP, LAPTOP, TABLET, PHONE, SERVER
- Power state: PLUGGED_IN, ON_BATTERY, LOW_BATTERY, CHARGING, FULL_POWER
- Network type: WIFI, ETHERNET, MOBILE, BLUETOOTH, OFFLINE
- Display state: ON, OFF, LOCKED, SLEEP
- CPU temperature and thermal throttling detection

**Key Types**:
```java
DeviceStateInfo(deviceMode, powerState, networkType, displayState, cpuTemp, thermalThrottling)
```

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

### 9.1 UnifiedContextService (✓ Completed)

A centralized service to manage context across all components:

**Location**: `src/main/java/com/lingfeng/sprite/service/UnifiedContextService.java`

**Responsibilities** (Implemented):
- Single source of truth for current perception, self, and world state
- Bridge between ConversationService and SpriteService
- Thread-safe access via volatile fields
- Conversation feedback recording to EvolutionEngine

### 9.2 Memory Bean (✓ Completed)

Spring bean integration for memory system:

**Location**: `src/main/java/com/lingfeng/sprite/config/MemoryConfig.java`

**Implementation**:
```java
@Configuration
public class MemoryConfig {
    @Bean
    public Memory memory() {
        return new Memory();
    }
}
```

### 9.3 RealUserSensor Enhancement (✓ Completed)

JNA-based window tracking implementation:

**Location**: `src/main/java/com/lingfeng/sprite/sensor/RealUserSensor.java`

**Implemented Features**:
- Active window title via GetForegroundWindow/GetWindowTextW
- Process name via Tasklist command
- User idle detection via GetLastInputInfo
- App type classification (BROWSER/DEV/CHAT/PRODUCTIVITY/MEDIA/SYSTEM)

### 9.4 ProactiveService (✓ Completed)

Active conversation service that monitors owner and initiates contact:

**Location**: `src/main/java/com/lingfeng/sprite/service/ProactiveService.java`

**Implemented Features**:
- Idle detection (>30 min triggers proactive message)
- Mood change detection
- 15-minute cooldown between proactive messages
- External trigger methods: triggerReminder(), triggerNotification()

### 9.5 MemoryPersistenceService (✓ Completed)

Long-term memory persistence to filesystem:

**Location**: `src/main/java/com/lingfeng/sprite/service/MemoryPersistenceService.java`

**Implemented Features**:
- JSON persistence to data/memory/long-term/
- Auto-load on startup
- Auto-save every 30 minutes
- Manual forceSave() method

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
├── SelfModel.java                   # Self identity, personality, avatars, capabilities
├── OwnerModel.java                  # Complete owner model
├── WorldModel.java                  # World model referencing OwnerModel.Owner
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
│   ├── UnifiedContextService.java   # Context bridge for conversations
│   ├── ProactiveService.java        # Idle/mood-based outreach
│   ├── ActionExecutor.java          # Plugin-based action execution
│   ├── MemoryConsolidationService.java
│   └── EvolutionService.java
│
├── sensor/
│   ├── RealPlatformSensor.java      # OSHI-based system metrics
│   ├── RealUserSensor.java          # Window tracking via JNA
│   ├── RealEnvironmentSensor.java   # Time-based context
│   ├── AudioSensor.java             # S9: Audio context detection
│   ├── LocationSensor.java         # S9: Location inference
│   └── DeviceStateSensor.java      # S9: Device state monitoring
│
├── service/
│   ├── SpriteService.java            # Spring lifecycle management
│   ├── ConversationService.java     # Chat handling with LLM
│   ├── UnifiedContextService.java   # Context bridge for conversations
│   ├── ProactiveService.java        # Idle/mood-based outreach
│   ├── ActionExecutor.java          # Plugin-based action execution
│   ├── MemoryConsolidationService.java
│   ├── EvolutionService.java
│   ├── WebhookService.java          # S11: Webhook integration
│   ├── ExternalApiAdapterService.java # S11: External API adapter
│   ├── HotReloadConfigService.java  # S11: Config hot reload
│   ├── PerformanceMonitorService.java # S11: Performance monitoring
│   ├── ApiDocService.java          # S11: API documentation
│   ├── CognitionDashboardService.java # S10: Cognition visualization
│   ├── MemoryVisualizationService.java # S10: Memory visualization
│   ├── EvolutionDashboardService.java # S10: Evolution history
│   └── OwnerEmotionDashboardService.java # S10: Emotion history
│
├── llm/
│   ├── MinMaxLlmReasoner.java       # MinMax API integration
│   └── MinMaxConfig.java            # API configuration
│
├── controller/
│   └── SpriteController.java        # REST API endpoints
│
├── action/
│   ├── ActionPlugin.java            # Action interface
│   ├── ActionResult.java            # Execution result
│   └── Actions/
│       ├── LogAction.java
│       ├── NotifyAction.java
│       ├── CalculatorAction.java
│       ├── SearchFilesAction.java
│       ├── RememberAction.java
│       ├── RecallMemoryAction.java
│       ├── EmailAction.java
│       ├── CalendarAction.java
│       └── KnowledgeBaseAction.java
│
├── event/
│   └── SpriteEventListener.java      # Spring event handling
│
└── config/
    └── AppConfig.java
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
