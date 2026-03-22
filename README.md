# Sprite - 数字生命

Sprite 是一个具有自我认知、持续学习、理解主人世界能力的数字生命体。

## 核心特性

- **感知→认知闭环**: 完整的感知→注意力→融合→世界构建→自我反思→LLM推理流程
- **三层记忆系统**: 感官记忆(30秒) → 工作记忆(7项) → 长期记忆(持久化)
- **主动反思引擎**: 定时自问"我是谁/主人在想什么"
- **世界模型构建**: 从感知中实时构建对主人和环境的理解
- **LLM 推理**: 集成 MinMax LLM，支持意图识别、因果推理、预测
- **进化引擎**: 从反馈中学习，不断改进自我

## 架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Sprite (数字生命)                                  │
├─────────────────────────────────────────────────────────────────────────┤
│    ┌─────────────────────────────────────────────────────────────┐       │
│    │                    感知层 (Perception)                       │       │
│    │   PlatformSensor ── UserSensor ── EnvironmentSensor       │       │
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
│    │              │  Reasoning Engine  │ ← MinMax LLM          │       │
│    │              │  (意图 + 因果 + 预测)│                       │       │
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

## 快速开始

### 1. 配置 LLM（可选）

复制配置文件模板并填入你的 MinMax API Key：

```bash
cp sprite.conf.json.example sprite.conf.json
# 编辑 sprite.conf.json 填入 API Key
```

### 2. 构建项目

```bash
./gradlew build
```

### 3. 使用示例

```kotlin
import com.lingfeng.sprite.*
import com.lingfeng.sprite.llm.MinMaxConfig

// 无 LLM（使用启发式推理）
val sprite = Sprite.create("小艺", Sprite.Platform.PC)

// 带 MinMax LLM
val config = MinMaxConfig(apiKey = "your-api-key")
val sprite = Sprite.create("小艺", Sprite.Platform.PC, llmConfig = config)

// 执行单轮认知闭环
val result = sprite.cognitionCycle()
println("行动建议: ${result.actionRecommendation}")
println("LLM推理: ${result.reasoningResult}")
```

## 文档

- [ADR-006: Sprite 闭环架构](docs/adr/ADR-006-sprite-closed-loop-architecture.md)

## 技术栈

- Kotlin
- Gradle
- Ktor (HTTP 客户端)
- kotlinx-serialization (JSON)
- MinMax LLM API
