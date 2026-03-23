# 需求池 (DEMAND_POOL)

## 优先级分类

### P0 - 阻断性问题 (Must Fix)

| ID | 需求 | 状态 | 来源 |
|----|------|------|------|
| S1-1 | 服务器内存告警监控 | ✅ done | Sprint-S1 |
| S1-2 | LLM API 失败降级处理 | ✅ done | Sprint-S1 |
| S1-3 | 系统健康状态API | ✅ done | Sprint-S1 |

### P1 - 核心功能 (Should Have)

| ID | 需求 | 状态 | 来源 |
|----|------|------|------|
| S2-1 | 主人响应追踪 | ✅ done | Sprint-S2 |
| S2-2 | 交互偏好学习 | ✅ done | Sprint-S2 |
| S2-3 | 反馈调整机制 | todo | Sprint-S2 |
| S2-4 | 情绪响应模型 | todo | Sprint-S2 |
| S3-1 | 情绪历史追踪 | todo | Sprint-S3 |
| S3-2 | 周内模式识别 | todo | Sprint-S3 |
| S3-3 | 时间模式预测 | todo | Sprint-S3 |
| S3-4 | 时机优化 | todo | Sprint-S3 |
| S4-1 | 定时导出任务 | todo | Sprint-S4 |
| S4-2 | GitHub API集成 | todo | Sprint-S4 |
| S4-3 | 版本回溯支持 | todo | Sprint-S4 |
| S4-4 | 冲突处理 | todo | Sprint-S4 |
| S5-1 | RealUserSensor Linux适配 | todo | Sprint-S5 |
| S5-2 | RealEnvironmentSensor增强 | todo | Sprint-S5 |
| S5-3 | 传感器健康检查 | todo | Sprint-S5 |
| S5-4 | Mock数据清理 | todo | Sprint-S5 |

### P2 - 优化功能 (Nice to Have)

| ID | 需求 | 状态 | 来源 |
|----|------|------|------|
| S6-1 | 多维度决策 | todo | Sprint-S6 |
| S6-2 | 置信度量化 | todo | Sprint-S6 |
| S6-3 | 决策历史分析 | todo | Sprint-S6 |
| S6-4 | 决策规则可视化 | todo | Sprint-S6 |
| S7-1 | 邮件动作插件 | todo | Sprint-S7 |
| S7-2 | 日历动作插件 | todo | Sprint-S7 |
| S7-3 | 知识库查询 | todo | Sprint-S7 |
| S7-4 | 动作链编排 | todo | Sprint-S7 |
| S8-1 | 学习速率自适应 | todo | Sprint-S8 |
| S8-2 | 遗忘机制 | todo | Sprint-S8 |
| S8-3 | 进化历史可视化 | todo | Sprint-S8 |
| S8-4 | 快速回滚机制 | todo | Sprint-S8 |
| S10-1 | 认知Dashboard | todo | Sprint-S10 |
| S10-2 | 记忆可视化 | todo | Sprint-S10 |
| S10-3 | 进化历史Dashboard | todo | Sprint-S10 |
| S10-4 | 主人情绪历史图表 | todo | Sprint-S10 |

### P3 - 探索性功能 (Future)

| ID | 需求 | 状态 | 来源 |
|----|------|------|------|
| S9-1 | 声音传感器 | todo | Sprint-S9 |
| S9-2 | 位置传感器 | todo | Sprint-S9 |
| S9-3 | 设备状态感知 | todo | Sprint-S9 |
| S9-4 | 多设备协同 | todo | Sprint-S9 |

---

## 需求详情

### S1-1: 服务器内存告警监控

**所属阶段**: S1 - 可靠性加固
**优先级**: P0
**状态**: todo

**背景/目标**: 服务器内存不足时需要告警，防止OOM

**实现内容**:
1. 在RealPlatformSensor中检测内存使用率
2. 当内存>80%时触发告警
3. 通过ProactiveService发送通知

**依赖**: S1-3 (健康检查API)
**验收标准**: 内存>80%时自动发送通知

**涉及文件**:
- `RealPlatformSensor.java`
- `ProactiveService.java`

---

### S1-2: LLM API失败降级处理

**所属阶段**: S1 - 可靠性加固
**优先级**: P0
**状态**: todo

**背景/目标**: LLM API不可用时，系统应自动降级到启发式推理

