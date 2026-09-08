# 求职创新赛项目开发文档

> 暂定项目名：**职径（CareerPath）——证据驱动的求职能力评估与成长系统**

本文档集将当前讨论形成可执行的产品、技术和比赛交付方案。项目的核心不是“把招聘、面试、课程、简历四个功能放在一起”，而是用同一套 **岗位—能力—证据—行动图谱** 串起完整闭环：

```text
目标 JD + 个人简历 + 求职约束
              ↓
        岗位能力与硬门槛解析
              ↓
      个人能力、证据与置信度建模
       ↙          ↓           ↘
 官方岗位发现   靶向能力测评   简历证据检查
       \          ↓           /
          能力差距与优先级
                  ↓
        学习任务、实践与日程
                  ↓
          复测并更新能力图谱
                  ↓
       更新岗位匹配度和简历版本
```

## 云码道读取入口

云码道开始任何开发任务前，必须按以下顺序阅读：

1. [00-codeartsdoer-master.md](./00-codeartsdoer-master.md)：总规则、阶段门和任务执行协议；
2. 本文档：产品边界、统一事实和模块导航；
3. [14-contracts-and-schemas.md](./14-contracts-and-schemas.md)：跨模块数据契约；
4. [15-codeartsdoer-backlog.md](./15-codeartsdoer-backlog.md)：任务编号、依赖和验收；
5. [16-model-prompt-contracts.md](./16-model-prompt-contracts.md)：模型调用边界与提示词契约；
6. 与当前任务编号对应的模块手册；
7. [11-codearts-and-engineering.md](./11-codearts-and-engineering.md)、[13-risk-and-compliance.md](./13-risk-and-compliance.md)：工程和合规约束。

如果任务与本文档冲突，以 `00`、`14`、`16` 中的约束为准；如果仍无法判断，停止修改并提出具体问题。不得通过猜测补齐接口、分数、硬门槛或业务事实。

## 产品原则

1. **只发现岗位，不替用户投递**：仅返回企业官方申请入口，最终投递由用户手动完成。
2. **所有判断必须可解释**：岗位要求、能力评价和简历建议都应定位到来源或用户证据。
3. **不把关键词当能力**：简历自述只是低可信证据，测试、代码、项目和实践任务逐步提高置信度。
4. **不把大模型当评分公式**：大模型负责理解与候选生成，程序负责硬条件、计分、排序和状态流转。
5. **学习必须可验收**：学习路径中的每个任务都有成果、验收标准和复测入口。
6. **禁止虚构经历**：简历模块只能重组、改写有证据支撑的内容。
7. **先做一个岗位族**：首版聚焦 Java 后端实习/校招，验证闭环后再扩展其他岗位。

## 文档导航

| 文档 | 用途 | 主要读者 |
|---|---|---|
| [01-product-definition.md](./01-product-definition.md) | 产品定位、用户流程、范围与验收口径 | 全员 |
| [02-system-architecture.md](./02-system-architecture.md) | 总体架构、数据模型、接口和云服务映射 | 后端、AI、云部署 |
| [03-official-job-discovery.md](./03-official-job-discovery.md) | 官方岗位搜索、解析、去重与有效性核验 | 搜索/采集负责人 |
| [04-capability-graph.md](./04-capability-graph.md) | 能力图谱、证据模型、差距计算与更新规则 | 后端、算法负责人 |
| [05-adaptive-assessment.md](./05-adaptive-assessment.md) | 分层题库、自适应测试、评分与复测 | 测评/AI负责人 |
| [06-targeted-interview.md](./06-targeted-interview.md) | 靶向面试、动态追问和报告生成 | AI、前端负责人 |
| [07-learning-path.md](./07-learning-path.md) | 学习路径、资源核验、实践任务与日程 | 后端、产品负责人 |
| [08-resume-workbench.md](./08-resume-workbench.md) | 简历导入、证据化改写、版本管理与导出 | 前端、后端负责人 |
| [09-data-evaluation.md](./09-data-evaluation.md) | 数据集、离线指标、用户实验和消融实验 | 测评负责人 |
| [10-delivery-plan.md](./10-delivery-plan.md) | 12周排期、四人分工、里程碑与降级策略 | 全员 |
| [11-codearts-and-engineering.md](./11-codearts-and-engineering.md) | 新仓库、Git记录、CodeArts流程和质量门禁 | 全员 |
| [12-demo-and-defense.md](./12-demo-and-defense.md) | 60秒演示、完整答辩、材料清单与追问 | 全员 |
| [13-risk-and-compliance.md](./13-risk-and-compliance.md) | 网页采集合规、隐私、AI可靠性与安全边界 | 全员 |
| [00-codeartsdoer-master.md](./00-codeartsdoer-master.md) | 云码道总入口、开发规则、阶段门和交付格式 | 云码道/全员 |
| [14-contracts-and-schemas.md](./14-contracts-and-schemas.md) | 跨模块实体、枚举、JSON示例和接口契约 | 后端、前端、AI |
| [15-codeartsdoer-backlog.md](./15-codeartsdoer-backlog.md) | 可领取任务、依赖、DoD和模块负责人 | 云码道/全员 |
| [16-model-prompt-contracts.md](./16-model-prompt-contracts.md) | JD/简历/岗位/测评/面试模型契约与防注入规则 | AI、后端 |
| [17-environment-and-runbook.md](./17-environment-and-runbook.md) | 版本基线、环境变量、启动、迁移、部署和烟测 | 全员/云码道 |

## 单一事实来源

以下约定如需修改，应先更新本文档，再同步相关模块：

- 初赛提交目标日期：**2026-11-30**。
- 从 2026-09-07 起按 12 个完整开发周规划，11月30日仅作提交缓冲。
- 首版岗位族：**Java 后端实习/校招**。
- 首版功能：动态网页岗位发现、能力图谱、文本测评、文本面试、学习计划、轻量简历工作台。
- 延后功能：语音面试、实时音视频、自动投递、大而全的行业题库、复杂简历排版编辑器。
- 技术建议：Vue 3 + TypeScript、Spring Boot、Python 采集/AI Worker、RDS、Redis、OBS、MaaS、ECS/容器部署。
- 必须在全新仓库从零开发，保留真实 Git 和 CodeArts 开发记录，不复制 MindSpace 的代码与提交历史。
- 所有开发任务必须引用 backlog 编号，提交、测试和 CodeArts 工作项保持可追溯。
- 任何模块完成前必须通过对应契约测试、最小端到端路径和文档更新，不接受只有页面或只有模型提示词的“完成”。

## 开工前必须确认

- 官方页面所示组队规则可能是“三名学生 + 一名指导教师”，而当前计划是四名学生。必须尽快向赛事方确认四名学生是否均能登记；技术分工可按四人执行，但不要拖到提交前才处理资格问题。
- 确认 CodeArts 编程智能体的必选使用环节和证据形式，开发过程中持续留存需求、任务、代码生成、调试和部署记录。
- 确认使用的搜索服务、模型服务、OCR服务及云资源额度，避免最后两周才发现配额或备案问题。
