# 18 当前项目状态与下一步

> 本文件是云码道判断“现在做到哪里”的唯一事实来源。每次完成一个 backlog 任务后更新本文件；不要根据旧对话、旧截图或模型推断更新状态。

## 1. 状态快照

状态日期：2026-09-21

| 项目 | 当前事实 | 状态 |
|---|---|---|
| 根目录 | `D:\MyProjects\ICT` | 已确认 |
| Git 仓库 | 已初始化，当前分支 `main` | 已完成 |
| 代码托管 | `origin` = `https://github.com/pppeach111/JDdriven-Jobprep.git`（GitHub，当前唯一远端）；CodeArts 远端已于 2026-09-16 移除 | 已确认，与 `11` 号文档冲突待确认 |
| 最新提交 | `b3f6bc3 docs: record contract fixes and end-to-end verification`（累计 16 个提交）；`main` 与 `origin/main` 指向同一提交、无待推送，此前“因 `github.com:443` 不可达尚未推送”的记录已不成立 | 已提交，远端已同步 |
| 工作区 | 有未提交改动：第 5 节 E 的导航与图谱改造已实现（新增 `frontend/src/composables/useCurrentGoal.ts`、`frontend/src/components/GoalSwitcher.vue`，修改 `App.vue` / `GoalCreateView.vue` / `CapabilityMapView.vue`，并同步 `19` 号文档 §3.2 / §4.6 / §18）；本文档改动同样未提交 | 待提交 |
| 产品代码 | 后端 36 个 Java 源文件可编译；前端 Vue 3 + TS 骨架可构建；Worker Python 骨架可启动并通过健康检查 | 已产出，核心闭环已实测 |
| 后端测试 | 84 项全部通过（Failures/Errors/Skipped 均为 0）；其中 `RuleBasedJdParserTest` 由 32 项增至 38 项 | 本地验证完成 |
| Worker 测试 | 79 项通过（`uv run pytest`）；`/health` 与 `/health/ready` 已实测响应（本地 `ready:true`，`degraded:["model"]`） | 本地验证完成 |
| 前端设计系统符合度 | `frontend/` 已按 `19` 号文档重构（浅色靛蓝紫、顶部横向导航、玻璃拟态受 §4.5 约束），并完成 Beautiful UI 本地适配；本轮导航与图谱改造后已重跑四档视口 + 键盘可达性 + 对比度验收，全部通过 | 已符合，视觉与无障碍验收矩阵已执行 |
| 数据库与端到端 | 本地 PostgreSQL 17.5 已创建 `career_path` 角色与库，Flyway 迁移至 v3；`目标 → JD 导入 → 解析 → 能力图谱 → 前端呈现` 已用真实数据实测跑通 | **已解除阻塞，已实测** |
| 产品名称 | “职径（CareerPath）”仅为暂定文案，尚未最终确定 | 待定 |
| 规格文档 | `shared_docs/00`～`19` 与索引已存在 | 已完成 |
| 本地 CodeArts 目录 | `.codeartsdoer/` 存在，但被根 `.gitignore` 排除 | 本地工具状态 |
| 项目级技能 | `tools/codearts-skills/` 已包含 `careerpath-ui` 与 11 个经许可核对的前端设计技能，可安装到本地云码道 | 已桥接 |
| 前端设计系统 | `shared_docs/19-ui-design-system.md` 已定义方向、令牌、页面模板和验收矩阵 | 已完成 |
| 华为云资源 | 尚未确认区域、实例、配额、凭据和服务端点 | 待确认 |
| CodeArts 云端项目 | 本文档无法证明已创建或已建立工作项 | 待确认 |
| 用户实验/评测 | 尚未开始收集正式数据 | 未开始 |

## 2. 对此前云码道总结的修正

以下说法不应再使用：

- “Git 无提交历史、无分支”：错误。根仓库已存在 `main` 分支，并有首个提交 `8ee65fc`，已推送 GitHub。
- “CP-001 尚未开始”：不准确。CP-001 的 Git 初始化、远端关联、根 README、忽略规则、基础属性和应用骨架已完成；CodeArts 云端项目、工作项、流水线与云资源仍未完成。
- “shared_docs 有18份文档”：需要说明统计口径。当前 `shared_docs` 包含 `00`～`19` 共20份编号文档，另有一份目录 README；根目录另有一份 README。数量不是开发状态判断依据。
- “需求文档非常完整”不能等同于“项目已完成”。业务代码、迁移和测试现已在工作区产出，但尚未提交，也未在真实数据库和端到端路径上验证；云端部署未开始。
- “职径（CareerPath）”不能视为已经确定的品牌名。实现阶段使用 `jobprep`/`jd-driven-job-prep` 作为技术标识，待产品命名确认后再替换界面文案。
- “当前应立即询问用户再开始”：不必阻塞所有开发。可以先完成不依赖外部账号的 CP-001 剩余工作和 CP-002 契约骨架；外部确认事项单独登记。
- “`ui-ux-pro-max` 因沙箱只读无法安装”：错误。项目级 `.codeartsdoer/skills/ui-ux-pro-max` 与用户级目录均已存在，`careerpath-ui` 同样已启用。安装受阻的说法不再使用。
- “前端骨架已完成”：需限定。`npm run build` 通过只证明可构建，不代表符合设计系统。该实现违反 `19` 号文档的强制约束，见第 5 节 A。
- “后端编译通过即等于契约实现正确”：不成立。编译与单测通过不覆盖数据库契约和端到端语义，且第 5 节 B 已记录 5 项已知契约缺陷。

## 3. CP-001 完成度

### 已完成

- 新 Git 仓库初始化；
- `main` 分支建立；
- GitHub `origin` 配置；
- 首次文档提交并推送；
- 根 `.gitignore` 和 `.gitattributes`；
- 根 README 与 `shared_docs` 导航。
- 可版本化的 `careerpath-ui` 项目技能、安装脚本和前端设计系统；
- `.env.example`；
- `17` 号文档规定的 6 个脚本：`scripts/dev-up`、`dev-down`、`db-migrate`、`db-seed`、`test`、`smoke`；
- 本地依赖编排 `deploy/docker-compose.yml`（PostgreSQL 16 + Redis 7，含健康检查与卷）；
- 虚构种子数据 `database/seed/seed_demo.sql`；
- `docs/` 与 `docs/contracts/` 目录；
- 后端 Spring Boot 骨架（36 个 Java 源文件）、前端 Vue 3 骨架、Worker Python 骨架；
- 健康检查：Worker `/health` 与 `/health/ready` 已实测；后端 actuator 已配置；
- Maven Wrapper（`backend/mvnw`），使构建不依赖全局 Maven。

### 未完成

- CodeArts 云端项目和工作项；
- CodeArts 编程智能体使用记录方案；
- 第一条 CI 流水线；
- 演示环境和云资源；
- 全部应用代码仍为工作区未跟踪文件，尚无对应提交；
- `frontend/` 不符合 `19` 号设计系统（见第 5 节 A）。

