# 14 跨模块数据契约与 JSON Schema

## 1. 契约规则

本文件是前端、Spring Boot、Python Worker 和规则引擎之间的单一数据契约。实现时：

- 字段名、枚举和必填性不得自行改写；
- 新增字段必须向后兼容，并同步示例和测试；
- 外部输入缺失时使用 `null` 或 `UNKNOWN`，不使用空字符串冒充未知；
- 所有响应带 `schemaVersion`；
- API 返回的引用必须可定位到原文或证据 ID；
- 分数、等级、置信度由程序校验范围；
- 模型输出先通过 Schema，再进入业务规则。

## 2. 统一类型

### 2.1 状态枚举

```text
TaskStatus: PENDING | RUNNING | SUCCEEDED | PARTIAL | FAILED | CANCELLED
EvidenceDirection: SUPPORTS | WEAKENS | NEUTRAL
EvidenceType: SELF_CLAIM | RESUME_CLAIM | COURSE | CERTIFICATE | PROJECT | CODE | OBJECTIVE_TEST | SCENARIO_TEST | INTERVIEW | MICRO_PRACTICE | PROJECT_RESULT
GapType: QUALIFICATION_BLOCK | KNOWLEDGE_GAP | APPLICATION_GAP | PRACTICE_GAP | EVIDENCE_GAP | EXPRESSION_GAP | CONFLICT
RequirementType: HARD_GATE | CORE | PREFERRED | BONUS
JobOpenStatus: OPEN_CONFIRMED | OPEN_PROBABLE | UNKNOWN | CLOSED | REMOVED
RecommendationAction: APPLY_NOW | APPLY_AFTER_GAP | STRETCH | NOT_RECOMMENDED | VERIFY_FIRST
AssessmentQuestionType: SINGLE_CHOICE | MULTI_CHOICE | TRUE_FALSE | CODE_READING | DIAGNOSIS | SCENARIO | SHORT_ANSWER
SkillLevel: L0 | L1 | L2 | L3 | L4 | L5
```

### 2.2 数值范围

```text
confidence: 0.0 <= x <= 1.0
importance: 0.0 <= x <= 1.0
estimatedLevel: 0.0 <= x <= 5.0
requiredLevel: integer 0..5
score: 0..100，除非具体评分量表另有说明
```

`requiredLevel` 无法从原文确定时必须为 `null`，不得用 `3` 等默认值冒充"熟悉"。
前端统一展示为"未知"，与真实的"熟悉（3）"严格区分。

### 2.3 时间、分页和标识

- 数据库保存时间使用 UTC ISO-8601，例如 `2026-09-08T12:30:00Z`；
- 所有资源拥有不可变 `id`，推荐 UUID；
- 列表接口使用 `page` 从1开始、`pageSize` 取值1～100；
- 变更接口支持 `updatedAt` 或版本号检查，防止覆盖他人更新；
- `traceId` 贯穿一次请求和所有异步子任务。

## 3. API 外层响应

成功响应：

```json
{
  "schemaVersion": "1.0",
  "data": {},
  "traceId": "01JEXAMPLE"
}
```

分页响应：

```json
{
  "schemaVersion": "1.0",
  "data": [],
  "pagination": {"page": 1, "pageSize": 20, "total": 0},
  "traceId": "01JEXAMPLE"
}
```

失败响应：

```json
{
  "schemaVersion": "1.0",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "请求内容不符合要求",
    "details": {},
    "retryable": false
  },
  "traceId": "01JEXAMPLE"
}
```

## 4. 目标 JD 解析结果

