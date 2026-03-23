# Sprite - 数字生命

Sprite 是一个具有自我认知、持续学习、理解主人世界能力的数字生命体。

## 核心特性

- **感知→认知闭环**: 完整的感知→注意力→融合→世界构建→自我反思→LLM推理流程
- **三层记忆系统**: 感官记忆(30秒) → 工作记忆(7项) → 长期记忆(持久化)
- **主动反思引擎**: 定时自问"我是谁/主人在想什么"
- **世界模型构建**: 从感知中实时构建对主人和环境的理解
- **LLM 推理**: 集成 MinMax LLM，支持意图识别、因果推理、预测
- **进化引擎**: 从反馈中学习，不断改进自我
- **Webhook集成**: 支持向外部服务发送事件通知
- **外部API适配**: 统一的天气/新闻/翻译等API调用接口
- **配置热更新**: 运行时动态更新配置，无需重启
- **性能监控**: JVM内存、线程、CPU实时监控
- **API文档自动化**: 自动生成OpenAPI格式文档

## 架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Sprite (数字生命)                                  │
├─────────────────────────────────────────────────────────────────────────┤
│    ┌─────────────────────────────────────────────────────────────┐       │
│    │                    感知层 (Perception)                       │       │
│    │   PlatformSensor ── UserSensor ── EnvironmentSensor       │       │
│    │   AudioSensor ── LocationSensor ── DeviceStateSensor     │       │
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
│                              │                                          │
│                              ▼                                          │
│    ┌─────────────────────────────────────────────────────────────┐       │
│    │                    服务层 (Services)                        │       │
│    │   WebhookService ── ExternalApiAdapter ── ConfigHotReload   │       │
│    │   PerformanceMonitor ── ApiDocService                      │       │
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
./mvnw compile
```

### 3. 使用示例

```java
import com.lingfeng.sprite.*;
import com.lingfeng.sprite.llm.MinMaxConfig;

// 无 LLM（使用启发式推理）
Sprite sprite = Sprite.create("雪梨", Sprite.Platform.PC);

// 带 MinMax LLM
MinMaxConfig config = new MinMaxConfig("your-api-key");
Sprite sprite = Sprite.create("雪梨", Sprite.Platform.PC, UUID.randomUUID().toString(), config);

// 执行单轮认知闭环
CognitionController.CognitionResult result = sprite.cognitionCycle();
System.out.println("行动建议: " + result.actionRecommendation());
System.out.println("LLM推理: " + result.reasoningResult());
```

## 文档

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - 完整架构文档
- [ADR-006: Sprite 闭环架构](docs/adr/ADR-006-sprite-closed-loop-architecture.md)
- [DEMAND_POOL.md](docs/DEMAND_POOL.md) - 需求池
- [PROGRESS.md](docs/PROGRESS.md) - 开发进度
- [HANDOVER.md](docs/HANDOVER.md) - 交接文档

## 技术栈

- Java 21
- Spring Boot 3.2.3
- Maven
- OSHI (系统监控)
- MinMax LLM API
- JNA (本地接口)