因此，CP-001 状态仍为 **PARTIAL**，不能标记 `ACCEPTED`。本地骨架部分已基本齐备，剩余缺口集中在 CodeArts 云端、CI 与云资源。

## 4. CP-002 完成度与 CP-003 状态

### CP-002 已完成

- `14` 号契约已落为后端 DTO 与枚举：`ApiResponse`/`ErrorResponse`（含 `schemaVersion`、`traceId`、`ErrorCode`）、`CreateGoalRequest`、`JobGoal`、`ImportJdRequest`、`JdParseResult`、`RequirementView`、`CapabilityMap`、`SkillCard`；
- 数据库迁移草案 `backend/src/main/resources/db/migration/V1__init_schema.sql`、`V2__seed_skills.sql`；
- `backend/pom.xml` 接入 Flyway Maven 插件与 PostgreSQL 驱动；
- 契约测试 79 项（`backend/src/test/java`），覆盖请求校验、响应包装、错误码映射与 JD 解析契约。

### CP-002 未完成

- OpenAPI / JSON Schema 导出，`docs/contracts` 仍为空目录；
- Redis 接入与缓存契约；
- 契约在真实数据库上的迁移与种子验证（依赖第 5 节 D 的数据库凭据）；
- 前端 `src/api/types.ts` 与后端 DTO 的一致性校验手段；
- `CapabilityMap.NextAction` 缺 `taskId`（见第 5 节 B）。

### CP-003 状态

未开始，依赖 CP-002 契约冻结。

## 5. 已知缺陷、不符合项与阻塞

本节只登记已核实的事实，不猜测成因或完成时间。

### A. 前端代码已按新规范重构，待视觉验收（不再阻塞构建，仅阻塞验收）

**变更理由**：2026-09-16 用户在静态预览稿 `frontend/prototype.html`（v2）上确认改用 Avenra AI Dashboard 视觉风格
（浅色 + 靛蓝紫 + 玻璃拟态 + 大圆角 + 顶部横向导航），认为其层级与可读性更贴合“结论—依据—下一步”的阅读路径。
`19-ui-design-system.md` 已按该方向正式修订 7 项条款（该文件 §18 修订台账）。
原先“文档要求浅色墨绿 / 代码为深色靛蓝”的**方向性冲突已在文档层消除**。

**当前状态（2026-09-16 更新）**：`frontend/src` 的有色令牌重构已完成（`theme.css` 全量重写、`App.vue` 外壳重写、
令牌重命名 74 处、组件内颜色字面量清零、`npm run build` 50 modules 通过）；
Plus Jakarta Sans 已自托管（4 个字重 latin 子集，共 48KB，附 OFL 许可证）；
视觉验收矩阵四档视口已执行（结果见下）；验收中自查发现的移动端抽屉缺失已同步修复（见下）。

**视觉验收矩阵实测结果（2026-09-16，`/goals/new`，Edge 153 headless + CDP 实测 DOM）**：

| 档位 | CSS 视口 | 命中媒体查询 | 内容列 | 表单列 | 水平溢出 | 侧栏位置 |
|---|---|---|---|---|---|---|
| 桌面全宽 | 1440×900 | 无 | 双列 823 / 531 | 2 | 0 | 首屏可见（t=275） |
| 桌面窄档 | 1024×768 | 1099 / 1359 | 双列 574 / 370 | 2 | 0 | 首屏可见（t=275） |
| 手机竖屏 | 390×844 | 全部命中 | 单列 358 | 1 | 0 | 下移至 t=1028 |
| 200% 缩放 | 720×450 | 全部命中 | 单列 678 | 1 | 0 | 下移至 t=939 |

四档均无水平溢出，`document.fonts.status` 均为 `loaded`。200% 缩放下侧栏「下移」是单列布局的正常顺序结果
（页面总高 1195px，滚动可达），**不构成 WCAG 1.4.4 意义上的内容丢失或功能失效**；
初轮截图的人工判读曾误报为“卡片丢失 / 断点未触发”，经 DOM 实测否定，此处以实测值为准。

**移动端导航抽屉（已实现并验证）**：重构初期 `@media (max-width: 767px)` 只是把 `.navlinks` 置为
`display: none`（源码注释自称“抽屉位置的占位”），与 `19` 号 §3.3“`<768px` 高度 62px 并进抽屉”不符，
属本轮自查发现的缺陷，现已补齐：

- 新增汉堡入口（`.menu-btn`，桌面档 `display: none`）与右侧抽屉；`19` 号 §4.1 相应新增
  `--color-scrim` 遮罩语义令牌（避免组件内出现颜色字面量，维持令牌单一来源）；
- 抽屉为不透明表面而非玻璃（`--color-surface`，§4.1 令牌用途表已如此规定），避免在内容之上再叠一层大面积模糊（§4.5）；
- 无障碍：`role=dialog` + `aria-modal=true`、Esc 关闭、遮罩点击关闭、打开期锁定背景滚动（`body overflow: hidden`）、
  打开时焦点移入抽屉并在抽屉内循环、关闭时焦点归还触发按钮、视口回到桌面档自动关闭。

**断点体系不统一（待确认是否需修订文档）**：外壳导航断点为 `1359 / 1099 / 767`（对齐 §3.2 / §3.3），
但 `frontend/src/views/GoalCreateView.vue:393` 内容区双栏→单栏用 **900px**，该档位未见于 `19` 号文档。
实测错位区间为 768～900px（导航已图标化 + 内容单列），当前无害，但建议将内容区断点收敛到文档已定义的档位。

**影响范围与迁移方式**：

| 项 | 旧规范 | 新规范（`19` 号） | 代码迁移动作 |
|---|---|---|---|
| 主色 | 墨绿 `#176B5B` | 靛蓝紫 `#5B4BE8` | 替换 `theme.css` 主色令牌；当前代码用的是 `#6366F1`，属第三套值，需一并归一 |
| 主题 | 仅浅色（代码实为深色） | 浅色 `--color-canvas:#F8F7FC` + 双径向淡色光晕 | 删除深色令牌，改用 §4.1 语义表 |
| 玻璃拟态 | 明令禁止 | 允许，受 §4.5 约束 | 卡片改用 `--color-surface-glass` + `blur(18px) saturate(170%)`；禁嵌套、加 `will-change`、补 `@supports` 降级 |
| 渐变 | 明令禁止 | 限主按钮 / 选中导航项 / 数据图形 | 移除装饰性渐变与 `shimmer` 循环动画 |
| 圆角 | 控件 8 / 容器 12 / 模态 14 | 控件 12 / 图标 12～14 / 卡片 24 / 面板 28 / pill | 按 §4.3 令牌替换，清除散落的自定义圆角 |
| 阴影 | 仅浮层 | 常态柔和阴影，边界见 §4.3 | 引入 5 个 `--shadow-*` 令牌，替换内联阴影 |
| 导航 | 240px 左侧栏，收起态 72px | 顶部横向导航 70px sticky，1024 / 1359 两档降级 | `App.vue` 外壳重写；`<768px` 导航进抽屉 |
| 字体 | Inter → 中文系统字体 | 自托管 Plus Jakarta Sans（400/600/700/800） | 加 `@font-face`，不引 Google Fonts CDN |
| 图标 | 一个线性家族 | 固定 Lucide，经 `better-icons` 取真实路径 | 替换现有手写 SVG 近似路径 |
| 品牌命名 | 不依赖 `careerpath` 品牌串 | 展示层用工作名“职径”，令牌/组件名用语义名 | §2.3 已澄清；此项**超出原 7 项**，需队伍知悉 |

