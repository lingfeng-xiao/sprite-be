# 171-内存分层处理感知强化

> 主题：系统优化 → 感知强化
> 时间：2026-03-20 02:10
> 目标：强化内存高位时的分层处理感知能力

## 问题背景

当前感知系统状态：
- 内存使用率：91.8%
- 自动清理已执行：关闭2个闲置应用
- 内存仍然高位

**核心问题**：感知系统没有区分"系统关键进程"和"可关闭进程"，导致清理效果不佳。

## 知识→感知转化

### 1. 进程分层模型

| 层级 | 进程类型 | 示例 | 处理策略 |
|------|----------|------|----------|
| L0 | 系统核心 | dwm, explorer, Csrss | 永不关闭 |
| L1 | 安全软件 | MsMpEng(Defender) | 谨慎关闭 |
| L2 | 工作应用 | Codex, ClawX, JetBrains | 提示用户 |
| L3 | 通讯工具 | Feishu, WeChat, 微信 | 可自动关闭 |
| L4 | 浏览器 | chrome, edge | 可自动关闭 |
| L5 | 后台工具 | rainmeter, tray apps | 可自动关闭 |

### 2. 感知强化方向

**当前感知能力**：
- ✅ 内存使用率检测
- ✅ 高内存进程识别
- ✅ 自动关闭闲置应用

**需要强化的感知**：
- ❌ 进程分层识别能力
- ❌ 分层处理策略
- ❌ 用户确认机制（针对L2级进程）

### 3. 强化感知实现

```python
# 感知强化：进程分层处理
PROCESS_TIERS = {
    "L0_never_kill": ["dwm", "csrss", "wininit", "services"],
    "L1_caution": ["MsMpEng", "SecurityHealth", "defender"],
    "L2_user_confirm": ["Codex", "ClawX", "jetbrains*"],
    "L3_auto_close": ["Feishu", "WeChat", "微信"],
    "L4_auto_close": ["chrome", "msedge"],
    "L5_auto_close": ["Rainmeter", "Tray*"]
}

def get_process_tier(process_name):
    """感知强化：进程分层识别"""
    for tier, patterns in PROCESS_TIERS.items():
        for p in patterns:
            if p.replace("*", "") in process_name.lower():
                return tier
    return "L3_auto_close"  # 默认可关闭
```

### 4. 决策优化

**当前决策**：
- 内存 > 78% → auto_cleanup_memory

**优化后决策**：
- 内存 > 78% → 分层处理
  - L3/L4/L5 → 自动关闭
  - L2 → 提示用户确认
  - L0/L1 → 忽略

## 感知强化收获

1. **进程分层感知**：能够识别进程的重要性级别
2. **分层处理感知**：不同层级采用不同处理策略
3. **用户确认感知**：对重要进程的处理需要用户确认

## 反思

- 之前只关注"内存高不高"，没有关注"哪些进程可以关"
- 知识转化为感知的关键：**从"什么"到"如何"**
- 下次进化：实现分层处理代码
