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
  ]
}
```

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