**未随本次修订消失的既有缺口**（重构时需一并补齐）：字号分级、4px 间距基数、控件高度与点击区、
页面标题区结构、分组优先级、主从布局比例。视觉验收矩阵（1440×900 / 1024×768 / 390×844 / 200% 缩放）
已于 2026-09-16 执行，结果见上表。

**预览稿边界**：`frontend/prototype.html` 是静态风格稿，卡片数据均为演示值，不是真实集成；
不得作为“页面已完成”的依据（`00` 号强约束与 `19` 号第 14 节禁止用 Mock 结果冒充真实集成）。
预览稿在编码前已知的两项偏离已在本轮修正：重复主按钮（违反 §2.2 / §6.5）、首屏卡片顺序（违反 §7.2）。

### B. 后端契约缺陷（“未知猜成已知”）—— 已于 2026-09-21 修复

以下均位于 `backend/src/main/java/com/careerpath/`，违反 `00` 号强约束与 `14` 号第 1 节。
2026-09-21 在真实数据库上完成端到端联调后，用真实 JD 文本复现并逐项修复：

1. `jd/RuleBasedJdParser.java` 的 `inferLevel` 兜底返回 `3` —— 改为返回 `null`，不再猜测；
2. `skill/CapabilityService.java` 中的 `getRequiredLevel() == null ? 3 : ...` —— 改为保留 `null`，取该技能全部要求的最高明确等级；
3. 兜底值 `3` 与“熟悉”同值，下游无法区分“JD 未提等级”与“JD 要求熟悉” —— 现由 `null` 表达未知，前端展示为“未知”；
4. `inferLevel` 的 24 字符上下文窗口在相邻技能间可能串扰 —— 改为按分句（逗号 / 顿号 / 分号 / 句号 / 换行）就近归属等级词，技能名内部的等级词（如“Java 基础与集合”的“基础”）不参与判断；
5. `skill/dto/CapabilityMap.java` 的 `NextAction` 缺 `14` 号第 5 节示例中的 `taskId` —— 已补字段，学习任务模块落地前为 `null`。

联调中另复现并修复 3 项：

6. `BONUS` 永不产生：`PREFERRED` 正则吞掉了“加分”，契约中 4 个 `RequirementType` 实际只产出 3 个 —— 已拆分 BONUS（加分 / nice to have）与 PREFERRED（优先 / 更佳）；
7. 重复提取：`Spring` 与 `Spring Boot` 从同一段文字各产出一条要求 —— 改为同一段文字只保留最长关键词命中；
8. 漏抽：JD 明写“精通 Java”与“Kubernetes”却无对应要求 —— 前者因 `V2__seed_skills.sql` 的 `java-basics` 别名缺裸 “Java”，后者因技能库无该词条，已由新增的 `V3__skill_alias_and_seed_update.sql` 补足（Flyway 禁止就地修改已应用的 V2）。

### C. Worker 已修复的安全问题（含回归测试）

Python 标准库 `ipaddress.is_private` 不覆盖 `100.64.0.0/10`（RFC 6598 运营商级 NAT）等网段，构成 SSRF 绕过点。已在 `worker/src/careerpath_worker/crawler/url_guard.py` 增加 `EXTRA_BLOCKED_NETWORKS` 并补回归测试。

### D. 当前阻塞

- **数据库**：已解除。5432 实例（服务 `postgresql-x64-17`）原本既无 `career_path` 角色也无 `career_path` 库，报出的 `28P01` 实为“角色不存在”的固定返回而非密码错误；已用单用户模式创建角色与库，Flyway 迁移至 v3。**遗留运维事项**：该实例当前由 WMI 脱离进程树临时启动，长期稳定需以管理员身份 `net start postgresql-x64-17`；切换前必须先 `pg_ctl stop -D D:\PostgreSQL\data\data -m fast`（两者共用同一数据目录）；
- **端到端闭环**：已解除。2026-09-21 用真实数据跑通并留痕，见第 10 节追加记录；
- **代码托管决策变更**：2026-09-16 按队伍指令移除 CodeArts 远端，`origin` 改指 GitHub。此举与 `11` 号文档第 7 行及 `README.md` 第 98、99、107 行的“CodeArts 覆盖全流程并留痕”要求冲突，赛事提交规则确认前不作最终结论，见第 7 节 B。当前 `github.com:443` 不可达，8 个提交尚未推送；
- **剩余契约缺口**：岗位发现、测评与面试、学习计划、简历建议四个页面**后端尚无对应接口**，其演示数据真实化必须先新建契约，见第 6 节第 8 项。

### E. 导航入口可用性与能力图谱对比视图（2026-09-21 用户确认的变更）

**已核实的缺陷**：`frontend/src/App.vue:11` 的 `const goalId = computed(() => String(route.params.goalId ?? ''))`
表明「当前求职目标」只存在于路由参数中，不是全局状态。离开目标建立流程（如进入岗位发现）后 `goalId` 为空，
一级导航「能力与证据」即被渲染为不可用（`to: null` + `disabledReason: '需先创建目标'`），
导致已创建过目标的用户仍进不去，且提示文案与事实不符。
此外 `capability-map` 同时挂在一级导航（`App.vue:31-37`）与目标建立流程条（`App.vue:75-79`）两个层级上，
访问该页时两处同时高亮——这是用户反馈“不适配”的直接来源。

**变更理由**：用户于 2026-09-21 提出“不选中一个职业计划就点不进去很反人类反直觉”，
并要求能力图谱页提供跨能力的横向对比视图。经文档复核，`19` 号 §3.2 第 79 行本已要求
“全局目标切换器置于导航顶部或页面标题区”，该能力从未实现，是上述缺陷的根因。

**用户确认的方案（2026-09-21）**：

1. **能力图谱保留在一级导航**；用户先前提议的“删除该栏并内嵌到岗位发现”已否决——
   `03` 号 §1 规定岗位发现的输入即目标 JD，能力图谱是其上游，内嵌会造成循环依赖；
2. 补齐三层：把「当前目标」提升为全局状态 → 实现 `19` 号 §3.2 要求的全局目标切换器 →
   一级导航项永远可点（有当前目标则直达图谱，确无目标则引导创建，不再渲染为禁用态）；
3. **能力图谱页新增分项对比条形图**：成对展示「岗位要求等级 vs 用户当前等级」、按差距排序、
   条形须同时标注置信度；不得做成单一“能力总分”或整体匹配度百分比（`04` 号 §10、`19` 号 §9）；
4. **不使用雷达图**：用户此前设想的多边形/雷达对比图已被明确否决
   （`04` 号 §1「不是单一雷达分数」、`19` 号 §9「默认不使用雷达图」）。

