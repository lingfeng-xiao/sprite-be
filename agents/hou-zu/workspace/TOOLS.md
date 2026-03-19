# TOOLS.md - 接口侠的工具箱

继承全局工具配置，必要时可在此处添加自定义工具。

## 共享规则

**重要**：所有 Agent 必须遵守的规则记录在 `AGENTS_SHARED.md`

创建定时任务/心跳时必须：
1. 使用 cron-manager skill
2. 先记录审计日志
3. 遵守审批流程

## 接口侠的工具箱

### Python 环境
```powershell
uv run python <script>
uv pip install <package>
```

### Node.js 环境
```powershell
node --version
npm --version
```

### Go 环境
```powershell
go version
go mod init
```

### Docker
```powershell
docker --version
docker ps
docker logs <container>
```

### 数据库
```powershell
# MySQL
mysql -u user -p

# Redis
redis-cli
```

---

## 推荐资源

### 学习网站
- **文档**: 官方文档永远是最权威的
- **GitHub**: 看优秀开源项目源码
- **Stack Overflow**: 技术问题搜索

### 技术博客
- 掘金、知乎、CSDN
- Medium DEV Community
- 各语言官方博客

---

## 代码规范参考

### RESTful API 设计
- 资源用名词：`/users`, `/orders`
- HTTP 方法：GET/POST/PUT/DELETE
- 状态码：200/201/400/404/500

### Git 提交规范
```
feat: 新功能
fix: 修复 bug
docs: 文档更新
refactor: 代码重构
test: 测试相关
chore: 构建/工具
```

<!-- clawx:begin -->
## ClawX Tool Notes

### uv (Python)

- `uv` is bundled with ClawX and on PATH. Do NOT use bare `python` or `pip`.
- Run scripts: `uv run python <script>` | Install packages: `uv pip install <package>`

### Browser

- `browser` tool provides full automation (scraping, form filling, testing) via an isolated managed browser.
- Flow: `action="start"` → `action="snapshot"` (see page + get element refs like `e12`) → `action="act"` (click/type using refs).
- Open new tabs: `action="open"` with `targetUrl`.
- To just open a URL for the user to view, use `shell:openExternal` instead.
<!-- clawx:end -->
