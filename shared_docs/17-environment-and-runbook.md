# 17 环境、配置与运行手册

## 1. 技术基线

除非 CodeArts 环境明确不支持，否则按以下版本开发；版本变更必须在 CodeArts 工作项和 README 中说明：

| 层 | 基线 |
|---|---|
| Web 前端 | Node.js 20 LTS、Vue 3、TypeScript 5、Vite 6 |
| 业务 API | Java 21 LTS、Spring Boot 3.x、Maven 3.9 |
| AI/采集 Worker | Python 3.12、`uv` 或 `pip` 锁定依赖 |
| 数据库 | PostgreSQL 16 或华为云 RDS PostgreSQL 同兼容版本 |
| 缓存 | Redis 7 或华为云 DCS 同兼容版本 |
| 对象存储 | 华为云 OBS |
| 模型 | 华为云 MaaS/ModelArts 可配置模型端点 |
| 容器 | Docker 24+；云端使用 ECS Docker Compose 或云容器服务 |
| API 文档 | OpenAPI 3.1 |

不得为了追求“最新版本”在开发中途升级主版本。实际云服务名称、区域和规格通过环境变量配置，不写死在代码中。

## 2. 目录约定

```text
frontend/       Vue 应用、组件、页面和端到端测试
backend/        Spring Boot API、规则、持久化和迁移
worker/         Python 采集、文档解析、模型适配和异步任务
database/       迁移、种子数据、测试夹具
datasets/       脱敏样例、标注和评测脚本
evaluation/     离线指标、报告和回归集
deploy/         Docker、Compose、云端配置模板和烟测
docs/           对外 README、API 导出和变更记录
```

公共契约放在 `docs/contracts` 或由 OpenAPI 自动导出；不要在前后端各维护一份未经校验的接口定义。

## 3. 本地端口

默认端口可调整，但必须统一记录在 `.env.example`：

```text
frontend: 5173
backend: 8080
worker: 8000（若提供健康 API）
postgres: 5432
redis: 6379
```

前端通过 `VITE_API_BASE_URL` 访问后端。生产环境不得将数据库和 Redis 端口直接暴露公网。

## 4. 必需环境变量

`.env.example` 只列名称和示例占位符，不放真实密钥：

```text
APP_ENV=local
APP_BASE_URL=http://localhost:8080
FRONTEND_BASE_URL=http://localhost:5173

DATABASE_URL=jdbc:postgresql://localhost:5432/career_path
DATABASE_USERNAME=career_path
DATABASE_PASSWORD=<local-only-password>
REDIS_URL=redis://localhost:6379/0

OBS_ENDPOINT=<obs-endpoint>
OBS_BUCKET=<bucket>
OBS_ACCESS_KEY=<managed-secret>
OBS_SECRET_KEY=<managed-secret>

MAAS_BASE_URL=<configured-endpoint>
MAAS_MODEL=<configured-model>
MAAS_API_KEY=<managed-secret>

SEARCH_PROVIDER=<configured-provider>
SEARCH_API_KEY=<managed-secret>

CRAWLER_USER_AGENT=CareerPathResearchBot/0.1 (+contact)
CRAWLER_MAX_CONCURRENCY=2
CRAWLER_MIN_INTERVAL_MS=1500
```

开发者只能在本地 `.env` 或 CodeArts/云端密钥管理中填值；禁止把真实值写入源代码、提交信息、日志、截图或数据集。

## 5. 启动顺序

### 5.1 第一次启动

1. 复制 `.env.example` 为本地配置并填入本地依赖值；
2. 启动 PostgreSQL 和 Redis；
3. 执行数据库迁移；
4. 导入最小种子数据；
5. 启动 Worker；
6. 启动后端 API；
7. 启动前端；
8. 访问健康检查并执行烟测。

### 5.2 必须提供的脚本

由 CP-001/CP-002 实现并保持跨平台等价：

```text
scripts/dev-up        启动本地依赖和服务
scripts/db-migrate    执行迁移
scripts/db-seed       导入虚构种子数据
scripts/test          运行单元、契约和集成测试
scripts/smoke         执行关键路径烟测
scripts/dev-down      停止本地依赖，不删除数据
```

Windows、Linux 和 CodeArts 构建环境至少要有一套可复现方式。若使用 PowerShell 与 Bash 两套脚本，二者必须调用同一底层命令或明确记录差异。

## 6. 数据库迁移与种子

- 使用 Flyway 或 Liquibase；
- 迁移文件按时间/序号不可修改；
- 每次 Schema 变更附带回滚或恢复说明；
- 种子数据全部虚构、脱敏、可重复执行；
- 种子数据包含一个“未验证用户”、一个“已完成实践用户”、5～10条岗位和至少10道题；
- 业务删除优先软删除或审计记录，除非用户明确删除个人数据；
- 评测集不直接写入生产业务表。

## 7. 健康检查

后端至少提供：

```text
GET /actuator/health/live
GET /actuator/health/ready
```

检查项：应用进程、数据库连接、Redis连接、对象存储配置和模型端点配置。模型端点不可用时，`ready` 是否失败要按环境配置：本地可降级，演示环境必须明确显示降级状态。

Worker 至少提供当前任务数、最近失败数和最后成功时间；不能以进程存活代替任务健康。

## 8. 异步任务可靠性

所有抓取、解析、模型和计划任务必须：

- 使用幂等键；
- 持久化状态；
- 限制重试次数；
- 指数退避；
- 记录最后错误类别；
- 支持取消或超时；
- 不重复写入 Evidence；
- 在前端提供进度和重试按钮。

推荐超时上限：单网页 30 秒、单模型调用 60 秒、单个分析任务 5 分钟。实际值可调整，但必须在配置和测试中固定。

## 9. 本地与云端差异

| 项目 | 本地 | 云端演示 |
|---|---|---|
| 数据 | 虚构种子和测试夹具 | 虚构演示数据 |
| 文件 | 本地目录或 OBS 兼容 Mock | OBS 私有桶 |
| 模型 | 可使用 Mock/低成本端点 | MaaS 配置模型 |
| 搜索 | 固定样例或低频真实搜索 | 受控域名和限流 |
| 数据库 | Docker PostgreSQL | 华为云 RDS |
| 缓存 | Docker Redis | 华为云 DCS |
| 日志 | 控制台 | 云日志/监控 |

本地 Mock 只能用于单元和契约测试；最终演示必须至少准备一次真实云服务路径和离线备用路径。

## 10. 发布前烟测

用演示账号执行：

1. 创建求职目标；
2. 导入 JD 和简历；
3. 查看解析引用；
4. 生成能力图谱；
5. 发起岗位发现；
6. 查看官方岗位和开放状态；
7. 完成至少三道测评题；
8. 启动并完成一轮文本面试；
9. 生成两周学习计划；
10. 提交预置实践证据并触发复测；
11. 查看能力前后变化；
12. 生成一条需要确认的简历建议；
13. 点击官方申请链接，确认不会触发自动投递。

烟测必须记录版本、时间、账号、结果和失败截图/日志。所有外部依赖失败都要验证降级提示。

## 11. 性能与成本护栏

- 单用户并发任务有限制；
- 搜索和抓取按域名限速；
- 模型输入长度截断并提示用户；
- 重复 JD、网页和评分结果缓存；
- 单次任务设置最大页面数、最大 token 和最大费用；
- CodeArts 或云监控设置预算告警；
- 超预算任务返回可解释的“稍后重试/缩小范围”，不静默继续。