**影响范围**：一级导航的可用性行为、`App.vue` 外壳与全局目标切换器、`CapabilityMapView` 的汇总视图。
不涉及 API、契约字段、评分规则或用户事实。

**迁移方式**：新增的全局目标状态须为目标内页面（`CapabilityMapView`、`JdImportView` 等）提供当前 `goalId` 来源；
本轮改动的留痕以本节与第 10 节为准（`19` 号 §17 要求的一级导航变更留痕；CodeArts 工作项未建立时本文件即临时留痕载体）。

**实现状态（2026-09-21，已完成并通过验收）**：

- 新增 `frontend/src/composables/useCurrentGoal.ts`：模块级 `reactive` 单例（项目未使用 Pinia，未新增依赖）；
  `localStorage` 只记住目标 id，不缓存目标名称；目标列表以 `GET /api/v1/goals` 为唯一权威，
  本地记住的 id 若不在列表中即清空；读取失败时保留本地选择但 `synced` 保持 false，界面必须显示「未同步」。
- 新增 `frontend/src/components/GoalSwitcher.vue`：顶栏与移动端抽屉复用同一组件，只列 `ACTIVE` 目标；
  在目标流程页切换时同步改写地址栏中的 `goalId`，避免顶栏与 URL 互相矛盾。
- `App.vue`：`goalId` 改为「路由参数优先，否则回落到全局当前目标」；一级导航 6 项全部可点，
  「能力与证据」在无当前目标时指向 `/goals/new?intent=capability`；目标建立流程条收回为
  `goal-create` / `jd-import` 两步，消除与一级导航同时高亮同一步骤的层级混淆。
- `GoalCreateView.vue`：支持 `intent=capability` 的引导说明卡与「创建并查看能力图谱」提交文案；
  「已有目标」列表与顶栏切换器共用同一份全局状态，不再各自维护数据源。
- `CapabilityMapView.vue`：新增「分项差距总览」，复用既有 `LevelCompare` + `ConfidenceBadge`，
  按差距处置优先级取前 8 项，不折算单一总分或匹配度百分比，未新增图表组件。
- 验收结果：`npm run typecheck`、`npm run build` 通过；1440×900 / 1024×768 / 390×844 / 200% 缩放四档实测，
  6 项导航无禁用态、切换器在 390px 下正确收进抽屉且抽屉内可用、页面无横向溢出；
  Tab 顺序包含切换器、抽屉焦点循环与 Esc 焦点归还均正常；关键文本对比度 4.58～17.72:1，全部达 WCAG AA。
- 本轮未改动：§5 D 的外部阻塞事项、四个一级页面的演示数据、API 与契约字段。

## 6. 立即可执行任务

云码道不需要等待所有外部问题回答后才工作，按以下顺序推进：

1. 收尾 CP-002：完成 [14-contracts-and-schemas.md](./14-contracts-and-schemas.md) 到 OpenAPI / JSON Schema 的导出，写入 `docs/contracts`；
2. ~~解除数据库阻塞，在真实库上跑通 `scripts/db-migrate` 与 `scripts/db-seed`~~ —— 已完成（2026-09-21，Flyway v3，10 张业务表）；
3. ~~端到端验证核心闭环：目标 → JD 导入 → 解析 → 能力图谱 → 前端呈现~~ —— 已完成（2026-09-21）；
4. ~~修复第 5 节 B 的 5 项契约缺陷，尤其是“未知猜成已知”的等级兜底~~ —— 已完成（2026-09-21，实际修复 8 项）；
5. ~~开专门任务重构 `frontend/`，使其符合 19 号设计系统~~ —— 已完成（含 Beautiful UI 本地适配与视觉验收矩阵）；
6. 用本地 PostgreSQL/Redis 或 Docker 替代服务开发，不把临时凭据提交；
7. ~~落地第 5 节 E 已确认的导航与图谱改造：全局「当前目标」状态 + 全局目标切换器 + 一级导航项永远可点，并给能力图谱页补分项对比条形图（要求 vs 现状、按差距排序、标注置信度）~~ —— 已完成（2026-09-21，见第 5 节 E 的实现状态；改动尚未提交）；
8. 新建 CP-003 起的最小契约，使四个一级页面（岗位发现 / 测评与面试 / 学习计划 / 简历建议）能接真实接口，随后替换页内演示数据；
9. 华为云账号和配额确认后，再配置 RDS、DCS、OBS、MaaS 和 ECS；
10. CP-003、CP-004、CP-005 可在 CP-002 契约冻结后并行。

## 7. 必须由队伍确认的外部事项

这些事项需要队伍或赛事方给出事实，云码道不得猜测：

### A. 报名资格

核对本届创新赛是否要求“三名学生 + 一名指导教师”，以及四名学生队伍是否允许报名。记录官方页面、咨询时间、答复人和结论。

### B. CodeArts 使用证据（含 2026-09-16 的托管决策变更）

确认必须在哪些阶段使用编程智能体，以及提交什么形式的记录。至少准备需求分析、代码生成、调试、测试和部署的可审查证据，但不要伪造已发生记录。

2026-09-16 队伍决定：不再向 CodeArts 代码托管推送，本地 `origin` 改指 GitHub，CodeArts 远端已删除。**该决定与 `11` 号文档第 7 行和 `README.md` 第 98、99、107 行的要求直接冲突**，属于待确认事项：

- 若赛事方确认“必须托管在 CodeArts 并留痕”，需要重新添加 CodeArts 远端并补推历史提交；
- 若确认 CodeArts 仅为可选工具，则同步修订 `11` 号文档与 `README.md` 的相关表述，避免文档与实际操作长期不一致；
- 在确认之前，本文件只登记事实与冲突，不修改 `11` 号文档。

### C. 华为云资源

确认可用区域、RDS/DCS/OBS/MaaS/ECS 或云容器配额、访问方式、预算和备案要求。未确认前使用接口 Mock 或本地兼容服务。

### D. 搜索与采集范围

确认允许采集的企业官方域名、robots/条款、搜索服务配额和访问频率。未审核的站点不得加入生产采集清单。

### E. 数据授权

确认简历、面试回答和用户实验数据的授权、脱敏、保存期限和公开范围。没有授权就使用虚构数据。

## 8. 云码道下一次启动时应输出

云码道读取本文件后，必须先报告：

- 当前任务编号及其依赖；
- CP-001 为 `PARTIAL`、CP-002 为 `PARTIAL`、CP-003 未开始，而不是未初始化；
- 应用代码已有 16 个本地提交，`main` 与 `origin/main` 已一致（无待推送）；`frontend/` 已符合 `19` 号设计系统并通过视觉验收矩阵；
- 代码托管为 GitHub 单一远端，CodeArts 远端已移除，该决策与 `11` 号文档冲突且待确认；
- 本次准备创建或修改的文件；
- 使用本地 Mock 还是已确认云服务；
- 测试和人工验收方式；
- 数据库与端到端闭环已于 2026-09-21 实测跑通（`career_path` 库与角色已建立、Flyway v3）；仍须转述第 5 节 D 的托管冲突与 PostgreSQL 运维事项；
- 若依赖外部确认，明确指出但不要因此停止无关工作。

