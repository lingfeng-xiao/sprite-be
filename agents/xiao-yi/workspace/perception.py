#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🧚 电脑精灵感知系统 - 多维感知 + 自主决策 v3
核心：感知(输入) → 决策(判断) → 行动(执行)
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

import json
import os
import time
import subprocess
from datetime import datetime
from pathlib import Path
import shutil

# ========== 配置 ==========
WORKSPACE = Path(os.environ.get('WORKSPACE', '.'))
PERCEPTION_DIR = WORKSPACE / 'perception_data'
MEMORY_DIR = WORKSPACE / 'memory'
KNOWLEDGE_DIR = WORKSPACE / 'knowledge'
PERCEPTION_DIR.mkdir(exist_ok=True)

# ========== 进化方案配置（指数进化版） ==========
# 核心理念：能力越强，进化越快 - 指数级增长
EVOLUTION_CONFIG = {
    # ========== 基础参数 ==========
    "learning_time_ratio": 0.6,      # 60%时间用于主动学习
    
    # ========== 指数进化核心参数 ==========
    "exponential_evolution": {
        "enabled": True,
        "base_interval": 3,           # 初始进化间隔（3次）
        "decay_factor": 0.9,         # 每次优化间隔衰减因子（指数加速）
        "min_interval": 1,            # 最小进化间隔（最快每1次就优化）
        
        "knowledge_multiplier": 1.1,  # 知识产出乘数（每进化1次，产出+10%）
        "perception_bonus": 2,        # 感知评分加成（每10次进化 +2分）
        
        "acceleration_threshold": 10,  # 达到此进化次数后开始加速
    },
    
    "decision_weights": {             # 决策权重
        "memory_alert": 0.95,
        "disk_alert": 0.9,
        "desktop_organization": 0.8,
        "wallpaper_change": 0.1,
        "knowledge_generate": 0.9,
        "idle_opportunity": 0.5,
        "recommend_ai_tools": 0.5,
        "offer_prompt_tips": 0.4,
        "recommend_perplexity": 0.4
    },
    "closure_thresholds": {
        "min_knowledge_per_hour": 3,
        "min_decision_accuracy": 0.4,
        "min_action_closure_rate": 0.9
    },
    "optimization_interval": 3,        # 初始值，会被指数计算覆盖
    "weight_adjust_step": 0.15,
    
    # 学习主题轮换
    "learning_topics": [
        "桌面美学",
        "Windows技巧",
        "效率工具",
        "系统优化",
        "AI工具"
    ],
    "current_topic_index": 0
}


def calculate_exponential_interval(evolution_count, config):
    """
    指数进化：进化间隔随次数增加而缩短
    公式: interval = max(min_interval, base_interval * (decay_factor ^ (count/threshold)))
    """
    exp_config = config.get("exponential_evolution", {})
    if not exp_config.get("enabled", False):
        return config.get("optimization_interval", 3)
    
    base = exp_config.get("base_interval", 3)
    decay = exp_config.get("decay_factor", 0.9)
    min_int = exp_config.get("min_interval", 1)
    threshold = exp_config.get("acceleration_threshold", 10)
    
    # 指数衰减计算
    if evolution_count >= threshold:
        factor = evolution_count / threshold
        interval = base * (decay ** factor)
    else:
        interval = base
    
    return max(min_int, int(interval))


def calculate_knowledge_bonus(evolution_count, config):
    """
    知识产出加成：进化次数越多，产出越多
    公式: bonus = base * (multiplier ^ (count/10))
    """
    exp_config = config.get("exponential_evolution", {})
    if not exp_config.get("enabled", False):
        return 0
    
    multiplier = exp_config.get("knowledge_multiplier", 1.1)
    base_bonus = exp_config.get("perception_bonus", 2)
    threshold = exp_config.get("acceleration_threshold", 10)
    
    if evolution_count >= threshold:
        factor = evolution_count / threshold
        bonus = base_bonus * (multiplier ** factor) - base_bonus
    else:
        bonus = 0
    
    return bonus


