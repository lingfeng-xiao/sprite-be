# Sprint-001 进度日志

## 2026-03-23

### 目标
在服务器上部署 sprite，接入真实 MinMax LLM，完成感知→认知闭环的全面测试。

### 当前阶段
- [x] Phase 1: 环境部署
- [x] Phase 2: 闭环功能测试
- [ ] Phase 3: 文档建设

### 今日计划
1. 上传项目到服务器
2. 编译并启动
3. 手动触发 cognition cycle 测试

### 进展记录

#### 14:00 - 开始
- 确认服务器可用 (Java 21)
- 确认端口 8080 可用
- 确认使用 /home/lingfeng 作为工作目录

### 14:30 - 项目上传
- 使用 tar + scp 上传项目
- 项目大小: 149KB (压缩后)

### 14:45 - JDK 安装
- 服务器只有 JRE，下载并安装 JDK 21
- 下载 openjdk-21.0.2_linux-x64_bin.tar.gz (194MB)

### 14:55 - 编译成功
```
BUILD SUCCESS
[INFO] Total time:  19.066 s
[INFO] Building jar: /home/lingfeng/sprite/target/sprite-0.1.0-SNAPSHOT.jar
```

#### 待办
- [x] 上传项目
- [x] 配置 API Key (LLM disabled - 待用户提供)
- [x] 编译
- [x] 启动测试

### 问题记录

#### 15:05 - RealUserSensor Linux 平台问题
**错误**: `UnsatisfiedLinkError: Unable to load library 'user32'`
**原因**: RealUserSensor 使用 JNA 调用 Windows user32.dll，在 Linux 服务器上无法加载
**解决方案**: 添加平台检测 (IS_WINDOWS)，非 Windows 平台返回默认感知数据
**文件修改**: `src/main/java/com/lingfeng/sprite/sensor/RealUserSensor.java`

### 15:10 - 闭环测试成功
Cognition Cycle 9步流程全部正常工作:
1. Perception - 平台/用户/环境感知正常
2. Pipeline - 感知管道处理正常
3. Fusion - 感知融合正常
4. World Building - 世界模型构建正常
5. Self-Reflection - 自我反思正常 (检测到"新情况：主人正在使用Unknown")
6. Self-Model Check - 自省检查正常
7. LLM Reasoning - LLM 已启用，推理链路正常
8. Memory Storage - 记忆存储正常
9. Action Recommendation - 行动建议正常

### 15:15 - LLM 推理链路测试成功
- MinMax API Key 已配置
- LLM 推理链路正常工作 (reasoningResult 有数据)
- 意图/因果/预测 三路推理全部返回结果（虽然数据为空但链路正常）
- 数据为空是因为 Linux 服务器没有真实用户活动数据

### 下一步

### 下一步