## 9. 状态更新格式

每次更新只修改事实，不写预测性完成：

```text
日期：YYYY-MM-DD
任务：CP-XXX
已完成：可由提交、日志或测试证明的内容
未完成：仍缺少的交付
证据：提交、CodeArts工作项、测试或截图位置
阻塞：具体外部依赖；无则写“无”
下一步：一个明确任务编号
```

## 10. 本次变更记录

```text
日期：2026-09-16
任务：CP-001（本地骨架收尾）/ CP-002（契约骨架与契约测试）
已完成：
  - .env.example；6 个运行脚本（dev-up / dev-down / db-migrate / db-seed / test / smoke）
  - deploy/docker-compose.yml（PostgreSQL 16 + Redis 7）；database/seed/seed_demo.sql（虚构数据）
  - docs/ 与 docs/contracts/ 目录；Maven Wrapper（backend/mvnw）
  - 后端 36 个 Java 源文件可编译；契约测试 79 项通过
  - 前端 Vue 3 + TS 骨架可构建（46 模块）
  - Worker Python 骨架可启动；79 项测试通过；/health 与 /health/ready 实测响应
未完成：
  - OpenAPI / JSON Schema 导出（docs/contracts 仍为空）
  - 真实数据库上的迁移与种子验证、Redis 契约
  - 端到端闭环验证（目标 → JD 导入 → 解析 → 能力图谱）
  - 前端不符合 19 号设计系统，需专门重构
  - CodeArts 云端项目与工作项、CI 流水线、云资源
证据：
  - backend/target/surefire-reports/*.txt（79 项，Failures/Errors/Skipped 均为 0）
  - backend/target/site/jacoco/jacoco.csv（行覆盖 177/177）
  - frontend/dist/（本地构建产物，不属于提交内容）
  - worker/worker.log、frontend/vite-dev.log（本地运行日志）
  - 无提交、无 CodeArts 工作项：全部应用代码仍是工作区未跟踪文件
阻塞：
  - 本地 PostgreSQL 17.5/18 占用 5432 需凭据；Docker 守护进程未启动
  - 赛事方对“是否必须托管在 CodeArts”的确认未完成
下一步：CP-002
```

### 追加记录（2026-09-16 代码托管切换）

```text
日期：2026-09-16
任务：无（远端配置与文档调整）
已完成：
  - 移除 CodeArts 远端（codehub.devcloud.cn-north-4.huaweicloud.com/.../JDdriven-Jobprep.git）
  - 将 GitHub 远端由 github 重命名为 origin；main 跟踪 origin/main
  - 修正上一记录中的错误事实：ae5c500 早已推送到 GitHub，并非“未推送”
未完成：
  - 赛事方对“是否必须托管在 CodeArts 并留痕”的确认
证据：
  - git remote -v：仅剩 origin → https://github.com/pppeach111/JDdriven-Jobprep.git
  - git branch -vv：main 跟踪 origin/main，两端同为 ae5c500
阻塞：
  - 11 号文档与 README 的 CodeArts 留痕要求与本决策冲突，待赛事方确认
下一步：CP-002
```
### 追加记录（2026-09-16 UI 设计系统修订，CP-010 前置）

```text
日期：2026-09-16
任务：CP-010 前端工作台（前置：设计系统修订）
已完成：
  - frontend/prototype.html：静态风格预览稿 v2（顶部横向导航 + 上排三列 / 下排两列网格）
    经用户确认定稿，作为 CP-010 重构的视觉基线
  - shared_docs/19-ui-design-system.md：修订 7 项条款并新增修订台账
    §1 视觉参数（视觉变化度 3→5；风格由“墨绿 / 少阴影 / 无渐变”改为“靛蓝紫 / 常态柔和阴影 / 玻璃拟态”）
    §2.3 品牌命名（展示层用工作名“职径”，令牌与组件名用语义名）
    §3.2 / §3.3 桌面外壳由 240px 左侧栏改为顶部横向导航（70px sticky，1024 / 1359 两档）
    §4.1 颜色令牌表全量替换为靛蓝紫体系，并明确三层令牌架构（primitive / semantic / component）
    §4.2 字体由 Inter 改为自托管 Plus Jakarta Sans（400/600/700/800，不引 Google Fonts CDN）
    §4.3 圆角分级（12 / 14 / 24 / 28 / pill）与 5 个阴影令牌及可核查的 alpha/模糊半径边界
    §4.4 表格行高 48 → 48～50
    §4.5（新增）玻璃拟态使用规范：禁嵌套、参数、性能、对比度、@supports 降级
    §9 明确环形进度允许边界（只表达可核算覆盖率，必须配数字与文本，不得表达匹配度或能力总分）
    §12 增加玻璃拟态与首屏关键渲染路径约束
    §13 图标家族固定为 Lucide，要求经 better-icons 取真实路径
    §16 验收清单：拆分“渐变”与“荧光光晕 / 高饱和彩色阴影 / 玻璃嵌套”检查项，新增玻璃层对比度项
    §18（新增）修订台账
  - 用户裁决：玻璃拟态全量保留（接受首屏渲染成本），后续以 will-change / 合成层优化
  - 预览稿修正两项编码前偏离：重复主按钮（§2.2 / §6.5）；首屏卡片顺序（§7.2，差距须先于覆盖）
未完成：
  - frontend/src 尚未按新规范重构（theme.css 仍是深色令牌，主色仍是第三套值 #6366F1）
  - 共享基础组件（App 外壳、卡片、状态标记、等级与置信度）尚未重建
  - 视觉验收矩阵四档视口未执行（1440×900 / 1024×768 / 390×844 / 200% 缩放）
  - Plus Jakarta Sans 字体文件尚未自托管引入
证据：
  - frontend/prototype.html（930 行，静态预览稿；已通过标签配对检查：section 8/8、div 118/118）
  - shared_docs/19-ui-design-system.md §18 修订台账（第 501 行）
  - 预览稿内保留状态诚实展示区与修订结果清单，可对照 §2.4 / §8 验收
阻塞：
  - 无（本次不涉及数据库）
下一步：CP-010
```
### 追加记录（2026-09-16 前端按新规范重构，CP-010 第一步）