```json
{
  "schemaVersion": "1.0",
  "sourceId": "jd_001",
  "jobFamily": {"value": "JAVA_BACKEND", "confidence": 0.94},
  "requirements": [
    {
      "id": "req_001",
      "type": "HARD_GATE",
      "field": "graduationYear",
      "value": {"min": 2027, "max": 2028},
      "importance": 1.0,
      "explicit": true,
      "sourceQuote": "面向2027-2028届本科生",
      "sourceLocator": {"page": 1, "charStart": 120, "charEnd": 135},
      "confidence": 0.98
    },
    {
      "id": "req_002",
      "type": "CORE",
      "field": "skill",
      "skillId": "redis-cache-design",
      "requiredLevel": 3,
      "importance": 0.82,
      "explicit": true,
      "sourceQuote": "熟悉 Redis 缓存及常见问题",
      "sourceLocator": {"page": 1, "charStart": 260, "charEnd": 278},
      "confidence": 0.91
    }
  ],
  "unknowns": ["internshipDuration"],
  "warnings": []
}
```

技能类要求（`field="skill"`）的等级归属规则：

- 等级词只在其所在分句内生效，逗号、顿号、分号、句号与换行都是分句边界；
- 并列项省略等级词时沿用同组前文等级，此时 `confidence` 低于直接命中；
- 同一段文字被多个技能命中时只保留最长关键词，禁止重复产出要求；
- `type` 按 `BONUS` → `PREFERRED` → `CORE` 顺序判定，规则解析不产出 `HARD_GATE`。

## 5. 个人能力图谱结果

```json
{
  "schemaVersion": "1.0",
  "goalId": "goal_001",
  "skills": [
    {
      "skillId": "redis-cache-design",
      "name": "Redis缓存设计",
      "requiredLevel": 3,
      "estimatedLevel": 2.6,
      "confidence": 0.74,
      "gapType": "PRACTICE_GAP",
      "dimensions": {
        "concept": 3.8,
        "codeReading": 3.1,
        "diagnosis": 2.2,
        "design": 1.8,
        "expression": 2.6,
        "practice": 1.4
      },
      "evidenceIds": ["ev_101", "ev_102"],
      "sourceQuotes": ["简历第2段提到Redis"],
      "nextAction": {
        "type": "MICRO_PRACTICE",
        "taskId": "task_cache_001",
        "reason": "高重要性且缺少可验收实践证据"
      },
  "updatedAt": "2026-09-08T12:30:00Z"
}
```

`nextAction.taskId` 指向对应的学习任务；任务尚未生成时为 `null`，不得用占位 ID 冒充。
`requiredLevel` 取该技能全部要求中的最高明确等级；全部不明确时为 `null`。

## 6. 官方岗位结果

```json
{
  "schemaVersion": "1.0",
  "jobId": "job_001",
  "company": {"name": "示例科技", "officialDomain": "careers.example.com"},
  "title": "Java后端开发实习生",
  "locations": ["西安"],
  "employmentType": "INTERNSHIP",
  "openStatus": "OPEN_CONFIRMED",
  "openStatusReason": [
    "申请按钮可用",
    "页面未显示截止或下线提示"
  ],
  "lastVerifiedAt": "2026-09-08T12:20:00Z",
  "applyUrl": "https://careers.example.com/jobs/123",
  "sourceQuote": "立即申请",
  "sourceLocator": {"url": "https://careers.example.com/jobs/123", "selector": "#apply"},
  "hardGate": {"status": "PASS", "unknownFields": []},
  "skillMatch": {"score": 0.71, "matched": 7, "total": 11},
  "recommendation": "APPLY_AFTER_GAP",
  "warnings": []
}
```

`applyUrl` 只用于用户手动打开。后端不得接受代表“自动投递”的字段或动作。

## 7. 测评题与回答

题目：

```json
{
  "schemaVersion": "1.0",
  "questionId": "q_redis_003",
  "version": 2,
  "skillIds": ["redis-cache-design"],
  "dimension": "diagnosis",
  "difficulty": 3,
  "type": "SCENARIO",
  "prompt": "高并发下热点帖子缓存同时失效，数据库被打满，如何处理？",
  "rubricId": "rubric_cache_01",
  "estimatedMinutes": 2
}
```

