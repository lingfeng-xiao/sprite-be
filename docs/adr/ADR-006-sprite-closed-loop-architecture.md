# ADR-006: Sprite 闭环架构

## Status

Accepted (2026-03-22)

## Context

ADR-005 定义了 Digital Being MVP 架构，但经过深度分析发现三个核心差距：

1. **感知→认知闭环缺失** - 各模块独立运转，没有形成闭环
2. **世界模型是"空壳"** - 数据结构存在，但没有构建逻辑
3. **跨平台是"复制"不是"同步"** - 只有迁移快照，没有实时同步

本 ADR 定义 Sprite（Digital Being v2）的闭环架构，解决感知→认知→行动的闭环问题。

## Decision

### 1. 整体架构

Sprite 采用五层架构：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Sprite (数字生命)                                  │
├─────────────────────────────────────────────────────────────────────────┤
│    ┌─────────────────────────────────────────────────────────────┐       │
│    │                    感知层 (Perception)                       │       │
│    │   PlatformSensor ── UserSensor ── EnvironmentSensor       │       │
│    │              │                                      │                    │
│    │              └───────────────┬──────────────┘                  │       │
│    │                              ▼                                   │       │
│    │              ┌─────────────────────┐                       │       │
│    │              │  Attention Pipeline │ ← 三重通道确认        │       │
│    │              └─────────────────────┘                       │       │
│    └─────────────────────────────────────────────────────────────┘       │
│                              │                                          │
│                              ▼                                          │
│    ┌─────────────────────────────────────────────────────────────┐       │
│    │                    认知层 (Cognition)                        │       │
│    │   ┌──────────┐    ┌──────────┐    ┌──────────────┐       │       │
│    │   │Perception│───→│ World    │───→│ Self         │       │       │
│    │   │  Fusion  │    │ Builder  │    │ Reflector    │       │       │
│    │   └──────────┘    └──────────┘    └──────────────┘       │       │
│    │                           ▼                                │       │
│    │              ┌─────────────────────┐                       │       │
│    │              │  Reasoning Engine │ ← 预留LLM接口         │       │
│    │              └─────────────────────┘                       │       │
│    └─────────────────────────────────────────────────────────────┘       │
│                              │                                          │
│                              ▼                                          │
│    ┌─────────────────────────────────────────────────────────────┐       │
│    │                    记忆层 (Memory)                            │       │
│    │   Sensory (30s) → Working (7) → LongTerm (Persistent)      │       │
│    └─────────────────────────────────────────────────────────────┘       │
│                              │                                          │
│                              ▼                                          │
│    ┌─────────────────────────────────────────────────────────────┐       │
│    │                    进化层 (Evolution)                        │       │
│    │   FeedbackCollector → LearningLoop → SelfModifier           │       │
│    └─────────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2. 感知→认知闭环流程

```
感知输入
    ↓
PerceptionPipeline (注意力过滤)
    ↓
PerceptionFusion (多源感知融合)
    ↓
WorldBuilder (世界模型构建)
    ↓
SelfReflector (主动反思)
    ↓
ReasoningEngine (推理 - 预留LLM接口)
    ↓
SelfModel + WorldModel 更新
    ↓
ActionRecommendation 输出
```

### 3. 核心组件

#### 3.1 PerceptionPipeline - 感知管道

**职责**：感知输入 → 注意力过滤

**实现**：
- 三重通道确认：进程白名单 + 窗口变化 + 时间冷却
- 显著性评分：新颖性×0.2 + 相关性×0.3 + 紧迫性×0.3 + 情感×0.2
- 阈值过滤：低于 0.5 显著性的感知被标记为非显著

**接口**：
```kotlin
class PerceptionPipeline {
    fun process(perceptionResult: PerceptionResult): PipelineOutput
    fun isOnCooldown(actionType: String): Boolean
    fun recordAction(actionType: String)
}
```

#### 3.2 PerceptionFusion - 感知融合

**职责**：多源感知 → 统一输入

**实现**：
- 时序加权融合（指数移动平均）
- 当前感知权重 70%，历史 30%
- 不同感知类型使用不同融合策略