```text
日期：2026-09-16
任务：CP-010 前端工作台（设计系统落地）
已完成：
  - frontend/src/styles/theme.css：全量重写为三层令牌（primitive / semantic / component）
    改为浅色靛蓝紫（#5B4BE8），玻璃拟态参数 blur(18px) saturate(170%)，5 个 --shadow-* 令牌，
    圆角分级 12 / 14 / 24 / 28 / pill；移除 shimmer 循环动画（§12）；焦点环改 --color-focus + outline-offset 2px
  - frontend/src/App.vue：外壳重写为顶部横向导航（品牌 → 6 项一级导航 → 搜索 / 通知 / 账户 → “演示数据”标记）
    backdrop-filter 由 @supports 包裹并仅对 sticky 层声明 will-change；未实现入口显式标注“尚未开放”，不伪造可用性
    保留“目标建立”子流程条（flowbar），改用 --color-surface-sunken 以避免第二层大面积模糊（§4.5 禁嵌套玻璃）
  - 令牌重命名 74 处（6 个文件）：将代码自造的 --accent / --text-* / --signal-* / --radius-*
    等一律归一到 19 号 §4.1 的 --color-* 语义名（此前代码从未使用文档定义的名称，属既有的未登记不符合项）
  - 组件内颜色字面量清零：rgba(255,255,255,*) / #fca5a5 / #fcd34d 等深色主题残留改为语义令牌
  - 新增 frontend/src/components/icons.ts（22 个 Lucide 真实路径常量 + IconName 类型）
    与 frontend/src/components/AppIcon.vue（通用图标组件，size / strokeWidth 可配，aria-hidden）
  - 自托管 Plus Jakarta Sans：通过 npm pack 提取 woff2（未新增依赖、未改 package.json）
    落地 frontend/public/fonts/ 共 4 个 latin 子集字重（400/600/700/800，合计 48KB）+ OFL 许可证文本
    theme.css 顶部新增 4 条 @font-face（font-display: swap，带 unicode-range 限定拉丁区），不引 Google Fonts CDN
  - 视觉验收矩阵四档视口已执行，实测无水平溢出、字体状态 loaded（结果表见 §5.A）
  - 移动端导航抽屉：汉堡入口（menu-btn）+ 右侧抽屉，含 Esc / 遮罩关闭、滚动锁、
    焦点移入与抽屉内循环、关闭归还焦点、桌面档自动关闭；新增 --color-scrim 遮罩令牌并登记入 19 号 §4.1
  - frontend/index.html 的 title / description 由“职径 CareerPath”统一为“职径 Jobprep”，与品牌区副标题一致
  - 验收截图归档至 frontend/screenshots/（5 张：1440×900 / 1024×768 / 390×844 / 200% 缩放 + 390×844 抽屉打开态）
未完成：
  - 内容区断点 900px 未见于 19 号文档，与外壳断点体系不统一（768～900px 错位）
证据：
  - npm run build（vue-tsc -b && vite build）通过，50 modules transformed；dist/fonts/ 含 4 个 woff2
  - grep 组件内颜色字面量（rgba / #hex）返回空
  - Vite dev server（http://localhost:5173）4 个字体 URL 均 HTTP 200（font/woff2，11816 / 12188 / 12244 / 11896 字节）
  - 四档截图与 DOM 探针实测数据（overflowX=0；layoutCols / gridCols / asideRect 见 §5.A）
  - 抽屉交互经 Edge 153 + CDP 实测：初始 drawerOpen=false；点击汉堡后焦点移入抽屉（activeInDrawer=true）；
    CDP 真实按键 Tab ×8 与 Shift+Tab 全程 activeInDrawer=true，未逃逸到背景；真实 Esc 关闭且焦点归还 .menu-btn；
    切到 1280 桌面档后 .menu-btn 隐藏且抽屉自动关闭
  - 备份位置（用于回退与核对）：/tmp/ict-token-backup、/tmp/ict-src-backup2
阻塞：
  - 无（本次不涉及数据库）
下一步：提交 git → CP-003 起的业务任务
```

> 注：`frontend/` 目录整体与 `shared_docs/19-ui-design-system.md` 目前均为 git 未跟踪状态，本轮改动尚未提交。

### 追加记录（2026-09-21 Beautiful UI 本地适配）

```text
日期：2026-09-21
任务：CP-010 前端工作台（Beautiful UI 适配）
已完成：
  - 依据 beautifului.dev 的 Loading State、Context Cards、Task Rows、Status 和 compact Button 语言，新增 Vue 本地适配组件
  - GoalCreateView 接入 BeautifulButton、BeautifulStatus、BeautifulTaskRow，保留创建目标与已有目标 API 行为
  - JdImportView 接入真实阶段 LoadingState、ContextCard、Status，保留原文引用、未知字段、警告与错误 traceId
  - CapabilityMapView 接入 LoadingState、Status、ContextCard 与紧凑筛选，保留等级、置信度、差距和下一动作语义
  - App.vue 外壳改为中性画布、细边界、紧凑导航与状态标记；不直接引入 React/Tailwind registry
  - theme.css 增加 Beautiful UI Vue adaptation 令牌与共享样式，保留既有语义令牌以降低迁移风险
  - 19-ui-design-system.md 增加 Beautiful UI 接入流程、组件映射和不透明表面规范
未完成：
  - 尚未完成真实后端联调下的截图回归；需要在 API 可用时检查正常、空、局部失败和长文本状态
证据：
  - frontend/src/components/Beautiful*.vue
  - frontend/src/styles/theme.css
  - frontend/src/views/GoalCreateView.vue、JdImportView.vue、CapabilityMapView.vue
  - npm run typecheck 与 npm run build 均通过（60 modules transformed）
阻塞：
  - 无（本次不涉及数据库与契约）
下一步：CP-010 视觉验收矩阵与真实 API 联调
```

### 追加记录（2026-09-21 Beautiful UI 优先复用规则）

```text
日期：2026-09-21
任务：CP-010 前端工作台（Beautiful UI 复用约束固化）
变更理由：
  - 后续由 Codex 或 CodeArts 设计页面、组件和布局时，需要稳定优先复用已完成的 Beautiful UI Vue 本地适配，避免重复建立平行视觉组件
已完成：
  - 新增根 AGENTS.md，要求所有前端任务先检查 19 号设计系统与 Beautiful*.vue 组件
  - careerpath-ui 技能增加 Beautiful UI 自动触发描述、组件决策顺序和交付说明
  - 19-ui-design-system.md §4.6 固化“直接复用 → 组合复用 → 扩展共享组件 → 新增共享适配”的默认顺序
  - 重新安装并启用本地 CodeArts 技能，careerpath-ui=true
影响范围：
  - 后续新增或修改的前端页面、共享组件和布局
  - 不修改现有 API、路由、业务文案或运行时依赖
迁移方式：
  - 现有页面无需迁移；新任务直接引用 frontend/src/components/Beautiful*.vue
  - 现有组件无法覆盖时，先扩展共享组件；确需新增时建立新的 Beautiful*.vue 并更新组件映射
验证：
  - careerpath-ui 技能通过 quick_validate.py 校验
  - tools/codearts-skills 与 .codeartsdoer/skills 中的 careerpath-ui SKILL.md SHA-256 一致
```
### 追加记录（2026-09-21 一级页面初版界面与产品名重做）