回答：

```json
{
  "assessmentId": "assessment_001",
  "questionId": "q_redis_003",
  "answerText": "……",
  "objectiveScore": null,
  "rubricScore": 16,
  "rubricMax": 20,
  "dimensionScores": {"diagnosis": 3.0, "design": 2.5},
  "evidence": [
    {
      "criterion": "cache_breakdown",
      "score": 4,
      "maxScore": 4,
      "quote": "可以用互斥锁让一个请求重建缓存",
      "confidence": 0.86
    }
  ],
  "missingPoints": ["故障降级"],
  "needsReview": false
}
```

## 8. 学习任务与复测

```json
{
  "schemaVersion": "1.0",
  "taskId": "task_cache_001",
  "skillIds": ["redis-cache-design", "performance-testing"],
  "gapTypes": ["PRACTICE_GAP"],
  "jdRequirementIds": ["req_002"],
  "title": "实现热点内容缓存与失效策略",
  "estimatedMinutes": 360,
  "prerequisiteTaskIds": [],
  "resources": ["resource_001"],
  "deliverables": ["代码提交", "测试结果", "设计说明"],
  "acceptanceCriteria": [
    "功能测试通过",
    "说明缓存穿透与击穿处理",
    "提供500并发前后P95对比"
  ],
  "retestPlan": {"questionIds": ["q_redis_equiv_01"], "interviewRequired": true},
  "status": "PLANNED"
}
```

## 9. 简历建议

```json
{
  "schemaVersion": "1.0",
  "suggestionId": "suggest_001",
  "resumeVersionId": "resume_001",
  "targetJobId": "job_001",
  "originalText": "使用Redis优化系统性能。",
  "suggestedText": "为热点内容设计Redis缓存与失效策略，在本地500并发压测下将接口P95由420ms降至135ms。",
  "reason": "补充经过实践验证的技术动作和实验指标",
  "evidenceIds": ["ev_practice_001"],
  "requiresUserConfirmation": true,
  "unsupportedClaims": [],
  "status": "PENDING_USER"
}
```

## 10. 兼容与迁移

- API 版本通过 `/api/v1` 等路径显式管理；
- 只增加可选字段时保持同一版本；
- 删除或改变语义必须新建版本并提供迁移脚本；
- 数据库迁移使用 Flyway 或 Liquibase，禁止运行时自动改表；
- 旧数据缺失字段进入 `UNKNOWN`，不得用默认值伪造证据；
- 每次契约变更附带一条回归测试和变更记录。

## 11. 能力证据登记（2026-09-22 新增，用户批准 P0 范围）

> 本节为 CP-004 证据链的最小契约：只复用第 2 节既有枚举（`EvidenceType` / `EvidenceDirection` / `GapType`），
> 不新增枚举值。机器可读导出物见 `docs/contracts/`，与本节同步维护。

### 11.1 接口

| Method | Path | 说明 |
|---|---|---|
| POST | `/api/v1/goals/{goalId}/evidences` | 登记一条证据，并重算受影响技能的估计 |
| GET | `/api/v1/goals/{goalId}/evidences` | 列出当前用户已登记证据，按 `createdAt` 降序；MVP 不分页，量级增长后启用 `PaginatedResponse` |

### 11.2 登记请求 `RegisterEvidenceRequest`

```json
{
  "type": "PROJECT_RESULT",
  "title": "校园二手书交易平台",
  "contentSummary": "独立完成下单与库存模块，代码与演示视频见仓库",
  "credibility": null,
  "links": [
    {
      "skillId": "spring-boot",
      "direction": "SUPPORTS",
      "claimedLevel": 3,
      "strength": null
    }
  ]
}
```