**接口**：
```kotlin
class PerceptionFusion {
    fun fuse(current: Perception, history: List<Stimulus>): Perception
}
```

#### 3.3 WorldBuilder - 世界模型构建

**职责**：感知 → 主人理解 + 世界知识

**实现**：
- 主人情感状态更新
- 习惯学习（从重复行为中提取）
- 偏好推断（从行动结果中推断）
- 世界知识构建（感知 → 事实）

**接口**：
```kotlin
class WorldBuilder {
    fun build(perception: Perception, currentWorld: World): WorldUpdateResult
}
```

#### 3.4 SelfReflector - 主动反思引擎

**职责**：定时自问"我是谁/主人在想什么"

**实现**：
- 定时反思触发（每10分钟）
- 新情境反思（检测到新情况时）
- 主人情绪反思（情绪强度 > 0.6 时）
- 环境变化反思（多维度变化时）

**接口**：
```kotlin
class SelfReflector {
    fun reflect(selfModel: Self, perception: Perception, worldModel: World): ReflectionResult
    fun applyInsight(selfModel: Self, reflection: ReflectionResult): Self
}
```

#### 3.5 ReasoningEngine - 推理引擎（预留LLM接口）

**职责**：意图识别、因果推理、预测

**实现原则**：
- LLM 接口抽象化，不绑定具体实现
- 支持 Claude API、OpenAI、本地模型等实现
- 无 LLM 时使用启发式推理降级

**接口**：
```kotlin
interface LlmReasoner {
    suspend fun inferIntent(prompt: IntentPrompt): Intent
    suspend fun reasonCausal(prompt: CausalPrompt): CausalChain
    suspend fun predict(prompt: PredictionPrompt): Prediction
    suspend fun reflect(prompt: ReflectionPrompt): Insight
}

class ReasoningEngine(llmReasoner: LlmReasoner?) {
    fun reason(context: ReasoningContext): ReasoningResult
}
```

### 4. CognitionController - 认知协调器

**职责**：管理感知→认知完整管道，协调各组件

**实现**：
```kotlin
class CognitionController {
    fun 认知Cycle(): CognitionResult
    fun getCurrentState(): CognitionState
    fun getStats(): CognitionStats
}
```

### 5. Sprite 主入口

**职责**：整合所有组件，提供统一入口

**实现**：
```kotlin
class Sprite {
    companion object {
        fun create(name: String, platform: Platform): Sprite
        fun createGuanGuan(): Sprite
    }

    fun cognitionCycle(): CognitionResult
    fun recordFeedback(type: FeedbackType, content: String, outcome: String)
    fun evolve(): EvolutionResult
    fun getState(): State
}
```

## Consequences

### Positive

1. **闭环形成**：感知→认知→行动→反馈完整闭环，每轮都是完整流程
2. **世界模型有活力**：WorldBuilder 实时从感知构建世界理解
3. **主动反思**：SelfReflector 定时主动反思，而非被动等待
4. **LLM 接口预留**：未来可接入各种 LLM 实现

### Negative

1. **复杂度增加**：相比 MVP，增加 5 个新组件
2. **性能开销**：每轮需要经过多个处理阶段

### Neutral

1. **Phase 1 聚焦感知→认知闭环，暂不包含跨平台同步**
2. **跨平台同步将在 Phase 3 实现（使用 CRDT）**

## Implementation Status

### Phase 1: 感知→认知闭环 ✅ (本版本实现)

- [x] PerceptionPipeline
- [x] PerceptionFusion
- [x] WorldBuilder
- [x] SelfReflector
- [x] CognitionController
- [x] Sprite 主入口
- [ ] 闭环验证测试

### Phase 2: 推理能力（预留接口）

- [ ] LlmReasoner 接口定义
- [ ] ReasoningEngine 接入 LlmReasoner
- [ ] 意图识别实现
- [ ] 因果推理实现

### Phase 3: 跨平台同步（后续版本）

- [ ] CRDTState
- [ ] VectorClock
- [ ] SyncEngine
- [ ] SyncProtocol

## References

- [ADR-005: Digital Being MVP 架构](ADR-005-digital-being-architecture.md)
- [Sprite 架构重构设计](../plans/sparkling-watching-sutherland.md)