**实现内容**:
1. 在MinMaxLlmReasoner中添加重试逻辑
2. 记录连续失败次数
3. 失败超过阈值时降级到启发式
4. 恢复后自动切回LLM

**依赖**: 无
**验收标准**: LLM API失败时系统仍能正常工作

**涉及文件**:
- `MinMaxLlmReasoner.java`
- `ReasoningEngine.java`

---

### S1-3: 系统健康状态API

**所属阶段**: S1 - 可靠性加固
**优先级**: P0
**状态**: todo

**背景/目标**: 提供统一的系统健康检查接口

**实现内容**:
1. 添加 `/api/sprite/health` 端点
2. 检查各传感器状态
3. 检查LLM连接状态
4. 检查内存使用率
5. 返回整体健康状态

**依赖**: S1-1
**验收标准**: 能通过API获取完整健康状态

**涉及文件**:
- `SpriteController.java`

---

### S2-1: 主人响应追踪

**所属阶段**: S2 - 主人反馈学习
**优先级**: P1
**状态**: done

**背景/目标**: 追踪主人对主动消息的响应，用于学习

**实现内容**:
1. 在ProactiveService中记录发送的消息
2. 监听主人回复（正向/负向/无响应）
3. 存储响应历史到OwnerModel

**依赖**: ProactiveService
**验收标准**: 能记录主人对每次主动消息的响应

**涉及文件**:
- `ProactiveService.java`
- `OwnerModel.java`
- `FeedbackTrackerService.java` (新增)

---

### S2-2: 交互偏好学习

**所属阶段**: S2 - 主人反馈学习
**优先级**: P1
**状态**: done

**背景/目标**: 学习主人交互偏好，调整主动消息策略

**实现内容**:
1. 分析主人对话风格（简短/详细/中性）
2. 记录主人最常用的交互时间
3. 学习主人对不同类型消息的响应率
4. 根据偏好调整消息长度和发送时机

**依赖**: S2-1 (FeedbackTrackerService)
**验收标准**: 能根据主人偏好调整消息长度和发送时机

**涉及文件**:
- `InteractionPreferenceLearningService.java` (新增)
- `ProactiveService.java` (修改)
- `UnifiedContextService.java` (修改)
- `SpriteController.java` (修改)

---

### S3-1: 情绪历史追踪

**所属阶段**: S3 - 情绪时间模式
**优先级**: P1
**状态**: todo

**背景/目标**: 建立长期情绪追踪数据

**实现内容**:
1. 在WorldBuilder中记录每次感知到的情绪
2. 存储到长期记忆（按日期索引）
3. 支持查询历史情绪

**依赖**: BehaviorEmotionInferrer
**验收标准**: 能查询任意日期的情绪历史

**涉及文件**:
- `WorldBuilder.java`
- `OwnerModel.java`
- `MemorySystem.java`

---

## 已完成需求追溯

| ID | 需求 | Sprint | 完成日期 |
|----|------|--------|----------|
| S6-A1 | 长期记忆检索机制 | Sprint-006 | 2026-03-23 |
| S6-A2 | 情境感知记忆索引 | Sprint-006 | 2026-03-23 |
| S6-A3 | 回忆触发行动 | Sprint-006 | 2026-03-23 |
| S6-B1 | 行为信号情感推断 | Sprint-006 | 2026-03-23 |
| S5-1 | 决策引擎重建 | Sprint-005 | 2026-03-22 |
| S5-2 | 动作系统扩展 | Sprint-005 | 2026-03-22 |
| S5-3 | 反馈收集机制 | Sprint-005 | 2026-03-22 |
| S5-4 | 进化机制增强 | Sprint-005 | 2026-03-22 |
| S5-5 | 主动交互智能化 | Sprint-005 | 2026-03-22 |
| S5-6 | 感知系统增强 | Sprint-005 | 2026-03-22 |
| S4 | 主动交互系统 | Sprint-004 | 2026-03-21 |

---

## 任务创建规则

每个任务必须包含：
- ID (阶段-序号)
- 标题
- 所属阶段
- 背景/目标
- 依赖关系
- 优先级
- 当前状态 (todo/doing/blocked/done)
- 验收标准
- 涉及文件
- 备注/风险

---

## 状态更新要求

1. 先更新需求池，再开始做任务
2. 完成任务后立即回写状态
3. 新发现的工作项必须补充进需求池
4. 不允许只改代码不更新文档