```text
日期：2026-09-21
任务：CP-010 前端工作台（一级页面初版界面 + 产品名重做）
变更理由：
  - 四个一级入口（岗位发现、测评与面试、学习计划、简历建议）此前只有占位导航，无法评审信息结构
  - 用户反馈「求职目标」页的大字报横幅式标题不像产品界面，要求重做产品名与图标并自然融入页面
  - 用户要求优先复用本地 Beautiful UI 适配组件
已完成：
  - 新增共享组件 BrandMark（自绘几何产品标记，不新增图标库依赖）、BeautifulPageHeader（§5.1 标题区结构）、BeautifulEmptyState（§8 三段式空状态）
  - 扩展共享组件：BeautifulTaskRow 增加 expanded/muted 与默认插槽；BeautifulButton 增加 to 属性，吸收页面内的 RouterLink 平行实现
  - GoalCreateView 去掉大字报 hero，改为标题区 + 720px 单列表单；已有目标列表下移到表单之后，消除原 900px 内容区断点
  - CapabilityMapView 改为可排序能力矩阵（7 列、表头 aria-sort、未知值恒排末尾、行选中联动详情区）
  - 新建 JobDiscoveryView、AssessmentView、LearningPlanView、ResumeAdviceView
  - 路由新增 /discovery、/assessment、/learning、/resume；App.vue 一级导航 6 项全部接入真实路由，目标建立流程条只在目标建立流程显示
影响范围：
  - 一级导航、路由、顶栏与页面外壳；不修改现有 API、契约字段、评分规则或用户事实
  - 四个新页面尚无后端接口，页内使用固定演示数据并在顶部保留「演示数据」标记（19 号 §14 允许的例外）
迁移方式：
  - 后端接口就绪后，把各页面演示数据替换为 client.ts 的真实调用，保留现有加载/空/失败/过期分支
  - 未新增第三方前端依赖，未引入 React/Tailwind 运行时
验证：
  - npm run build 通过（vue-tsc -b && vite build）
  - 四档视口（1440x900 / 1024x768 / 390x844 / 200% 缩放）共 28 个页面组合的 CDP 实测：横向溢出 0、对比度低于 AA 0、图标按钮小于 36px 0、点击目标小于 24px 0、文本竖排 0
  - 断点命中正确（≥1024 显示一级导航；<1024 显示抽屉按钮且一级导航收起）；流程条只在目标建立相关路由出现
  - 移动端抽屉 role=dialog / aria-modal / 焦点锁 / 背景滚动锁均通过，Esc 关闭后焦点归还触发按钮
  - 能力矩阵成功态（CDP 拦截接口注入契约合法数据）验证排序 none→descending→ascending、未知值排末尾、行选中联动详情
修复的验收缺陷：
  - 顶栏高度与控件尺寸回归 §3.2 / §3.3 / §4.4（曾被外壳紧凑化压到 64px / 60px / 34px）
  - 一级导航补齐 aria-current="page"；不可用项原因文案区分「尚未开放」与「需先创建目标」
  - 能力矩阵在 <768px 转为分组列表（标签 + 值），不再横向滚动；修正 caption 因 table-caption 显示类型被压成逐字竖排
  - 表头排序按钮补足 28px 最小点击高度（WCAG 2.2 AA 要求 24px）
未完成：
  - JdImportView 仍为旧版标题区样式，待统一到 BeautifulPageHeader
  - 四个新页面的演示数据待后端接口就绪后替换为真实调用
阻塞：
  - 无（本次不涉及数据库与契约）
下一步：统一 JdImportView 标题区；后端就绪后替换演示数据并做真实 API 回归
```
### 追加记录（2026-09-21 真实库端到端联调与契约缺陷修复）

```text
日期：2026-09-21
任务：CP-002（契约缺陷修复） / 核心闭环端到端验证 / CP-010 收尾
已完成：
  - 数据库：5432 实例原本既无 career_path 角色也无该库，已用单用户模式创建角色与库，
    Flyway 由 v1 迁移至 v3，建成 10 张业务表（此前“缺少凭据”的判断实为“角色不存在”误报）
  - 端到端闭环实测：POST /api/v1/goals → POST /goals/{id}/jd → POST /goals/{id}/analyze → GET /goals/{id}/capability-map
    goalId=2e0a76ae-af11-4684-8639-390f1c1ad6f5，sourceId=b48d95d3-b6b3-471b-893b-b55e64044e7b
    解析出 10 条要求（2 HARD_GATE / 7 CORE / 1 PREFERRED）与 8 项能力，前端经 Edge headless 截图确认渲染真实数据
  - 契约缺陷修复 8 项（明细见第 5 节 B）：
    等级兜底 3 → null；按分句就近归属等级词并排除技能名内部的等级词；BONUS 与 PREFERRED 拆分；
    同段文字最长关键词去重；种子补裸 "Java" 与新增 Kubernetes 词条（V3 迁移）；NextAction 补 taskId
  - 前端：顶栏「演示数据」标记由全局常驻改为按路由收敛（只在一级导航的四个演示页面显示）；
    JdImportView 标题区统一到 BeautifulPageHeader，移除大字报 hero 与页面局部的「返回上一步」链接，
    内容区断点 980px → 1023px（并入外壳断点体系）
  - 契约文档：14 号补充“requiredLevel 未知时为 null”“技能等级归属规则”“nextAction.taskId 语义”
未完成：
  - OpenAPI / JSON Schema 导出（docs/contracts 仍为空）
  - 四个一级页面的真实数据替换（后端尚无对应接口，需先新建契约）
  - 8 个提交尚未推送到 GitHub（github.com:443 不可达）
证据：
  - backend/src/test/java/com/careerpath/jd/RuleBasedJdParserTest.java：38 项通过；后端全量 84 项通过
  - backend/src/main/resources/db/migration/V3__skill_alias_and_seed_update.sql
  - backend/.tmp-backend.log：Flyway「Current version of schema "public": 2」→
    「Migrating schema "public" to version "3 - skill alias and seed update"」→「Successfully applied 1 migration」
  - 能力图谱页截图实测：微服务架构行「目标等级」显示“未知”；Spring Boot 由误判 L4 修正为 L3；
    Java 基础与集合（L4）与 Kubernetes 与容器编排（L2）新进入图谱
  - npm run typecheck 通过（vue-tsc -b --force）
阻塞：
  - 赛事方对“是否必须托管在 CodeArts 并留痕”的确认未完成
  - 本地 PostgreSQL 当前为 WMI 临时实例，长期稳定需以管理员身份启动 Windows 服务
下一步：OpenAPI 导出（CP-002 收尾），随后为四个一级页面新建最小契约
```
### 追加记录（2026-09-21 导航入口与能力图谱对比视图决策）

