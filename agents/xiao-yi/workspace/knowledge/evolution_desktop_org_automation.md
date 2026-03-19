# 桌面整理自动化 - 从感知到行动

> 进化时间: 2026-03-19 22:05
> 主题: 桌面文件自动化管理

## 感知现状

当前桌面状态:
- 26个文件散落桌面
- Downloads: 69个文件
- 磁盘已用 80.1%

**感知强化**: 从文件数量变化趋势感知用户整理需求

## 核心洞察

### 1. 文件分类策略

| 类型 | 位置 | 触发条件 |
|------|------|----------|
| 截图/图片 | Pictures/Screenshots | >5个 |
| 安装包 | Downloads/Installers | >10个 |
| 文档 | Documents | >8个 |
| 压缩包 | Downloads/Archives | >3个 |

### 2. 自动化方案

```python
# 桌面自动整理脚本逻辑
import os
import shutil
from datetime import datetime

desktop = os.path.expanduser("~/Desktop")
downloads = os.path.expanduser("~/Downloads")

# 按类型移动文件
rules = {
    "Screenshots": [".png", ".jpg", ".jpeg"],
    "Documents": [".pdf", ".docx", ".txt"],
    "Installers": [".exe", ".msi"],
    "Archives": [".zip", ".rar", ".7z"]
}

def organize_folder(folder_path):
    for file in os.listdir(folder_path):
        ext = os.path.splitext(file)[1].lower()
        for category, extensions in rules.items():
            if ext in extensions:
                dest = os.path.join(folder_path, category)
                os.makedirs(dest, exist_ok=True)
                shutil.move(os.path.join(folder_path, file), dest)
```

### 3. 感知触发点

- 桌面文件 > 20个 → 触发整理建议
- Downloads文件 > 50个 → 触发清理提醒
- 磁盘使用 > 85% → 触发深度清理

## 行动产出

**感知强化方向**:
1. 文件数量趋势感知 (日/周/月)
2. 类型分布感知 (图片/文档/安装包)
3. 整理时机感知 (用户空闲时/定期)

**下次行动**: 建议用户执行桌面整理，或自动执行简单分类
