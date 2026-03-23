# 交接文档 (Handover)

## 项目概述

**项目名称**: Digital Being (数字生命)
**代码位置**: `/home/lingfeng/builds/digital-beings-java`
**远程仓库**: https://github.com/lingfeng-xiao/soul-hub.git

## 当前运行状态

| 指标 | 状态 |
|------|------|
| 服务器进程 | PID 209347 ✅ 运行中 |
| LLM 支持 | ✅ 已启用 |
| 编译状态 | ✅ 通过 |
| 最后部署 | 2026-03-23 16:36 |
| 监听端口 | 8080 |

## 连接信息

```bash
# 服务器连接
ssh jd

# 编译
export JAVA_HOME=/home/lingfeng/jdk-21.0.2
cd /home/lingfeng/builds/digital-beings-java
/home/lingfeng/.m2/wrapper/dists/apache-maven-3.9.6/apache-maven-3.9.6/bin/mvn compile

# 部署
/home/lingfeng/.m2/wrapper/dists/apache-maven-3.9.6/apache-maven-3.9.6/bin/mvn package -DskipTests
nohup $JAVA_HOME/bin/java -jar target/sprite-0.1.0-SNAPSHOT.jar > sprite.log 2>&1 &

# 查看日志
tail -50 ~/sprite.log

# 检查状态
curl http://localhost:8080/api/sprite/state
```

## 最新代码变更

### 本次提交 (ed815b0)
```
fix: 修复 CognitionController 变量作用域错误
- retrievalContext 声明移到 if 块外部，确保在决策阶段可见
- 无论是否有推理引擎都会检索记忆
```

### 相关提交历史
- `fb576b0` - fix: 修复 BehaviorEmotionInferrer 类型错误
- `fd9d2a7` - feat: 行为信号情感推断 (S6-B1)
- `bc2785d` - feat: 决策引擎支持记忆触发动作 (S6-A3)
- `b7f1194` - feat: 集成长期记忆检索到推理引擎

## 关键文件状态

| 文件 | 修改类型 | 说明 |
|------|----------|------|
| `MemoryRetrievalService.java` | 新增 | 情境感知记忆检索服务 |
| `BehaviorEmotionInferrer.java` | 新增 | 行为信号情感推断器 |
| `CognitionController.java` | 修改 | 集成记忆检索 |
| `DecisionEngine.java` | 修改 | 支持记忆触发动作 |
| `ReasoningEngine.java` | 修改 | ReasoningContext 增加 memoryHighlights |
| `MinMaxLlmReasoner.java` | 修改 | 意图识别包含记忆上下文 |
| `WorldBuilder.java` | 修改 | 使用 BehaviorEmotionInferrer |

## 下一步建议 (Next Steps)

### 立即执行 (Immediate)

1. **验证 Sprint-006 功能**
   ```bash
   # 检查记忆检索是否正常工作
   curl http://localhost:8080/api/sprite/state | grep -i memory

   # 检查决策日志
   tail -100 ~/sprite.log | grep -i "memory\|decision\|recall"
   ```

2. **处理积压的心跳消息**
   - 服务器上可能有积压的 avatar 心跳任务
   - 检查 `data/avatars.json` 中的设备状态

### 短期任务 (1-2天)

1. **S6-B2: 主人反馈学习** (P1)
   - 追踪主人对 `ProactiveService` 消息的响应
   - 在 `OwnerModel` 中添加响应历史
   - 调整主动消息触发条件

2. **S6-B3: 情绪时间模式** (P1)
   - 在 `WorldBuilder` 中添加每日情绪追踪
   - 实现 `getEmotionalTimePattern()` 方法
   - 在 `BehaviorEmotionInferrer` 中使用时间模式

### 中期任务 (1周)

1. **P1-3: 长期记忆持久化到 GitHub**
   - 创建定时任务导出记忆
   - 使用 GitHub API 提交备份

2. **P1-4: 传感器数据 Mock 清理**
   - 确认所有传感器返回真实数据
   - 删除或完成 MockSensor

### 长期愿景

1. **多设备协同感知**
   - 整合 DesktopSensor, ProcessSensor, DigitalSensor
   - 实现跨设备主人状态追踪

2. **主人意图预测模型**
   - 基于历史行为训练简单预测模型
   - 预测主人下一步需求

## 已知问题

| 问题 | 影响 | 解决方案 |
|------|------|----------|
| RealUserSensor 在 Linux 返回 UNKNOWN | 情感推断降级为时间基准 | 已在 BehaviorEmotionInferrer 中有降级处理 |
| 服务器时区为 CST | 时间上下文可能不一致 | 已使用 ZoneId.of("Asia/Shanghai") |

## 紧急回滚方案

如果最新代码导致问题，回滚命令：

```bash
ssh jd
cd /home/lingfeng/builds/digital-beings-java
git reset --hard fb576b0  # 回滚到之前可用的版本
# 重新编译部署
```