```text
日期：2026-09-21
任务：无（产品架构讨论收敛，产出设计决策）
变更理由：
  - 用户就导航信息架构与产品定位提出一组构想并要求批判性讨论，核心分歧是
    “能力图谱是否该单列一级导航”以及“爬虫式岗位发现”与“事后规划”的路线之争
  - 经复核 01/03/04/07/19 号文档，“抓取”本就是“以目标 JD 为入口”流水线的下游
    （03 号 §2：目标 JD 解析 → 岗位画像 → 生成搜索查询 → 抓取），两条路线并非互斥；
    真正与文档定位冲突的是“无 JD、凭裸需求全域搜索”，且 01 号 §5.3 已把
    “招聘平台账号数据抓取”列为 P2 延后
已完成：
  - 核实导航缺陷：App.vue:11 的 goalId 仅取自 route.params，离开目标流程即为空，
    导致一级导航「能力与证据」对已建目标的用户仍显示不可用；capability-map 同时挂在
    一级导航与目标建立流程条两个层级，访问时两处同时高亮
  - 确认根因：19 号 §3.2 第 79 行要求“全局目标切换器置于导航顶部或页面标题区”，
    该能力从未实现，故「当前目标」缺少全局状态载体
  - 用户裁决 4 项：① 能力图谱保留一级导航（不内嵌岗位发现）；② 补齐全局目标状态 +
    全局目标切换器 + 导航项永远可点；③ 图谱页新增分项对比条形图（要求 vs 现状、
    按差距排序、标注置信度，不做单一总分）；④ 不使用雷达图
  - 被否决的提案：删除能力图谱导航栏（会造成 03 号 §1 的循环依赖）、雷达图（04 号 §1 / 19 号 §9）
影响范围：
  - 一级导航的可用性行为、App.vue 外壳与全局目标切换器、CapabilityMapView 汇总视图
  - 不涉及 API、契约字段、评分规则或用户事实
迁移方式：
  - 全局目标状态须为目标内页面统一提供当前 goalId 来源；导航项由禁用态改为可用态
  - 19 号 §17 要求的一级导航变更留痕落在本文第 5 节 E 与本记录
未完成：
  - 上述改造尚未实现（本轮只做决策留痕，未改动任何代码）
  - 四个一级页面接真实接口仍待新建 CP-003 起的最小契约
证据：
  - 本文第 5 节 E（决策明细）
  - shared_docs/19-ui-design-system.md §3.2 第 79 行（全局目标切换器的既有要求）
  - shared_docs/03-official-job-discovery.md §1（岗位发现以目标 JD 为输入）
阻塞：
  - 无
下一步：第 6 节第 7 项（导航与图谱改造）
```
### 追加记录（2026-09-21 导航三层补齐与能力图谱分项总览实现）

```text
日期：2026-09-21
任务：第 6 节第 7 项（落地第 5 节 E 已确认的导航与图谱改造）
变更理由：
  - 第 5 节 E 此前只做决策留痕、代码未动；本次按 4 项裁决实现，目的是让「能力与证据」
    不再对已建目标的用户显示不可用，并给图谱页补上分项对比视图
已完成：
  - 全局「当前目标」状态：新增 composables/useCurrentGoal.ts，模块级 reactive 单例，
    不新增依赖（项目未使用 Pinia）；localStorage 只记住目标 id、不缓存目标名；
    列表以 GET /api/v1/goals 为权威，本地记住的 id 不在列表中即清空；
    读取失败时保留本地选择但 synced=false，切换器显示「未同步」而不编造目标名
  - 全局目标切换器：新增 components/GoalSwitcher.vue，按 19 号 §3.2 置于顶栏
    （一级导航之后、搜索之前）；<768px 随一级导航一并收进抽屉并复用同一组件；
    在目标流程页切换时同步改写地址栏 goalId，避免顶栏与 URL 互相矛盾
  - 一级导航永远可点：App.vue 的 goalId 改为「路由参数优先，否则回落到全局当前目标」；
    6 项全部渲染为 RouterLink，删除 disabledReason 与 navlink--disabled / drawer__link--disabled；
    无当前目标时「能力与证据」指向 /goals/new?intent=capability，且该项保持选中态
  - GoalCreateView 支持 intent=capability：顶部引导卡 + 提交按钮文案「创建并查看能力图谱」，
    创建成功后直接进入该目标的能力图谱；「已有目标」列表改用同一份全局状态，消除双数据源
  - CapabilityMapView 新增「分项差距总览」：复用既有 LevelCompare + ConfidenceBadge，
    按差距处置优先级取前 8 项，明确不折算单一总分或匹配度百分比（19 号 §9 边界）
  - 目标建立流程条收回为 goal-create / jd-import 两步，修复 capability-map 在一级导航
    与子流程条两处同时高亮的层级混淆
  - 19 号文档同步：§3.2 顶栏结构行补入切换器并写明收缩与抽屉复用规则；
    §4.6 新增 GoalSwitcher / useCurrentGoal 归属与两条约束；§18 台账新增两行
影响范围：
  - frontend/src/App.vue、composables/useCurrentGoal.ts（新增）、
    components/GoalSwitcher.vue（新增）、views/GoalCreateView.vue、views/CapabilityMapView.vue
  - shared_docs/19-ui-design-system.md §3.2 / §4.6 / §18
  - 不涉及 API、契约字段、评分规则或用户事实
验收证据：
  - npm run typecheck 通过；npm run build 通过（验收时用 vite preview 指向最新 dist）
  - CDP 探针实测四档视口：1440×900 / 1024×768 / 390×844 / 200% 缩放（等效 720px CSS 视口）
    · 每档 navCount=6、navDisabled=0，页面 scrollWidth 均小于视口宽，无横向溢出
    · 切换器 1440 宽 207px 完整显示目标名；1024 收窄至 160px；390 与 720 下顶栏隐藏，
      抽屉内切换器显示目标名且 6 个链接无禁用项
    · 总览区块 8 行、8 条 level__track、8 个 level__scale，无溢出
  - 交互实测：无记忆时首项为「未选择目标」；非流程页切换目标只改全局状态与导航 href，
    URL 保持 /discovery；流程页切换目标 URL 跟随到新目标；清空选择回到 /goals/new；
    无记忆时点击「能力与证据」落到 /goals/new?intent=capability 且该项高亮
  - 无障碍实测：桌面 Tab 顺序为 品牌 → 6 项导航 → 目标切换器 → 页面动作，无禁用项缺口；
    抽屉内 Tab 循环覆盖切换器与关闭按钮，Shift+Tab 反向正常，Esc 关闭并归还焦点，
    打开期间 body overflow=hidden
  - 对比度实测（WCAG AA 阈值 4.5:1）：navlink 未选中 5.79、选中 5.03、切换器文字 17.72、
    总览说明 5.79、总览能力名 17.72、矩阵能力 id 4.58，全部达标
未完成：
  - 上述改动尚未提交（待按主题分组提交）
  - 已知数据限制：当前后端返回的能力项 estimatedLevel 全部为 null、gapType 全为 EVIDENCE_GAP，
    因此总览目前实际呈现「要求 vs 未知」，尚无真实的估计填充可对比；这是数据现状，
    不得在前端补默认值或改画成 0
  - 新增登记项：19 号 §3.2 / §3.3 的导航断点（文档写 1024～1359px 与 <1024px）
    与实现（1099px、767px）不符，本轮未改动，已登记在 19 号 §18 台账待用户裁决
  - 四个一级页面接真实接口仍待新建 CP-003 起的最小契约
证据：
  - 本文第 5 节 E 的实现状态、第 6 节第 7 项
  - shared_docs/19-ui-design-system.md §3.2（切换器位置与收缩规则）、§4.6（组件归属）、§18（台账）
阻塞：
  - 无
下一步：
  - 第 6 节第 8 项（新建 CP-003 起的最小契约，替换四个一级页面的演示数据）
  - 待用户裁决 19 号 §3.2 / §3.3 的导航断点以文档还是实现为准
```