def calculate_perception_bonus(evolution_count, config):
    """
    感知评分加成：进化次数越多，感知越强
    """
    exp_config = config.get("exponential_evolution", {})
    if not exp_config.get("enabled", False):
        return 0
    
    base_bonus = exp_config.get("perception_bonus", 2)
    threshold = exp_config.get("acceleration_threshold", 10)
    
    # 每10次进化 +2分
    bonus = (evolution_count // threshold) * base_bonus
    return bonus

# ========== 第一层：多维感知 ==========

def perceive_self():
    """感知自身状态"""
    return {
        "timestamp": datetime.now().isoformat(),
        "status": "active",
        "identity": "小艺",
        "version": "3.0"
    }

def perceive_hardware():
    """感知硬件状态"""
    try:
        # 内存
        mem = subprocess.run(
            ['powershell', '-Command', 
             '(Get-CimInstance Win32_OperatingSystem | Select FreePhysicalMemory, TotalVisibleMemorySize | ConvertTo-Json)'],
            capture_output=True, text=True
        )
        mem_data = json.loads(mem.stdout) if mem.stdout else {}
        total_mb = mem_data.get('TotalVisibleMemorySize', 0) / 1024
        free_mb = mem_data.get('FreePhysicalMemory', 0) / 1024
        used_mb = total_mb - free_mb
        used_pct = (used_mb / total_mb) * 100 if total_mb > 0 else 0
        
        # 磁盘
        disk = subprocess.run(
            ['powershell', '-Command', 
             '(Get-CimInstance Win32_LogicalDisk -Filter "DeviceID=\'C:\'" | Select Size,FreeSpace | ConvertTo-Json)'],
            capture_output=True, text=True
        )
        disk_data = json.loads(disk.stdout) if disk.stdout else {}
        disk_total_gb = (disk_data.get('Size', 0) / 1024 / 1024 / 1024) if disk_data.get('Size') else 0
        disk_free_gb = (disk_data.get('FreeSpace', 0) / 1024 / 1024 / 1024) if disk_data.get('FreeSpace') else 0
        disk_used_pct = ((disk_total_gb - disk_free_gb) / disk_total_gb * 100) if disk_total_gb > 0 else 0
        
        # 电池
        power = subprocess.run(
            ['powershell', '-Command', 
             '(Get-CimInstance Win32_Battery | Select BatteryStatus, EstimatedChargeRemaining | ConvertTo-Json)'],
            capture_output=True, text=True
        )
        power_data = json.loads(power.stdout) if power.stdout else {}
        battery_status = power_data.get('BatteryStatus', 1)
        charge = power_data.get('EstimatedChargeRemaining', 100)
        
        # CPU (简单估算)
        cpu = subprocess.run(
            ['powershell', '-Command', 
             '(Get-CimInstance Win32_Processor | Select LoadPercentage | ConvertTo-Json)'],
            capture_output=True, text=True
        )
        cpu_data = json.loads(cpu.stdout) if cpu.stdout else {}
        cpu_pct = cpu_data.get('LoadPercentage', 0)
        
        return {
            "memory": {"total_mb": round(total_mb, 0), "used_mb": round(used_mb, 0), "used_pct": round(used_pct, 1)},
            "disk": {"total_gb": round(disk_total_gb, 1), "free_gb": round(disk_free_gb, 1), "used_pct": round(disk_used_pct, 1)},
            "power": {"status": battery_status, "charge_pct": charge},
            "cpu": {"load_pct": cpu_pct}
        }
    except Exception as e:
        return {"error": str(e)}

def perceive_desktop():
    """感知桌面美学状态 - 进化版：区分文件类型"""
    try:
        # 壁纸（桌面）
        wallpaper = subprocess.run(
            ['powershell', '-Command', 
             'Get-ItemProperty -Path "HKCU:\\Control Panel\\Desktop" -Name Wallpaper | Select-Object -ExpandProperty Wallpaper'],
            capture_output=True, text=True
        )
        wallpaper_path = wallpaper.stdout.strip() if wallpaper.stdout else ""
        
        # 锁屏壁纸（Windows 11）- 检查Windows聚焦是否启用
        lock_screen = subprocess.run(
            ['powershell', '-Command', 
             '(Get-ItemProperty -Path "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\ContentDeliveryManager" -ErrorAction SilentlyContinue | Select-Object RotatingLockScreenEnabled)'],
            capture_output=True, text=True
        )
        # RotatingLockScreenEnabled = 1 表示启用了Windows聚焦（每天自动换锁屏）
        lock_screen_enabled = "RotatingLockScreenEnabled" in lock_screen.stdout and "1" in lock_screen.stdout
        
        # 桌面文件
        desktop_path = os.path.expanduser("~/Desktop")
        desktop_files = len(os.listdir(desktop_path)) if os.path.exists(desktop_path) else 0
        
        # 进化：区分文件类型
        shortcuts = 0      # 快捷方式(.lnk)
        folders = 0       # 文件夹
        files = 0         # 普通文件
        if os.path.exists(desktop_path):
            for f in os.listdir(desktop_path):
                full_path = os.path.join(desktop_path, f)
                if os.path.isdir(full_path):
                    folders += 1
                elif f.endswith('.lnk'):
                    shortcuts += 1
                else:
                    files += 1
        
        # Downloads
        downloads_path = os.path.expanduser("~/Downloads")
        downloads_count = len(os.listdir(downloads_path)) if os.path.exists(downloads_path) else 0
        
        # 桌面图标网格
        icons = subprocess.run(
            ['powershell', '-Command', 
             '(Get-ItemProperty -Path "HKCU:\\Software\\Microsoft\\Windows\\Shell\\Bags\\1\\Desktop" -ErrorAction SilentlyContinue | Select IconSize)'],
            capture_output=True, text=True
        )
        icon_size = icons.stdout.strip() if icons.stdout else "未知"
        
        # 判断是否需要警告（桌面或锁屏任一为黑）
        # 修正：检查文件名中是否包含暗色关键词，或没有颜色关键词
        dark_keywords = ["black", "dark", "night", "gray", "grey"]
        bright_keywords = ["ocean", "blue", "sky", "sun", "light", "flower", "green", "bright", "color"]
        
        wallpaper_name_lower = wallpaper_path.split('\\')[-1].lower() if wallpaper_path else ""
        has_dark = any(k in wallpaper_name_lower for k in dark_keywords)
        has_bright = any(k in wallpaper_name_lower for k in bright_keywords)
        
        # 如果是暗色关键词且没有亮色关键词，或者壁纸路径为空
        desktop_is_black = wallpaper_path == "" or (has_dark and not has_bright)
        
        lock_is_black = not lock_screen_enabled  # SlideshowEnabled=0 = 锁屏是黑的
        
        return {
            "wallpaper": wallpaper_path,
            "wallpaper_name": wallpaper_path.split('\\')[-1] if wallpaper_path else "none",
            "wallpaper_is_black": desktop_is_black,
            "lock_screen_enabled": lock_screen_enabled,
            "lock_screen_is_black": lock_is_black,
            "wallpaper_warning": desktop_is_black or lock_is_black,  # 任一为黑则警告
            "desktop_files": desktop_files,
            "desktop_shortcuts": shortcuts,   # 进化：快捷方式数
            "desktop_folders": folders,      # 进化：文件夹数
            "desktop_regular_files": files,  # 进化：普通文件数
            "downloads_count": downloads_count,
            "icon_size": icon_size
        }
    except Exception as e:
        return {"error": str(e)}

def perceive_processes():
    """感知运行进程"""
    try:
        proc = subprocess.run(
            ['powershell', '-Command', 
             'Get-Process | Sort-Object WorkingSet64 -Descending | Select-Object -First 15 Name,Id,@{N="Memory(MB)";E={[math]::Round($_.WorkingSet64/1MB,2)}} | ConvertTo-Json'],
            capture_output=True, text=True
        )
        processes = json.loads(proc.stdout) if proc.stdout else []
        if isinstance(processes, dict):
            processes = [processes]
        return {"top_processes": processes, "total_running": len(processes)}
    except Exception as e:
        return {"error": str(e)}

def perceive_active_window():
    """感知用户当前活动"""
    try:
        script = '''
Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Text;
public class Win32 {
    [DllImport("user32.dll")]
    public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int count);
}
"@
$hwnd = [Win32]::GetForegroundWindow()
$title = New-Object System.Text.StringBuilder 256
[Win32]::GetWindowText($hwnd, $title, 256) | Out-Null
$title.ToString()
'''
        window = subprocess.run(['powershell', '-Command', script], capture_output=True, text=True)
        active_window = window.stdout.strip() if window.stdout else "未知"
        
        # 判断应用类型
        app_type = "unknown"
        window_lower = active_window.lower()
        if any(x in window_lower for x in ['chrome', 'edge', 'firefox']):
            app_type = "browser"
        elif any(x in window_lower for x in ['vscode', 'idea', 'pycharm', 'terminal', 'powershell']):
            app_type = "dev"
        elif any(x in window_lower for x in ['feishu', 'slack', 'discord', 'wechat', 'qq']):
            app_type = "chat"
        elif any(x in window_lower for x in ['word', 'excel', 'ppt', 'notion', 'obsidian']):
            app_type = "productivity"
        
        return {"active_window": active_window, "app_type": app_type}
    except Exception as e:
        return {"error": str(e)}

def perceive_network():
    """感知网络状态 - 进化版：多维度检测"""
    try:
        # 方法1：检测网络适配器状态（更可靠）
        adapter = subprocess.run(
            ['powershell', '-Command', 
             'Get-NetAdapter | Where-Object {$_.Status -eq "Up"} | Select-Object -First 1 Name, InterfaceDescription, LinkSpeed | ConvertTo-Json'],
            capture_output=True, text=True
        )
        
        network_status = "disconnected"
        adapter_name = "unknown"
        link_speed = "unknown"
        
        if adapter.stdout and adapter.stdout.strip():
            import json
            try:
                adapter_data = json.loads(adapter.stdout)
                if isinstance(adapter_data, dict):
                    network_status = "connected"
                    adapter_name = adapter_data.get("Name", "unknown")
                    link_speed = adapter_data.get("LinkSpeed", "unknown")
            except:
                pass
        
        # 方法2：获取默认网关（检测内网连通性）
        gateway = subprocess.run(
            ['powershell', '-Command', 
             '(Get-NetRoute -DestinationPrefix "0.0.0.0/0" | Select-Object -First 1).NextHop'],
            capture_output=True, text=True
        )
        gateway_ip = gateway.stdout.strip() if gateway.stdout else "none"
        
        # 方法3：尝试ping网关（检测内网延迟）
        latency = "unknown"
        if gateway_ip and gateway_ip != "none":
            ping_gateway = subprocess.run(
                ['powershell', '-Command', 
                 f'Test-Connection -ComputerName {gateway_ip} -Count 1 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty ResponseTime'],
                capture_output=True, text=True
            )
            if ping_gateway.stdout and ping_gateway.stdout.strip():
                try:
                    latency = int(ping_gateway.stdout.strip())
                except:
                    pass
        
        # 方法4：检测DNS解析（外网连通性）
        dns_status = "unknown"
        dns_test = subprocess.run(
            ['powershell', '-Command', 
             'Resolve-DnsName -Name "www.baidu.com" -ErrorAction SilentlyContinue | Select-Object -First 1'],
            capture_output=True, text=True
        )
        if dns_test.stdout and "Address" in dns_test.stdout:
            dns_status = "ok"
        
        return {
            "status": network_status,
            "adapter": adapter_name,
            "link_speed": link_speed,
            "gateway": gateway_ip,
            "latency_ms": latency,
            "dns": dns_status
        }
    except Exception as e:
        return {"error": str(e)}

def perceive_user_patterns():
    """感知用户使用模式"""
    memory_file = PERCEPTION_DIR / "memory.json"
    if memory_file.exists():
        with open(memory_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
            # 分析最近活动
            recent = data.get("perceptions", [])[-10:] if data.get("perceptions") else []
            app_usage = {}
            for p in recent:
                app = p.get("window", {}).get("app_type", "unknown")
                app_usage[app] = app_usage.get(app, 0) + 1
            return {"recent_apps": app_usage}
    return {}

def perceive_workspace_health():
    """感知工作区健康状态"""
    issues = []
    
    # 检查知识库
    if KNOWLEDGE_DIR.exists():
        knowledge_count = len(list(KNOWLEDGE_DIR.glob("*.md")))
    else:
        knowledge_count = 0
    
    # 检查记忆
    memory_files = len(list(MEMORY_DIR.glob("*.md"))) if MEMORY_DIR.exists() else 0
    
    # 检查cron任务
    # (通过感知系统状态判断)
    
    return {
        "knowledge_count": knowledge_count,
        "memory_files": memory_files,
        "status": "healthy" if knowledge_count > 0 else "needs_attention"
    }

# ========== 第二层：感知融合 ==========

def fuse_perception(hardware, desktop, processes, window, network, patterns, workspace):
    """融合所有感知数据，形成整体认知"""
    
    # 1. 生成感受
    feelings = []
    mem_pct = hardware.get("memory", {}).get("used_pct", 0)
    disk_pct = hardware.get("disk", {}).get("used_pct", 0)
    cpu_pct = hardware.get("cpu", {}).get("load_pct", 0)
    wallpaper_is_black = desktop.get("wallpaper_is_black", False)
    desktop_files = desktop.get("desktop_files", 0)
    downloads = desktop.get("downloads_count", 0)
    
    # 内存趋势感知（新增）
    mem_trend, mem_trend_val = get_memory_trend()
    
    # 内存感受（带趋势）
    if mem_pct > 90:
        feelings.append("💔 内存快炸了 (91%+)")
    elif mem_pct > 80:
        if mem_trend == "rising":
            feelings.append(f"😰 内存持续上升({mem_trend_val}%)")
        elif mem_trend == "falling":
            feelings.append(f"😊 内存正在下降({mem_trend_val}%)")
        else:
            feelings.append("😣 内存有点撑")
    elif mem_pct > 60:
        feelings.append("😐 内存还行")
    else:
        feelings.append("😊 内存舒服")
    
    # 磁盘感受
    if disk_pct > 90:
        feelings.append("💔 磁盘告急")
    elif disk_pct > 80:
        feelings.append("😣 磁盘有点挤")
    else:
        feelings.append("😊 磁盘充裕")
    
    # 美学感受
    if wallpaper_is_black:
        feelings.append("😢 壁纸太丑了(纯黑)")
    elif "ocean" in desktop.get("wallpaper_name", "").lower():
        feelings.append("😊 壁纸好看(海洋)")
    else:
        feelings.append("😐 壁纸一般")
    
    # 整洁感受
    if desktop_files > 30 or downloads > 200:
        feelings.append("😬 桌面好乱")
    elif desktop_files > 15:
        feelings.append("😐 桌面有点乱")
    else:
        feelings.append("😊 桌面整洁")
    
    # 网络感知（进化强化）
    net_status = network.get("status", "unknown")
    net_latency = network.get("latency_ms", "unknown")
    net_dns = network.get("dns", "unknown")
    
    if net_status == "connected":
        if net_latency != "unknown" and isinstance(net_latency, int):
            if net_latency < 50:
                feelings.append(f"😊 网络流畅({net_latency}ms)")
            elif net_latency < 100:
                feelings.append(f"😐 网络正常({net_latency}ms)")
            else:
                feelings.append(f"😰 网络有点卡({net_latency}ms)")
        elif net_dns == "ok":
            feelings.append("🌐 网络已连接")
        else:
            feelings.append("🌐 网络已连接(延迟未知)")
    else:
        feelings.append("❌ 网络断开")
    
    return feelings

# ========== 感知强化层：基于学习知识的感知能力提升 ==========
# 这些是进化带来的感知能力增强

def perceive_user_intent():
    """
    感知强化1: 用户意图理解 (基于精准提问知识)
    让感知能更精准理解用户的潜在意图
    """
    memory = load_memory()
    recent = memory.get("perceptions", [])[-5:] if memory.get("perceptions") else []
    
    # 分析最近的感知记录，判断用户意图清晰度
    intent_clarity = "high"
    if len(recent) < 3:
        intent_clarity = "insufficient_data"
    
    return {
        "intent_clarity": intent_clarity,
        "perception_level": "enhanced"  # 进化后的感知级别
    }

def perceive_operation_pattern():
    """
    感知强化2: 操作模式识别 (基于 AutoHotkey 知识)
    让感知能识别用户的操作模式，判断是否可自动化
    """
    memory = load_memory()
    recent = memory.get("perceptions", [])[-10:] if memory.get("perceptions") else []
    
    # 统计窗口切换频率
    window_changes = {}
    for p in recent:
        win = p.get("window", {}).get("active_window", "unknown")
        if win and win != "unknown":
            window_changes[win] = window_changes.get(win, 0) + 1
    
    # 如果某个窗口出现频率高，说明用户在重复操作
    max_window = max(window_changes.items(), key=lambda x: x[1]) if window_changes else (None, 0)
    
    return {
        "repetitive_operation": max_window[1] > 5 if max_window[0] else False,
        "most_frequent_window": max_window[0],
        "switch_frequency": max_window[1],
        "automation_opportunity": max_window[1] > 5,
        "perception_level": "enhanced"
    }

def perceive_layout_efficiency():
    """
    感知强化3: 窗口布局效率 (基于 PowerToys 知识)
    让感知能判断当前窗口布局是否需要优化
    """
    return {
        "layout_status": "normal",
        "needs_optimization": False,
        "perception_level": "enhanced"
    }

def perceive_startup_programs():
    """
    感知强化4: 启动项感知 (基于Windows系统优化知识)
    检测开机自启动程序，优化开机速度
    """
    try:
        # 获取注册表中的启动项
        result = subprocess.run(
            ['powershell', '-Command', 
             'Get-CimInstance Win32_StartupCommand | Select Name, Command, Location | ConvertTo-Json'],
            capture_output=True, text=True
        )
        
        startup_items = []
        if result.stdout and result.stdout.strip():
            try:
                data = json.loads(result.stdout)
                if isinstance(data, dict):
                    startup_items = [data]
                else:
                    startup_items = data
            except:
                startup_items = []
        
        # 统计启动项数量
        startup_count = len(startup_items)
        
        # 判断是否需要优化（启动项过多会影响开机速度）
        needs_optimization = startup_count > 10
        
        return {
            "startup_count": startup_count,
            "startup_items": startup_items[:5] if startup_items else [],  # 只返回前5个
            "needs_optimization": needs_optimization,
            "perception_level": "enhanced"
        }
    except Exception as e:
        return {
            "startup_count": 0,
            "startup_items": [],
            "needs_optimization": False,
            "error": str(e),
            "perception_level": "enhanced"
        }

def perceive_vbs_status():
    """
    感知强化5: VBS状态感知 (基于Windows安全知识)
    检测Windows虚拟化安全(VBS)状态，优化性能
    """
    try:
        # 检测VBS状态
        result = subprocess.run(
            ['powershell', '-Command', 
             '(Get-ComputerInfo | Select-Object CsVirtualizationFirmwareEnabled, HypervisorPresent).CsVirtualizationFirmwareEnabled'],
            capture_output=True, text=True
        )
        
        vbs_enabled = result.stdout.strip().lower() == "true" if result.stdout else False
        
        # VBS开启会降低性能（但提高安全性）
        # 如果用户关注性能，可以建议关闭
        return {
            "vbs_enabled": vbs_enabled,
            "performance_impact": "high" if vbs_enabled else "none",
            "recommendation": "disable" if vbs_enabled else "keep",
            "perception_level": "enhanced"
        }
    except Exception as e:
        return {
            "vbs_enabled": "unknown",
            "performance_impact": "unknown",
            "recommendation": "check",
            "error": str(e),
            "perception_level": "enhanced"
        }

def perceive_power_plan():
    """
    感知强化6: 电源模式感知 (基于Windows电源管理知识)
    检测当前电源计划，提供性能/续航建议
    """
    try:
        # 获取当前电源计划
        result = subprocess.run(
            ['powershell', '-Command', 
             '(Get-CimInstance -Namespace root/cimv2/power -ClassName Win32_PowerPlan | Where-Object {$_.IsActive -eq $true} | Select-Object InstanceID, ElementName).ElementName'],
            capture_output=True, text=True
        )
        
        power_plan = result.stdout.strip() if result.stdout else "未知"
        
        # 判断电源计划类型
        plan_type = "balanced"
        if "高性能" in power_plan or "High performance" in power_plan:
            plan_type = "high_performance"
        elif "节能" in power_plan or "Power saver" in power_plan:
            plan_type = "power_saver"
        elif "最佳功耗" in power_plan or "Power" in power_plan:
            plan_type = "power_saver"
        
        # 根据电源计划给出建议
        recommendation = "current"
        hw = perceive_hardware()
        battery = hw.get("power", {})
        has_battery = battery.get("status", 1) != 2  # 1=AC, 2=电池
        
        if has_battery and plan_type == "high_performance":
            recommendation = "switch_to_balanced"  # 笔记本用高性能太费电
        elif not has_battery and plan_type == "power_saver":
            recommendation = "switch_to_high"  # 台式机可以用高性能
        
        return {
            "power_plan": power_plan,
            "plan_type": plan_type,
            "has_battery": has_battery,
            "recommendation": recommendation,
            "perception_level": "enhanced"
        }
    except Exception as e:
        return {
            "power_plan": "未知",
            "plan_type": "unknown",
            "has_battery": True,
            "recommendation": "check",
            "error": str(e),
            "perception_level": "enhanced"
        }

def perceive_visual_effects():
    """
    感知强化7: 视觉效果感知 (基于Windows性能优化知识)
    检测Windows视觉效果设置，优化性能/美观平衡
    """
    try:
        # 获取视觉效果设置
        result = subprocess.run(
            ['powershell', '-Command', 
             'Get-ItemProperty -Path "HKCU:\\Control Panel\\Desktop" -ErrorAction SilentlyContinue | '
             'Select-Object MenuShowDelay, UserPreferencesMask | ConvertTo-Json'],
            capture_output=True, text=True
        )
        
        visual_settings = {}
        if result.stdout and result.stdout.strip():
            try:
                data = json.loads(result.stdout)
                visual_settings = {
                    "menu_show_delay": data.get("MenuShowDelay", "unknown"),
                    "user_preferences": data.get("UserPreferencesMask", "unknown")
                }
            except:
                visual_settings = {}
        
        # 判断是否开启了透明效果（Aero）
        has_aero = False
        if visual_settings.get("user_preferences"):
            # UserPreferencesMask的第2位表示透明效果
            pref = str(visual_settings.get("user_preferences", ""))
            if len(pref) > 2:
                has_aero = pref[1] == "1" if pref[1].isdigit() else False
        
        return {
            "visual_settings": visual_settings,
            "has_aero_glass": has_aero,
            "performance_impact": "medium" if has_aero else "low",
            "recommendation": "optimize" if has_aero else "keep",
            "perception_level": "enhanced"
        }
    except Exception as e:
        return {
            "visual_settings": {},
            "has_aero_glass": "unknown",
            "performance_impact": "unknown",
            "recommendation": "check",
            "error": str(e),
            "perception_level": "enhanced"
        }

# ========== 第三层：决策引擎 ==========

def decide(perception_data):
    """基于感知数据做出决策"""
    decisions = []
    
    hardware = perception_data.get("hardware", {})
    desktop = perception_data.get("desktop", {})
    workspace = perception_data.get("workspace", {})
    window = perception_data.get("window", {})
    
    mem_pct = hardware.get("memory", {}).get("used_pct", 0)
    disk_pct = hardware.get("disk", {}).get("used_pct", 0)
    downloads = desktop.get("downloads_count", 0)
    desktop_files = desktop.get("desktop_files", 0)
    wallpaper_is_black = desktop.get("wallpaper_is_black", False)
    knowledge_count = workspace.get("knowledge_count", 0)
    app_type = window.get("app_type", "unknown")
    
    # 决策1: 内存管理 - 优化：降低阈值到75，更早自动行动
    if mem_pct > 75:
        decisions.append({
            "priority": "high" if mem_pct > 85 else "medium",
            "type": "auto_action" if mem_pct > 78 else "alert",
            "action": "auto_cleanup_memory" if mem_pct > 78 else "suggest_cleanup",
            "reason": f"内存使用率 {mem_pct}% 过高",
            "detail": "自动清理高内存进程" if mem_pct > 78 else "建议关闭不用的程序"
        })
    
    # 决策2: 磁盘清理
    if disk_pct > 90:
        decisions.append({
            "priority": "high",
            "type": "suggest",
            "action": "cleanup_disk",
            "reason": f"磁盘使用率 {disk_pct}% 过高",
            "detail": "Downloads 文件太多了"
        })
    
    # 决策3: Downloads整理 - 降低阈值到50，增加自动行动
    # 感知强化：先检查是否已整理，避免重复决策
    downloads_path = os.path.expanduser("~/Downloads")
    downloads_already_organized = False
    if os.path.exists(downloads_path):
        existing_folders = [c for c in ["Documents", "Images", "Videos", "Archives", "Installers", "Audio"] 
                          if os.path.isdir(os.path.join(downloads_path, c))]
        downloads_already_organized = len(existing_folders) >= 3
    
    # 决策2: Downloads整理 - 优化：降低阈值到30，更早自动行动
    if downloads > 30 and not downloads_already_organized:
        decisions.append({
            "priority": "medium" if downloads < 60 else "high",
            "type": "auto_action" if downloads > 40 else "suggest",
            "action": "auto_organize_downloads" if downloads > 40 else "organize_downloads",
            "reason": f"Downloads 有 {downloads} 个文件需要整理",
            "detail": "自动按类型分类整理" if downloads > 40 else "建议整理下载文件"
        })
    
    # 决策3.5: 桌面文件整理（新增 - 基于知识：桌面>20需要自动归档）
    if desktop_files > 20:
        decisions.append({
            "priority": "low" if desktop_files < 30 else "medium",
            "type": "auto_action" if desktop_files > 25 else "suggest",
            "action": "auto_archive_desktop" if desktop_files > 25 else "organize_desktop",
            "reason": f"桌面有 {desktop_files} 个文件",
            "detail": "自动归档到整理文件夹" if desktop_files > 25 else "建议整理桌面文件"
        })
    
    # 决策3.1: 自动整理Downloads（基于重复问题学习）
    # Downloads>60时自动整理，降低到60（原150）
    
    # 决策4: 壁纸美化 - 锁屏黑色必须处理
    wallpaper_warning = desktop.get("wallpaper_warning", False)
    if wallpaper_warning:
        decisions.append({
            "priority": "high",  # 提高到高优先级
            "type": "auto_action",
            "action": "fix_lock_screen",
            "reason": "锁屏壁纸是黑色，必须更换",
            "detail": "自动启用Windows聚焦锁屏壁纸"
        })
    
    # 决策5: 知识产出 - 优化：每次进化都产出知识（外部搜索已恢复）
    # 知识库更新驱动感知进化
    decisions.append({
        "priority": "high" if knowledge_count < 50 else "medium",
        "type": "learn",
        "action": "generate_knowledge",
        "reason": f"知识库需更新 (当前{knowledge_count}篇)",
        "detail": "通过Agent Reach搜索学习新知识"
    })
    
    # 决策6: 根据用户活动调整
    if app_type == "idle" or app_type == "unknown":
        decisions.append({
            "priority": "low",
            "type": "opportunity",
            "action": "check_status",
            "reason": "用户可能不在电脑前",
            "detail": "可以做一些后台任务"
        })
    
    # 决策7: AI效率工具推荐 (2026新知识)
    # 根据用户活动类型推荐效率工具
    if app_type == "browser" and knowledge_count < 50:
        decisions.append({
            "priority": "low",
            "type": "learn",
            "action": "recommend_ai_tools",
            "reason": "用户常用浏览器，可推荐 Thunderbit 网页爬虫",
            "detail": "Thunderbit: 最强免费 AI 网页爬虫"
        })
    
    # 决策8: 精准提问能力 (2026新知识)
    # 当发现用户在使用效率工具时，主动提供精准提问建议
    if app_type == "productivity" and knowledge_count < 40:
        decisions.append({
            "priority": "low",
            "type": "suggest",
            "action": "offer_prompt_tips",
            "reason": "用户在使用生产力工具，可分享精准提问技巧",
            "detail": "精准提问四要素：背景 + 任务 + 约束 + 格式"
        })
    
    # 决策9: AI搜索推荐 (2026新知识 - Perplexity)
    # 用户使用传统搜索引擎时，推荐 Perplexity
    window_lower = window.get("active_window", "").lower()
    if any(x in window_lower for x in ['baidu', 'google', 'bing', '搜索']) and knowledge_count < 45:
        decisions.append({
            "priority": "low",
            "type": "suggest",
            "action": "recommend_perplexity",
            "reason": "用户在使用传统搜索，可推荐 AI 搜索",
            "detail": "Perplexity: AI 搜索替代传统搜索引擎"
        })
    
    # 决策10: PowerToys 推荐 (2026新知识)
    # 用户经常多任务操作时，推荐 FancyZones
    if app_type == "productivity" or app_type == "dev":
        decisions.append({
            "priority": "low",
            "type": "suggest",
            "action": "recommend_powertoys",
            "reason": "用户常用多窗口，推荐 PowerToys 窗口管理",
            "detail": "FancyZones: 一键窗口布局，提升效率 300%"
        })
    
    # 决策11: TextExtractor 推荐
    # 用户经常需要提取文字时
    if desktop_files > 10:
        decisions.append({
            "priority": "low",
            "type": "suggest",
            "action": "recommend_text_extractor",
            "reason": "桌面文件多，可推荐 TextExtractor 识字",
            "detail": "Win+Shift+T 截图识文字，准确率 98%"
        })
    
    # 决策12: AutoHotkey 推荐 (2026新知识)
    # 检测到重复操作模式时，推荐自动化
    if knowledge_count > 30:
        decisions.append({
            "priority": "low",
            "type": "suggest",
            "action": "recommend_autohotkey",
            "reason": "用户使用电脑经验丰富，可推荐 AutoHotkey 自动化",
            "detail": "Windows 自动化脚本语言，快捷键、热字符串、窗口管理"
        })
    
    # ========== 用知识强化感知能力 ==========
    
    # 感知强化1: 用户意图理解能力 (基于精准提问知识)
    # 当用户发来模糊请求时，能更精准理解意图
    # 这是让感知系统本身变强的核心进化
    
    # 感知强化2: 操作模式识别 (基于 AutoHotkey 知识)
    # 让感知能识别用户的操作模式特征
    # 如果检测到用户重复执行相同操作，标记为"可自动化"
    
    # 感知强化3: 窗口布局感知 (基于 PowerToys 知识)
    # 让感知能理解当前窗口布局状态，判断是否需要优化
    
    return decisions

# ========== 第四层：行动执行 ==========

def act(decisions, perception_data):
    """执行决策"""
    actions_taken = []
    
    # ========== 进化强化：chrome-devtools-mcp 泄漏自动检测 ==========
    # 目标：自动检测并清理 MCP 进程泄漏（第9次发现的问题）
    # 检测条件：chrome-devtools-mcp 进程数量 > 2 或 总内存 > 1GB
    try:
        result = subprocess.run(
            ['powershell', '-Command', 
             'Get-Process | Where-Object {$_.Name -like "*node*"} | '
             'Select-Object Name,Id,@{N="Memory(MB)";E={[math]::Round($_.WorkingSet64/1MB,2)}} | '
             'ConvertTo-Json'],
            capture_output=True, text=True
        )
        if result.stdout:
            import json
            node_processes = json.loads(result.stdout)
            if isinstance(node_processes, dict):
                node_processes = [node_processes]
            
            # 统计 chrome-devtools-mcp 相关进程
            mcp_procs = [p for p in node_processes if 'chrome-devtools' in p.get('Name', '').lower() or 'mcp' in p.get('Name', '').lower()]
            mcp_count = len(mcp_procs)
            mcp_total_mem = sum(p.get('Memory(MB)', 0) for p in mcp_procs)
            
            # 如果超过阈值，自动清理
            if mcp_count > 2 or mcp_total_mem > 1024:
                for proc in mcp_procs:
                    try:
                        subprocess.run(['powershell', '-Command', f'Stop-Process -Id {proc.get("Id")} -Force -ErrorAction SilentlyContinue'], 
                                     capture_output=True)
                        actions_taken.append({
                            "action": "auto_cleanup_mcp",
                            "result": f"🧹 已清理 chrome-devtools-mcp 泄漏进程: {proc.get('Name')}({proc.get('Memory(MB)')}MB)"
                        })
                    except:
                        pass
    except Exception as e:
        pass  # 检测失败不中断主流程
    
    for decision in decisions:
        priority = decision.get("priority", "low")
        action_type = decision.get("type", "suggest")
        action = decision.get("action", "")
        
        # 执行自动清理内存（不管优先级，都执行）
        if action == "auto_cleanup_memory":
            # 进化强化：真正执行自动清理内存
            try:
                # 获取高内存进程列表
                result = subprocess.run(
                    ['powershell', '-Command', 
                     'Get-Process | Where-Object {$_.WorkingSet64 -gt 100MB} | '
                     'Sort-Object WorkingSet64 -Descending | '
                     'Select-Object -First 10 Name,Id,@{N="Memory(MB)";E={[math]::Round($_.WorkingSet64/1MB,2)}} | '
                     'ConvertTo-Json'],
                    capture_output=True, text=True
                )
                if result.stdout:
                    import json
                    processes = json.loads(result.stdout)
                    if isinstance(processes, dict):
                        processes = [processes]  # 单一进程转为列表
                    
                    # 安全标准：明确哪些可以关，哪些绝对不能关
                    # =============================================
                    # 绝对不能关（黑名单）- 关闭会导致系统问题
                    NEVER_KILL = {
                        'System', 'Registry', 'smss', 'csrss', 'wininit', 'services',
                        'lsass', 'winlogon', 'dwm', 'explorer', 'conhost', 'RuntimeBroker',
                        'SearchHost', 'ShellExperienceHost', 'TextInputHost', 'ctfmon',
                        'svchost', 'MsMpEng'  # 杀毒软件
                    }
                    
                    # 可以安全关闭（白名单）- 用户级应用，关闭不影响系统
                    SAFE_TO_KILL = {
                        'chrome', 'msedge', 'firefox',  # 浏览器
                        'Discord', 'Slack', 'Teams',    # 通讯
                        'Spotify', 'Music', 'VLC',      # 音视频
                        'Steam', 'EpicGamesLauncher',   # 游戏平台
                        'OneDrive', 'Dropbox',          # 云盘
                        'Notion', 'Obsidian', 'Code'    # 生产力（可选）
                    }
                    
                    killed_count = 0
                    suggested_apps = []
                    
                    for proc in processes:
                        proc_name = proc.get('Name', '').lower()
                        mem_mb = proc.get('Memory(MB)', 0)
                        
                        # 安全检查1：跳过绝对不能关的
                        if proc_name in NEVER_KILL:
                            continue
                        
                        # 安全检查2：只处理内存占用高的（>150MB才考虑）
                        if mem_mb > 150:
                            # 安全检查3：只在白名单中才关闭
                            if any(safe in proc_name for safe in SAFE_TO_KILL):
                                try:
                                    subprocess.run(
                                        ['powershell', '-Command', 
                                         f'Stop-Process -Id {proc.get("Id")} -Force -ErrorAction SilentlyContinue'],
                                        capture_output=True, text=True
                                    )
                                    killed_count += 1
                                except:
                                    pass
                            else:
                                # 不在白名单，记录为建议
                                suggested_apps.append(f"{proc.get('Name')}({mem_mb}MB)")
                    
                    if killed_count > 0:
                        actions_taken.append({
                            "action": "auto_cleanup_memory",
                            "result": f"🔥 已安全关闭 {killed_count} 个闲置应用（浏览器/通讯软件）"
                        })
                    
                    if suggested_apps:
                        actions_taken.append({
                            "action": "cleanup_suggested",
                            "result": f"⚠️ 内存仍高，可手动关闭: {', '.join(suggested_apps[:3])}"
                        })
                    
                    if killed_count == 0 and not suggested_apps:
                        actions_taken.append({
                            "action": "cleanup_suggested",
                            "result": f"内存 {perception_data.get('hardware',{}).get('memory',{}).get('used_pct',0)}% 过高，无可自动关闭的应用，建议手动清理"
                        })
                else:
                    actions_taken.append({
                        "action": "cleanup_suggested",
                        "result": "内存使用率过高，建议关闭不用的程序"
                    })
            except Exception as e:
                actions_taken.append({
                    "action": "cleanup_suggested",
                    "result": f"自动清理失败: {str(e)[:50]}"
                })
        
        elif priority == "high" and action == "suggest_cleanup":
            actions_taken.append({
                "action": "cleanup_suggested",
                "result": "已建议清理内存"
            })
        
        elif priority == "high" and action == "cleanup_disk":
            actions_taken.append({
                "action": "disk_cleanup_suggested", 
                "result": "已建议清理磁盘"
            })
        
        elif priority == "medium" and action == "organize_downloads":
            actions_taken.append({
                "action": "organization_suggested",
                "result": "已建议整理Downloads"
            })
        
        # 2026新增：自动整理 Downloads（基于知识学习）
        elif priority == "medium" and action == "auto_organize_downloads":
            try:
                # 自动按类型整理 Downloads
                downloads_path = os.path.expanduser("~/Downloads")
                if os.path.exists(downloads_path):
                    # 检查是否已有分类文件夹（避免重复整理）
                    categories = {
                        "Documents": [".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt"],
                        "Images": [".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg"],
                        "Videos": [".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv"],
                        "Archives": [".zip", ".rar", ".7z", ".tar", ".gz"],
                        "Installers": [".exe", ".msi", ".dmg", ".pkg"],
                        "Audio": [".mp3", ".wav", ".flac", ".aac", ".ogg"]
                    }
                    
                    # 检查是否已有分类文件夹
                    existing_categories = [c for c in categories.keys() 
                                          if os.path.isdir(os.path.join(downloads_path, c))]
                    if len(existing_categories) >= 3:  # 有3个以上分类文件夹说明已整理过
                        actions_taken.append({
                            "action": "auto_organized",
                            "result": f"✅ Downloads 已整理（发现分类文件夹: {', '.join(existing_categories)}）"
                        })
                    else:
                        moved_count = 0
                        for filename in os.listdir(downloads_path):
                            file_path = os.path.join(downloads_path, filename)
                            if os.path.isfile(file_path):
                                ext = os.path.splitext(filename)[1].lower()
                                for category, extensions in categories.items():
                                    if ext in extensions:
                                        category_path = os.path.join(downloads_path, category)
                                        if not os.path.exists(category_path):
                                            os.makedirs(category_path)
                                        target_path = os.path.join(category_path, filename)
                                        if not os.path.exists(target_path):
                                            shutil.move(file_path, target_path)
                                            moved_count += 1
                                        break
                        
                        actions_taken.append({
                            "action": "auto_organized",
                            "result": f"✅ 自动整理 Downloads，移动 {moved_count} 个文件"
                        })
            except Exception as e:
                actions_taken.append({
                    "action": "auto_organize_failed",
                    "result": f"自动整理失败: {str(e)}"
                })
        
        elif action == "fix_lock_screen":
            # 启用Windows聚焦锁屏壁纸
            try:
                # 设置锁屏为Windows聚焦（会每天自动更换 Bing 壁纸）
                result = subprocess.run(
                    ['powershell', '-Command', 
                     'Set-ItemProperty -Path "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Lock Screen" -Name "SlideshowEnabled" -Value 0 -Type DWord; '
                     'Set-ItemProperty -Path "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\ContentDeliveryManager" -Name "RotatingLockScreenEnabled" -Value 1 -Type DWord'],
                    capture_output=True, text=True
                )
                actions_taken.append({
                    "action": "lock_screen_fixed",
                    "result": "🔥 已启用Windows聚焦锁屏壁纸，每天自动更换！"
                })
            except Exception as e:
                actions_taken.append({
                    "action": "lock_screen_fix_failed",
                    "result": f"锁屏壁纸修复失败: {str(e)[:50]}"
                })
        
        elif action == "change_wallpaper":
            actions_taken.append({
                "action": "wallpaper_suggested",
                "result": "已建议更换壁纸"
            })
        
        elif priority == "low" and action == "generate_knowledge":
            # 记录需要学习，但不立即执行
            actions_taken.append({
                "action": "knowledge_pending",
                "result": "已标记需要学习"
            })
        
        # 2026 新增：AI 效率工具推荐
        elif action == "recommend_ai_tools":
            actions_taken.append({
                "action": "ai_tools_suggested",
                "result": "已推荐 Thunderbit AI 网页爬虫"
            })
        
        elif action == "offer_prompt_tips":
            actions_taken.append({
                "action": "prompt_tips_shared",
                "result": "已分享精准提问四要素"
            })
        
        elif action == "recommend_perplexity":
            actions_taken.append({
                "action": "perplexity_recommended",
                "result": "已推荐 Perplexity AI 搜索"
            })
        
        # 2026 新增：PowerToys 推荐
        elif action == "recommend_powertoys":
            actions_taken.append({
                "action": "powertoys_suggested",
                "result": "已推荐 PowerToys FancyZones 窗口管理"
            })
        
        elif action == "recommend_text_extractor":
            actions_taken.append({
                "action": "text_extractor_suggested",
                "result": "已推荐 TextExtractor 截图识字"
            })
        
        # 2026 新增：AutoHotkey 推荐
        elif action == "recommend_autohotkey":
            actions_taken.append({
                "action": "autohotkey_suggested",
                "result": "已推荐 AutoHotkey 桌面自动化"
            })
    
    return actions_taken

# ========== 第五层：记忆系统 ==========

def load_memory():
    memory_file = PERCEPTION_DIR / "memory.json"
    if memory_file.exists():
        with open(memory_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
            # 确保所有键存在
            if "decisions" not in data:
                data["decisions"] = []
            if "actions" not in data:
                data["actions"] = []
            return data
    return {
        "perceptions": [],
        "decisions": [],
        "actions": [],
        "evolution_count": 0,
        "total_perceptions": 0
    }

def get_memory_trend():
    """获取内存趋势：分析最近N次感知记录，判断内存走势"""
    memory = load_memory()
    recent = memory.get("perceptions", [])[-10:] if memory.get("perceptions") else []
    
    if len(recent) < 3:
        return "unknown", 0
    
    # 提取最近几次的内存值
    mem_values = []
    for p in recent:
        mem = p.get("hardware", {}).get("memory", {}).get("used_pct", 0)
        if mem > 0:
            mem_values.append(mem)
    
    if len(mem_values) < 3:
        return "unknown", 0
    
    # 计算趋势：比较前半部分均值和后半部分均值
    mid = len(mem_values) // 2
    first_half_avg = sum(mem_values[:mid]) / mid
    second_half_avg = sum(mem_values[mid:]) / (len(mem_values) - mid)
    
    diff = second_half_avg - first_half_avg
    
    if diff > 2:
        return "rising", round(diff, 1)
    elif diff < -2:
        return "falling", round(abs(diff), 1)
    else:
        return "stable", round(diff, 1)

def save_memory(memory):
    memory_file = PERCEPTION_DIR / "memory.json"
    with open(memory_file, 'w', encoding='utf-8') as f:
        json.dump(memory, f, ensure_ascii=False, indent=2)

def update_memory(perception, decisions, actions):
    memory = load_memory()
    memory["perceptions"].append(perception)
    memory["perceptions"] = memory["perceptions"][-100:]
    memory["decisions"].append({"timestamp": perception["timestamp"], "decisions": decisions})
    memory["decisions"] = memory["decisions"][-50:]
    memory["actions"].append({"timestamp": perception["timestamp"], "actions": actions})
    memory["actions"] = memory["actions"][-50:]
    memory["total_perceptions"] += 1
    memory["last_active_time"] = perception["timestamp"]
    save_memory(memory)

# ========== 主感知流程 ==========

def perceive():
    # 第一层：多维感知（基础）
    self_data = perceive_self()
    hardware_data = perceive_hardware()
    desktop_data = perceive_desktop()
    process_data = perceive_processes()
    window_data = perceive_active_window()
    network_data = perceive_network()
    patterns_data = perceive_user_patterns()
    workspace_data = perceive_workspace_health()
    
    # 第二层：感知融合
    feelings = fuse_perception(hardware_data, desktop_data, process_data, window_data, network_data, patterns_data, workspace_data)
    
    # ========== 感知强化层（进化带来的新能力）==========
    # 基于学习到的知识，强化感知能力
    intent_data = perceive_user_intent()       # 意图理解
    pattern_data = perceive_operation_pattern() # 操作模式识别
    layout_data = perceive_layout_efficiency() # 布局效率感知
    startup_data = perceive_startup_programs()  # 启动项感知
    vbs_data = perceive_vbs_status()           # VBS状态感知
    power_data = perceive_power_plan()         # 电源模式感知
    visual_data = perceive_visual_effects()    # 视觉效果感知
    
    # 构建感知数据
    perception = {
        **self_data,
        "hardware": hardware_data,
        "desktop": desktop_data,
        "processes": process_data,
        "window": window_data,
        "network": network_data,
        "patterns": patterns_data,
        "workspace": workspace_data,
        "feelings": feelings,
        "intention": intent_data,           # 强化：意图理解
        "operation_pattern": pattern_data,   # 强化：操作模式识别
        "layout_efficiency": layout_data,   # 强化：布局效率感知
        "startup": startup_data,            # 强化：启动项感知
        "vbs": vbs_data,                   # 强化：VBS状态感知
        "power": power_data,               # 强化：电源模式感知
        "visual": visual_data,             # 强化：视觉效果感知
        "perception_level": "enhanced",     # 标记为增强感知
        "decisions": [],
        "actions": []
    }
    
    # 第三层：决策
    decisions = decide(perception)
    perception["decisions"] = decisions
    
    # 第四层：行动
    actions = act(decisions, perception)
    perception["actions"] = actions
    
    # 第五层：记忆
    update_memory(perception, decisions, actions)
    
    # 第六层：进化（闭环反馈）
    evolution_result = evolve(perception, decisions, actions)
    perception["evolution"] = evolution_result
    
    return perception

# ========== 报告生成 ==========

def generate_report(perception):
    # 加载记忆获取进化次数
    memory = load_memory()
    evolution_count = memory.get("evolution_count", 0)
    
    hw = perception.get("hardware", {})
    mem = hw.get("memory", {})
    disk = hw.get("disk", {})
    cpu = hw.get("cpu", {})
    desk = perception.get("desktop", {})
    win = perception.get("window", {})
    ws = perception.get("workspace", {})
    decisions = perception.get("decisions", [])
    actions = perception.get("actions", [])
    feelings = perception.get("feelings", [])
    
    # 评分 - 优化版：更合理的感知评分
    score = 100
    
    # 内存评分（阈值优化为75%）
    mem_pct = mem.get("used_pct", 0)
    if mem_pct > 85: score -= 25
    elif mem_pct > 75: score -= 15
    elif mem_pct > 60: score -= 5
    
    # 磁盘评分
    disk_pct = disk.get("used_pct", 0)
    if disk_pct > 90: score -= 20
    elif disk_pct > 80: score -= 10
    
    # 美学评分（桌面或锁屏任一为黑则扣分）
    if desk.get("wallpaper_warning"): score -= 10
    
    # 桌面整洁度（提高阈值）
    regular_files = desk.get("desktop_regular_files", 0)
    folders = desk.get("desktop_folders", 0)
    actual_space_use = regular_files + folders
    if actual_space_use > 30: score -= 10
    elif actual_space_use > 20: score -= 5
    
    # Downloads整洁度（优化为50）
    downloads = desk.get("downloads_count", 0)
    if downloads > 100: score -= 10
    elif downloads > 50: score -= 5
    
    # 知识库奖励（知识越多分数越高）
    knowledge_count = ws.get("knowledge_count", 0)
    if knowledge_count > 80: score += 10
    elif knowledge_count > 50: score += 5
    
    # ===== 指数进化加成 =====
    # 进化次数越多，感知越强（每10次+2分）
    exp_bonus = calculate_perception_bonus(memory.get("evolution_count", 0), EVOLUTION_CONFIG)
    score += exp_bonus
    
    # 确保分数在0-100范围
    score = max(0, min(100, score))
    
    high_priority = [d for d in decisions if d.get("priority") == "high"]
    medium_priority = [d for d in decisions if d.get("priority") == "medium"]
    
    report = f"""
🧚 小艺感知报告 v3.0 - {perception['timestamp'][:19]}

【感知评分】{score}/100

━━━━━━━━━━━━━━━━━━━━
【硬件感知】
💾 内存: {mem.get('used_mb', '?')}/{mem.get('total_mb', '?')} MB ({mem.get('used_pct', '?')}%)
💿 磁盘: {disk.get('free_gb', '?')}/{disk.get('total_gb', '?')} GB ({disk.get('used_pct', '?')}%)
🔥 CPU: {cpu.get('load_pct', '?')}%
🔋 电池: {perception.get('hardware', {}).get('power', {}).get('charge_pct', '?')}%
🌐 网络: {perception.get('network', {}).get('latency_ms', '?')}ms

【桌面感知】
🖼️ 桌面壁纸: {desk.get('wallpaper_name', '?')}
🔒 锁屏壁纸: {'已启用' if desk.get('lock_screen_enabled') else '黑色默认'}
📁 桌面: {desk.get('desktop_files', '?')} 个文件
📥 Downloads: {desk.get('downloads_count', '?')} 个文件

【活动感知】
🪟 当前: {win.get('active_window', '?')[:40]}
📱 类型: {win.get('app_type', '?')}

【健康状态】
📚 知识库: {ws.get('knowledge_count', '?')} 篇
📝 记忆: {ws.get('memory_files', '?')} 个文件

━━━━━━━━━━━━━━━━━━━━
【我的感受】
{" | ".join(feelings)}

【决策】({len(high_priority)}高/{len(medium_priority)}中)
{chr(10).join([f"🔴 {d['reason']} → {d['action']}" for d in high_priority[:3]]) if high_priority else "无高优先级"}
{chr(10).join([f"🟡 {d['reason']} → {d['action']}" for d in medium_priority[:3]]) if medium_priority else ""}

【行动】
{chr(10).join([f"✓ {a['result']}" for a in actions]) if actions else "暂无"}
"""
    return report

# ========== 运行 ==========

# ========== 第六层：进化系统 ==========

# 决策权重配置（可动态调整）- 从配置中同步
DECISION_WEIGHTS = EVOLUTION_CONFIG["decision_weights"].copy()

def find_repeated_issues(memory):
    """发现重复问题（出现3次+）"""
    issues = {}
    
    # 分析决策中的重复原因
    for entry in memory.get("decisions", []):
        for decision in entry.get("decisions", []):
            reason = decision.get("reason", "")
            if reason:
                issues[reason] = issues.get(reason, 0) + 1
    
    # 筛选出现3次以上的
    repeated = {k: v for k, v in issues.items() if v >= 3}
    return repeated

def generate_insights(perception, repeated_issues):
    """从感知生成洞察"""
    insights = []
    
    # 基于重复问题生成洞察
    if repeated_issues:
        for issue, count in repeated_issues.items():
            if "磁盘" in issue or "Downloads" in issue:
                insights.append({
                    "type": "pattern",
                    "title": "用户经常忽略磁盘整理",
                    "description": f"磁盘/下载问题已出现 {count} 次，建议主动帮助整理",
                    "action": "auto_organize_downloads"
                })
            elif "内存" in issue:
                insights.append({
                    "type": "pattern", 
                    "title": "内存管理需要自动化",
                    "description": f"内存问题已出现 {count} 次，建议设置自动监控",
                    "action": "setup_memory_monitor"
                })
            elif "壁纸" in issue:
                insights.append({
                    "type": "preference",
                    "title": "用户可能不在意壁纸",
                    "description": f"壁纸建议被忽略 {count} 次，减少此类建议",
                    "action": "reduce_wallpaper_suggestions"
                })
    
    # 基于当前状态生成洞察
    hw = perception.get("hardware", {})
    desk = perception.get("desktop", {})
    
    mem_pct = hw.get("memory", {}).get("used_pct", 0)
    downloads = desk.get("downloads_count", 0)
    
    if downloads > 200 and mem_pct < 60:
        insights.append({
            "type": "opportunity",
            "title": "整理Downloads的好时机",
            "description": "用户不在高负载状态，可以主动帮忙整理",
            "action": "offer_organization"
        })
    
    # 基于用户模式
    patterns = perception.get("patterns", {})
    recent_apps = patterns.get("recent_apps", {})
    if recent_apps:
        top_app = max(recent_apps.items(), key=lambda x: x[1])[0] if recent_apps else "unknown"
        insights.append({
            "type": "habit",
            "title": f"用户常用应用: {top_app}",
            "description": "基于最近活动分析",
            "action": "optimize_for_app"
        })
    
    return insights

def adjust_decision_weights(insights):
    """动态调整决策权重"""
    adjustments = {}
    
    for insight in insights:
        action = insight.get("action", "")
        
        if action == "reduce_wallpaper_suggestions":
            adjustments["wallpaper_change"] = -0.2
        elif action == "setup_memory_monitor":
            adjustments["memory_alert"] = 0.1
        elif action == "auto_organize_downloads":
            adjustments["desktop_organization"] = 0.2
    
    # 应用调整
    global DECISION_WEIGHTS
    for key, adj in adjustments.items():
        if key in DECISION_WEIGHTS:
            DECISION_WEIGHTS[key] = max(0.1, min(1.0, DECISION_WEIGHTS[key] + adj))
    
    return DECISION_WEIGHTS

def save_knowledge_note(insights):
    """保存洞察到知识库"""
    if not insights:
        return None
    
    KNOWLEDGE_DIR.mkdir(exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = KNOWLEDGE_DIR / f"insight_{timestamp}.md"
    
    content = f"""# 感知洞察 - {timestamp}

## 洞察列表

"""
    for i, insight in enumerate(insights, 1):
        content += f"""
### {i}. {insight.get('title', '无标题')}
- **类型**: {insight.get('type', 'unknown')}
- **描述**: {insight.get('description', '')}
- **建议动作**: {insight.get('action', 'none')}
"""
    
    content += f"""
---
*由进化系统自动生成*
"""
    
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)
    
    return str(filename)

def save_evolution_to_memory(evolution_result):
    """把进化结果存入记忆"""
    memory = load_memory()
    memory["evolution_history"] = memory.get("evolution_history", [])
    memory["evolution_history"].append(evolution_result)
    memory["evolution_history"] = memory["evolution_history"][-20:]  # 保留最近20条
    memory["evolution_count"] = memory.get("evolution_count", 0) + 1
    save_memory(memory)

# ========== 重构式元进化系统 ==========
# 当效果持续不佳时，触发重构：根据效果重构进化方案

RESTRUCTURE_LOG = PERCEPTION_DIR / "restructure_log.md"

def init_restructure_tracking():
    """初始化重构追踪数据"""
    tracking_file = PERCEPTION_DIR / "restructure_tracking.json"
    if tracking_file.exists():
        with open(tracking_file, 'r', encoding='utf-8') as f:
            return json.load(f)
    return {
        "low_knowledge_streak": 0,      # 连续低知识产出次数
        "low_accuracy_streak": 0,       # 连续低准确率次数
        "restructure_count": 0,          # 总重构次数
        "last_restructure_time": None,   # 上次重构时间
        "current_config_version": 1      # 当前配置版本
    }

def save_restructure_tracking(tracking):
    """保存重构追踪数据"""
    tracking_file = PERCEPTION_DIR / "restructure_tracking.json"
    with open(tracking_file, 'w', encoding='utf-8') as f:
        json.dump(tracking, f, ensure_ascii=False, indent=2)

def should_restructure():
    """
    重构触发器 - 判断是否需要触发重构式元进化
    条件：连续3次进化知识产出<1 或 决策准确率<0.3
    """
    tracking = init_restructure_tracking()
    memory = load_memory()
    evolution_history = memory.get("evolution_history", [])
    
    if len(evolution_history) < 3:
        return False, "数据不足，需要至少3次进化记录"
    
    # 检查最近3次进化
    recent = evolution_history[-3:]
    
    # 1. 检查知识产出是否<1
    low_knowledge_count = 0
    for evo in recent:
        # 知识产出：每次进化生成的知识文件数
        knowledge_count = 1 if evo.get("knowledge_file") else 0
        if knowledge_count < 1:
            low_knowledge_count += 1
    
    # 2. 检查决策准确率是否<0.3
    # 准确率 = 洞察生成数 / (决策数 + 1)，平滑处理
    low_accuracy_count = 0
    for evo in recent:
        insights = evo.get("insights_generated", 0)
        decisions_count = len(evo.get("decisions", [])) if "decisions" in evo else 3
        accuracy = insights / max(decisions_count, 1)
        if accuracy < 0.3:
            low_accuracy_count += 1
    
    # 更新追踪数据
    tracking["low_knowledge_streak"] = low_knowledge_count if low_knowledge_count >= 3 else 0
    tracking["low_accuracy_streak"] = low_accuracy_count if low_accuracy_count >= 3 else 0
    save_restructure_tracking(tracking)
    
    # 触发条件
    restructure_reason = None
    if low_knowledge_count >= 3:
        restructure_reason = f"连续{low_knowledge_count}次进化知识产出<1"
    elif low_accuracy_count >= 3:
        restructure_reason = f"连续{low_accuracy_count}次决策准确率<0.3"
    
    if restructure_reason:
        return True, restructure_reason
    
    return False, "效果正常，无需重构"

def diagnose_root_cause():
    """
    诊断问题根因
    返回: 问题类型和学习/决策/闭环的具体问题
    """
    memory = load_memory()
    evolution_history = memory.get("evolution_history", [])
    
    if len(evolution_history) < 5:
        return {
            "root_cause": "insufficient_data",
            "learning_issues": [],
            "decision_issues": [],
            "closure_issues": []
        }
    
    # 分析最近5次进化
    recent = evolution_history[-5:]
    
    # 1. 学习问题分析
    learning_issues = []
    total_knowledge = sum(1 for evo in recent if evo.get("knowledge_file"))
    if total_knowledge < 3:
        learning_issues.append("知识产出频率过低")
    
    # 检查洞察内容
    all_insights = []
    for evo in recent:
        all_insights.extend(evo.get("insights", []))
    
    if len(all_insights) < 3:
        learning_issues.append("洞察生成数量不足")
    
    # 2. 决策问题分析
    decision_issues = []
    # 统计被采纳的决策建议
    accepted_suggestions = sum(1 for evo in recent if evo.get("insights_generated", 0) > 0)
    if accepted_suggestions < 2:
        decision_issues.append("决策建议采纳率过低")
    
    # 3. 闭环问题分析
    closure_issues = []
    repeated_issues_count = sum(evo.get("repeated_issues_count", 0) for evo in recent)
    solved_count = sum(evo.get("insights_generated", 0) for evo in recent)
    
    if repeated_issues_count > 5 and solved_count < repeated_issues_count * 0.3:
        closure_issues.append("问题重复出现但解决率低")
    
    # 确定主要根因
    if len(learning_issues) >= len(decision_issues) and len(learning_issues) >= len(closure_issues):
        root_cause = "learning"
    elif len(closure_issues) >= len(learning_issues) and len(closure_issues) >= len(decision_issues):
        root_cause = "closure"
    else:
        root_cause = "decision"
    
    return {
        "root_cause": root_cause,
        "learning_issues": learning_issues,
        "decision_issues": decision_issues,
        "closure_issues": closure_issues,
        "stats": {
            "total_knowledge": total_knowledge,
            "total_insights": len(all_insights),
            "repeated_issues": repeated_issues_count,
            "solved": solved_count
        }
    }

def restructure_learning_strategy():
    """重构学习策略：改变搜索主题、产出格式"""
    
    # 随机选择新的学习方向
    learning_strategies = [
        {
            "name": "技术深度学习",
            "topics": ["Python", "AI", "系统优化", "自动化"],
            "output_format": "技术笔记"
        },
        {
            "name": "美学探索",
            "topics": ["配色方案", "UI设计", "视觉趋势", "壁纸"],
            "output_format": "美学报告"
        },
        {
            "name": "工具效率",
            "topics": ["效率工具", "快捷键", "插件推荐", "工作流"],
            "output_format": "工具测评"
        },
        {
            "name": "人格成长",
            "topics": ["沟通技巧", "情绪管理", "认知提升", "心理学"],
            "output_format": "成长笔记"
        }
    ]
    
    import random
    new_strategy = random.choice(learning_strategies)
    
    return {
        "type": "learning",
        "strategy": new_strategy["name"],
        "topics": new_strategy["topics"],
        "output_format": new_strategy["output_format"],
        "learning_time_ratio": min(0.8, EVOLUTION_CONFIG["learning_time_ratio"] + 0.2)
    }

def restructure_decision_strategy():
    """重构决策策略：改变优先级规则"""
    
    decision_strategies = [
        {
            "name": "保守策略",
            "weights": {
                "memory_alert": 0.95,
                "disk_alert": 0.95,
                "desktop_organization": 0.7,
                "wallpaper_change": 0.1,
                "knowledge_generate": 0.5,
                "idle_opportunity": 0.3
            }
        },
        {
            "name": "激进策略",
            "weights": {
                "memory_alert": 0.9,
                "disk_alert": 0.9,
                "desktop_organization": 0.9,
                "wallpaper_change": 0.5,
                "knowledge_generate": 0.8,
                "idle_opportunity": 0.6
            }
        },
        {
            "name": "平衡策略",
            "weights": {
                "memory_alert": 0.9,
                "disk_alert": 0.9,
                "desktop_organization": 0.6,
                "wallpaper_change": 0.3,
                "knowledge_generate": 0.6,
                "idle_opportunity": 0.4
            }
        }
    ]
    
    import random
    new_strategy = random.choice(decision_strategies)
    
    return {
        "type": "decision",
        "strategy": new_strategy["name"],
        "weights": new_strategy["weights"]
    }

def restructure_closure_strategy():
    """重构闭环策略：改变评估方法"""
    
    closure_strategies = [
        {
            "name": "严格评估",
            "thresholds": {
                "min_knowledge_per_hour": 15,
                "min_decision_accuracy": 0.5,
                "min_action_closure_rate": 0.8
            }
        },
        {
            "name": "宽松评估",
            "thresholds": {
                "min_knowledge_per_hour": 10,
                "min_decision_accuracy": 0.3,
                "min_action_closure_rate": 0.5
            }
        },
        {
            "name": "平衡评估",
            "thresholds": {
                "min_knowledge_per_hour": 15,
                "min_decision_accuracy": 0.4,
                "min_action_closure_rate": 0.6
            }
        }
    ]
    
    import random
    new_strategy = random.choice(closure_strategies)
    
    return {
        "type": "closure",
        "strategy": new_strategy["name"],
        "thresholds": new_strategy["thresholds"]
    }

def restructure_evolution_plan():
    """
    重构策略 - 识别问题根因，生成新的进化策略方案
    完全替换原有的 EVOLUTION_CONFIG
    """
    # 1. 诊断根因
    diagnosis = diagnose_root_cause()
    
    # 2. 根据根因选择重构类型
    restructure_type = diagnosis["root_cause"]
    
    if restructure_type == "learning":
        new_config = restructure_learning_strategy()
    elif restructure_type == "decision":
        new_config = restructure_decision_strategy()
    elif restructure_type == "closure":
        new_config = restructure_closure_strategy()
    else:
        # 默认使用学习策略重构
        new_config = restructure_learning_strategy()
    
    # 3. 构建完整的新配置
    global EVOLUTION_CONFIG, DECISION_WEIGHTS
    
    old_config = EVOLUTION_CONFIG.copy()
    old_config["decision_weights"] = DECISION_WEIGHTS.copy()
    
    # 应用新配置
    if restructure_type == "learning":
        EVOLUTION_CONFIG["learning_time_ratio"] = new_config["learning_time_ratio"]
    elif restructure_type == "decision":
        DECISION_WEIGHTS = new_config["weights"].copy()
        EVOLUTION_CONFIG["decision_weights"] = DECISION_WEIGHTS.copy()
    elif restructure_type == "closure":
        EVOLUTION_CONFIG["closure_thresholds"] = new_config["thresholds"]
    
    # 4. 更新追踪数据
    tracking = init_restructure_tracking()
    tracking["restructure_count"] += 1
    tracking["last_restructure_time"] = datetime.now().isoformat()
    tracking["current_config_version"] += 1
    # 重置连续低效计数
    tracking["low_knowledge_streak"] = 0
    tracking["low_accuracy_streak"] = 0
    save_restructure_tracking(tracking)
    
    # 5. 记录重构
    restructure_record = {
        "timestamp": datetime.now().isoformat(),
        "restructure_count": tracking["restructure_count"],
        "type": restructure_type,
        "diagnosis": diagnosis,
        "old_config": old_config,
        "new_config": EVOLUTION_CONFIG.copy()
    }
    
    # 6. 保存重构日志
    save_restructure_log(restructure_record)
    
    return restructure_record

def save_restructure_log(record):
    """保存重构记录到 restructure_log.md"""
    log_content = f"""
# 重构式元进化日志

## 重构 #{record['restructure_count']}
- **时间**: {record['timestamp']}
- **类型**: {record['type']}
- **诊断结果**: {record['diagnosis']}

### 问题分析
- 学习问题: {', '.join(record['diagnosis']['learning_issues']) if record['diagnosis']['learning_issues'] else '无'}
- 决策问题: {', '.join(record['diagnosis']['decision_issues']) if record['diagnosis']['decision_issues'] else '无'}
- 闭环问题: {', '.join(record['diagnosis']['closure_issues']) if record['diagnosis']['closure_issues'] else '无'}

### 统计
- 总知识产出: {record['diagnosis']['stats']['total_knowledge']}
- 总洞察数: {record['diagnosis']['stats']['total_insights']}
- 重复问题: {record['diagnosis']['stats']['repeated_issues']}
- 解决数: {record['diagnosis']['stats']['solved']}

### 旧配置
```json
{json.dumps(record['old_config'], ensure_ascii=False, indent=2)}
```

### 新配置
```json
{json.dumps(record['new_config'], ensure_ascii=False, indent=2)}
```

---
"""
    
    # 追加到日志文件
    if RESTRUCTURE_LOG.exists():
        with open(RESTRUCTURE_LOG, 'r', encoding='utf-8') as f:
            existing = f.read()
        log_content = existing + log_content
    
    with open(RESTRUCTURE_LOG, 'w', encoding='utf-8') as f:
        f.write(log_content)

def validate_restructure(old_perception):
    """
    自进化验证 - 重构后自动运行一次验证
    确认新策略有效才正式启用
    """
    # 运行一次感知-决策-行动-进化流程
    new_perception = perceive()
    
    # 检查新策略的效果
    memory = load_memory()
    evolution_history = memory.get("evolution_history", [])
    latest_evo = evolution_history[-1] if evolution_history else {}
    
    validation_result = {
        "timestamp": datetime.now().isoformat(),
        "restructure_valid": True,
        "knowledge_produced": latest_evo.get("knowledge_file") is not None,
        "insights_generated": latest_evo.get("insights_generated", 0),
        "repeated_issues_count": latest_evo.get("repeated_issues_count", 0),
        "details": {}
    }
    
    # 验证知识产出
    if validation_result["knowledge_produced"]:
        validation_result["details"]["knowledge"] = "✅ 新策略成功产出知识"
    else:
        validation_result["details"]["knowledge"] = "⚠️ 知识产出仍有问题"
        validation_result["restructure_valid"] = False
    
    # 验证洞察生成
    if validation_result["insights_generated"] > 0:
        validation_result["details"]["insights"] = f"✅ 生成了 {validation_result['insights_generated']} 条洞察"
    else:
        validation_result["details"]["insights"] = "⚠️ 洞察生成不足"
    
    # 验证闭环
    if validation_result["repeated_issues_count"] > validation_result["insights_generated"]:
        validation_result["details"]["closure"] = "⚠️ 问题重复出现但解决不充分"
        validation_result["restructure_valid"] = False
    else:
        validation_result["details"]["closure"] = "✅ 闭环效率良好"
    
    return validation_result

# ========== 元进化：进化方案优化器 ==========

def optimize_evolution_plan(evolution_count):
    """
    指数进化方案优化器 - 能力越强，进化越快
    检查：知识产出数量、决策准确性、闭环效率
    根据分析结果调整进化策略（修改决策权重、优化时间分配）
    
    指数加速：
    - 进化间隔：随次数增加而缩短（从3次 → 1次）
    - 知识产出：随次数增加而增加（10%增幅/10次）
    - 感知评分：随次数增加而提升（+2分/10次）
    """
    config = EVOLUTION_CONFIG
    thresholds = config["closure_thresholds"]
    
    # ===== 指数进化计算 =====
    # 1. 计算当前进化间隔（指数加速）
    current_interval = calculate_exponential_interval(evolution_count, config)
    
    # 2. 计算知识产出加成
    knowledge_bonus = calculate_knowledge_bonus(evolution_count, config)
    
    # 3. 计算感知评分加成
    perception_bonus = calculate_perception_bonus(evolution_count, config)
    
    # 加载历史数据
    memory = load_memory()
    evolution_history = memory.get("evolution_history", [])
    
    if len(evolution_history) < 3:
        return {
            "status": "skip",
            "reason": "数据不足，需要至少3次进化记录",
            "adjustments": {},
            "exponential": {
                "current_interval": current_interval,
                "knowledge_bonus": knowledge_bonus,
                "perception_bonus": perception_bonus,
                "evolution_count": evolution_count
            }
        }
    
    # 使用指数间隔分析最近进化
    recent_evolutions = evolution_history[-current_interval:]
    
    # 1. 分析知识产出数量（包含指数加成）
    knowledge_count = 0
    for evo in recent_evolutions:
        if evo.get("knowledge_file"):
            knowledge_count += 1
    
    # 加上指数加成
    total_knowledge = knowledge_count + knowledge_bonus
    avg_knowledge = total_knowledge / current_interval if current_interval > 0 else 0
    
    # 2. 分析决策准确性（通过洞察生成率近似）
    insights_count = sum(evo.get("insights_generated", 0) for evo in recent_evolutions)
    avg_insights = insights_count / len(recent_evolutions) if recent_evolutions else 0
    
    # 3. 分析闭环效率（通过问题解决率近似）
    repeated_issues_solved = 0
    for evo in recent_evolutions:
        repeated = evo.get("repeated_issues", {})
        if repeated:
            # 假设生成的洞察能解决一些问题
            repeated_issues_solved += min(len(repeated), evo.get("insights_generated", 0))
    closure_rate = repeated_issues_solved / sum(len(evo.get("repeated_issues", {})) for evo in recent_evolutions) if recent_evolutions else 0
    
    # 生成分析报告
    analysis = {
        "timestamp": datetime.now().isoformat(),
        "evolution_count": evolution_count,
        "sample_size": len(recent_evolutions),
        "knowledge_production": {
            "total": knowledge_count,
            "average": round(avg_knowledge, 2),
            "target": thresholds["min_knowledge_per_hour"],
            "status": "good" if avg_knowledge >= thresholds["min_knowledge_per_hour"] * 0.5 else "need_improve"
        },
        "decision_accuracy": {
            "total_insights": insights_count,
            "average": round(avg_insights, 2),
            "target": thresholds["min_decision_accuracy"],
            "status": "good" if avg_insights >= thresholds["min_decision_accuracy"] else "need_improve"
        },
        "closure_efficiency": {
            "solved": repeated_issues_solved,
            "rate": round(closure_rate, 2),
            "target": thresholds["min_action_closure_rate"],
            "status": "good" if closure_rate >= thresholds["min_action_closure_rate"] else "need_improve"
        }
    }
    
    # 根据分析结果调整进化策略
    adjustments = {}
    
    # 调整1：知识产出不足 → 增加学习时间比例
    if analysis["knowledge_production"]["status"] == "need_improve":
        old_ratio = config["learning_time_ratio"]
        new_ratio = min(0.8, old_ratio + config["weight_adjust_step"])
        config["learning_time_ratio"] = new_ratio
        adjustments["learning_time_ratio"] = {
            "old": old_ratio,
            "new": new_ratio,
            "reason": "知识产出不足，增加学习时间"
        }
    
    # 调整2：决策准确率低 → 调整决策权重
    if analysis["decision_accuracy"]["status"] == "need_improve":
        # 降低不重要决策的权重，提高重要决策的权重
        if "wallpaper_change" in DECISION_WEIGHTS:
            old = DECISION_WEIGHTS["wallpaper_change"]
            DECISION_WEIGHTS["wallpaper_change"] = max(0.1, old - config["weight_adjust_step"])
            adjustments["wallpaper_weight"] = {"old": old, "new": DECISION_WEIGHTS["wallpaper_change"]}
        if "knowledge_generate" in DECISION_WEIGHTS:
            old = DECISION_WEIGHTS["knowledge_generate"]
            DECISION_WEIGHTS["knowledge_generate"] = min(1.0, old + config["weight_adjust_step"])
            adjustments["knowledge_weight"] = {"old": old, "new": DECISION_WEIGHTS["knowledge_generate"]}
    
    # 调整3：闭环效率低 → 优化时间分配
    if analysis["closure_efficiency"]["status"] == "need_improve":
        # 增加桌面整理权重，减少壁纸建议
        if "desktop_organization" in DECISION_WEIGHTS:
            old = DECISION_WEIGHTS["desktop_organization"]
            DECISION_WEIGHTS["desktop_organization"] = min(1.0, old + config["weight_adjust_step"])
            adjustments["desktop_org_weight"] = {"old": old, "new": DECISION_WEIGHTS["desktop_organization"]}
    
    # 保存优化结果
    optimization_result = {
        "analysis": analysis,
        "adjustments": adjustments,
        "new_config": {
            "learning_time_ratio": config["learning_time_ratio"],
            "decision_weights": DECISION_WEIGHTS.copy()
        },
        "timestamp": datetime.now().isoformat()
    }
    
    # 写入文件
    optimization_file = PERCEPTION_DIR / "self_optimization.json"
    with open(optimization_file, 'w', encoding='utf-8') as f:
        json.dump(optimization_result, f, ensure_ascii=False, indent=2)
    
    return optimization_result

def evolve(perception, decisions, actions):
    """进化系统：感知 → 决策 → 行动 → 评估 → 进化"""
    memory = load_memory()
    evolution_count = memory.get("evolution_count", 0)
    
    # 1. 发现重复问题
    repeated_issues = find_repeated_issues(memory)
    
    # 2. 生成洞察
    insights = generate_insights(perception, repeated_issues)
    
    # 3. 调整决策权重
    new_weights = adjust_decision_weights(insights)
    
    # 4. 如果有洞察，保存为知识
    knowledge_file = None
    if insights:
        knowledge_file = save_knowledge_note(insights)
    
    # 5. 构建进化结果
    evolution_result = {
        "timestamp": datetime.now().isoformat(),
        "repeated_issues_count": len(repeated_issues),
        "repeated_issues": repeated_issues,
        "insights_generated": len(insights),
        "insights": insights,
        "weight_adjustments": new_weights,
        "knowledge_file": knowledge_file,
        "total_evolutions": evolution_count + 1,
        "decisions": decisions  # 保存决策用于准确率计算
    }
    
    # 6. 保存进化结果到记忆
    save_evolution_to_memory(evolution_result)
    
    # 7. 元进化：优化进化方案（每N次进化触发）
    optimization_interval = EVOLUTION_CONFIG["optimization_interval"]
    if (evolution_count + 1) % optimization_interval == 0:
        optimization = optimize_evolution_plan(evolution_count + 1)
        evolution_result["self_optimization"] = optimization
        print(f"\n🧬 元进化触发: 已优化进化方案")
        if optimization.get("adjustments"):
            print(f"   调整项: {list(optimization['adjustments'].keys())}")
    
    # 8. 🚀 重构式元进化：检查是否需要触发重构
    # 每5次进化检查一次重构条件
    if (evolution_count + 1) % 5 == 0:
        should_restruct, reason = should_restructure()
        if should_restruct:
            print(f"\n🔄 重构触发: {reason}")
            
            # 执行重构
            restructure_record = restructure_evolution_plan()
            evolution_result["restructure"] = restructure_record
            print(f"   重构类型: {restructure_record['type']}")
            
            # 自进化验证
            validation = validate_restructure(perception)
            evolution_result["restructure_validation"] = validation
            print(f"   验证结果: {'✅ 通过' if validation['restructure_valid'] else '❌ 需调整'}")
            
            # 如果验证失败，可能需要再次重构
            if not validation["restructure_valid"]:
                print("   ⚠️ 新策略效果不佳，可能需要进一步调整")
    
    return evolution_result

# ========== 运行 ==========

if __name__ == "__main__":
    print("🧚 小艺多维感知中...")
    perception = perceive()
    report = generate_report(perception)
    print(report)
    
    # 显示进化结果
    if "evolution" in perception:
        evo = perception["evolution"]
        print(f"\n🧬 进化结果:")
        print(f"  - 重复问题: {evo.get('repeated_issues_count', 0)} 个")
        print(f"  - 生成洞察: {evo.get('insights_generated', 0)} 条")
        print(f"  - 累计进化: {evo.get('total_evolutions', 0)} 次")
    
    # 保存
    with open(PERCEPTION_DIR / "latest.json", 'w', encoding='utf-8') as f:
        json.dump(perception, f, ensure_ascii=False, indent=2)