- `type`：`EvidenceType`，必填；
- `title`：1..200，必填；
- `contentSummary`：≤2000，可空；
- `credibility`：0.0..1.0，可空；`null` 时由类型基准权重决定（见 11.4），不得在前端伪造默认值；
- `links`：1..20，必填；
  - `skillId` 必须已存在于技能库，同一请求内不得重复；
  - `direction`：`EvidenceDirection`；
  - `claimedLevel`：0..5 整数，`SUPPORTS` 时必填，`WEAKENS` / `NEUTRAL` 时可为 `null`；
  - `strength`：0.0..1.0，可空；`null` 视为 `1.0`。

### 11.3 响应 `EvidenceView`

```json
{
  "schemaVersion": "1.0",
  "data": {
    "evidenceId": "0f2b1c0e-…",
    "type": "PROJECT_RESULT",
    "title": "校园二手书交易平台",
    "contentSummary": "独立完成下单与库存模块，代码与演示视频见仓库",
    "credibility": 0.80,
    "occurredAt": null,
    "createdAt": "2026-09-22T08:30:00Z",
    "links": [
      {
        "skillId": "spring-boot",
        "skillName": "Spring Boot",
        "direction": "SUPPORTS",
        "strength": null,
        "claimedLevel": 3
      }
    ],
    "updatedEstimates": [
      {
        "skillId": "spring-boot",
        "estimatedLevel": 3.0,
        "confidence": 0.44,
        "gapType": "KNOWLEDGE_GAP"
      }
    ]
  },
  "traceId": "01JEXAMPLE"
}
```

- `updatedEstimates` 返回本次登记触发重算后各技能的估计结果，字段与第 5 节 `SkillCard` 同名同义；
  GET 列表场景不触发重算，该字段表示受影响技能的**当前**估计现值（估计行不存在时按 11.3 占位语义呈现：`null` / `0.10` / `EVIDENCE_GAP`）；
- `gapType` 语义：估计等级 ≥ 岗位要求或要求未知 → `null`（无明显差距）；估计等级低于要求 → `KNOWLEDGE_GAP`；无证据支撑 → `EVIDENCE_GAP`。

### 11.4 聚合规则（MVP，程序计算，模型不得直接写入估计）

- 类型基准权重 `w(type)`：

| type | w | type | w |
|---|---|---|---|
| SELF_CLAIM | 0.30 | CODE | 0.70 |
| RESUME_CLAIM | 0.45 | PROJECT_RESULT | 0.80 |
| COURSE | 0.55 | INTERVIEW | 0.80 |
| MICRO_PRACTICE | 0.60 | OBJECTIVE_TEST | 0.85 |
| CERTIFICATE | 0.65 | SCENARIO_TEST | 0.85 |
| PROJECT | 0.70 | | |

- 证据显式提供 `credibility` 时覆盖类型基准权重；
- `estimatedLevel = Σ(w × strength × claimedLevel) / Σ(w × strength)`，仅统计 `SUPPORTS` 且 `claimedLevel` 非空的链接，四舍五入到 0.1；
- `confidence = min(0.95, Σ(w × strength) / (Σ(w × strength) + 1.0))`，每条 `WEAKENS` 链接额外扣减 0.15、下限 0.05，四舍五入到 0.01；
- 不存在 `SUPPORTS` 且 `claimedLevel` 非空的链接时：`estimatedLevel = null`、`gapType = EVIDENCE_GAP`、`confidence = 0.10`（与第 5 节"未知不猜测"一致）；
- 本权重表为 MVP 取值，后续 CP-004 完整版修订时同步本节与回归测试，不得在实现里静默改值。

### 11.5 对既有契约的兼容影响

- 第 5 节 `SkillCard.evidenceIds` / `sourceQuotes` 由占位空数组改为填充真实证据引用（字段本身不变）；
- `user_skill_estimate.gap_type` 从只产出 `EVIDENCE_GAP` / `KNOWLEDGE_GAP`（占位）扩展为 11.3 的三种取值，仍在第 2.1 节枚举内；
- 数据库仅新增可空列 `evidence_skill_link.claimed_level`（Flyway V4），不改已有列语义。

