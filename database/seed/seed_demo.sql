-- 演示种子数据（全部虚构，可重复执行）
-- 依据：shared_docs/17-environment-and-runbook.md 第 6 节
--   「种子数据包含一个"未验证用户"、一个"已完成实践用户"…全部虚构、脱敏、可重复执行」
-- 说明：
--   - 「未验证用户」由 V2 迁移创建（id 尾号 0001）；
--   - 本文件创建「已完成实践用户」（id 尾号 0002）及其目标、证据和能力估计；
--   - 岗位与题库表尚未建立（属 CP-005/CP-006），此处不预置。
-- 幂等：所有 INSERT 均带 ON CONFLICT DO NOTHING。

-- ============================================================
-- 1. 已完成实践的演示用户
-- ============================================================
INSERT INTO app_user (id, display_name, timezone, privacy_agreed) VALUES
    ('00000000-0000-0000-0000-000000000002', '演示用户-已完成实践', 'Asia/Shanghai', TRUE)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 2. 该用户的求职目标
-- ============================================================
INSERT INTO job_goal (id, user_id, name, job_family, city, employment_type, graduation_year, status) VALUES
    ('00000000-0000-0000-0000-0000000000a1',
     '00000000-0000-0000-0000-000000000002',
     'Java 后端开发（校招）', 'JAVA_BACKEND', '深圳', 'CAMPUS', 2026, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 3. 岗位要求（演示用，直接落结构化结果；真实场景由 JD 解析写入）
-- ============================================================
INSERT INTO job_requirement
    (id, goal_id, source_jd_id, requirement_type, field, skill_id, required_level, importance, explicit, source_quote, confidence)
VALUES
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-0000000000a1', NULL,
     'CORE', 'skill', 'java-basics', 3, 0.90, TRUE, '熟悉 Java 基础与集合框架', 0.80),
    ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-0000000000a1', NULL,
     'CORE', 'skill', 'spring-boot', 3, 0.85, TRUE, '熟悉 Spring Boot 开发', 0.80),
    ('00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-0000000000a1', NULL,
     'CORE', 'skill', 'mysql', 3, 0.80, TRUE, '熟悉 MySQL 与索引优化', 0.80),
    ('00000000-0000-0000-0000-000000000104', '00000000-0000-0000-0000-0000000000a1', NULL,
     'CORE', 'skill', 'redis-cache-design', 3, 0.70, TRUE, '了解 Redis 缓存设计', 0.80),
    ('00000000-0000-0000-0000-000000000105', '00000000-0000-0000-0000-0000000000a1', NULL,
     'PREFERRED', 'skill', 'java-concurrency', 3, 0.60, TRUE, '有并发编程实践经验者优先', 0.75)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 4. 可验证证据（虚构）
-- ============================================================
INSERT INTO evidence (id, user_id, evidence_type, source, content_summary, credibility, occurred_at) VALUES
    ('00000000-0000-0000-0000-0000000000e1',
     '00000000-0000-0000-0000-000000000002',
     'COURSE', '演示课程平台',
     '完成 Java 并发编程专项课程，含线程池与 JUC 工具的编程练习。',
     0.60, TIMESTAMPTZ '2026-03-10 00:00:00+08'),
    ('00000000-0000-0000-0000-0000000000e2',
     '00000000-0000-0000-0000-000000000002',
     'PROJECT_RESULT', '演示课程项目',
     '实现订单查询接口（Spring Boot + MyBatis），补充联合索引后 P95 响应从 800ms 降至 120ms。',
     0.75, TIMESTAMPTZ '2026-05-20 00:00:00+08')
ON CONFLICT (id) DO NOTHING;

INSERT INTO evidence_skill_link (id, evidence_id, skill_id, direction, strength) VALUES
    ('00000000-0000-0000-0000-0000000000f1',
     '00000000-0000-0000-0000-0000000000e1', 'java-concurrency', 'SUPPORTS', 0.60),
    ('00000000-0000-0000-0000-0000000000f2',
     '00000000-0000-0000-0000-0000000000e2', 'spring-boot', 'SUPPORTS', 0.75),
    ('00000000-0000-0000-0000-0000000000f3',
     '00000000-0000-0000-0000-0000000000e2', 'mysql', 'SUPPORTS', 0.70)
ON CONFLICT (evidence_id, skill_id) DO NOTHING;

-- ============================================================
-- 5. 技能估计（演示程序计算结果；真实值由测评与证据规则写入）
--    gap_type 为 NULL 表示当前证据下无明显差距。
-- ============================================================
INSERT INTO user_skill_estimate
    (id, user_id, goal_id, skill_id, estimated_level, confidence, gap_type, dimensions) VALUES
    ('00000000-0000-0000-0000-0000000000c1',
     '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-0000000000a1',
     'spring-boot', 3.5, 0.75, NULL,
     '{"concept":4,"codeReading":4,"diagnosis":3,"design":3,"expression":3,"practice":4}'),
    ('00000000-0000-0000-0000-0000000000c2',
     '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-0000000000a1',
     'mysql', 3.0, 0.70, NULL,
     '{"concept":3,"codeReading":3,"diagnosis":4,"design":3,"expression":3,"practice":3}'),
    ('00000000-0000-0000-0000-0000000000c3',
     '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-0000000000a1',
     'java-concurrency', 2.5, 0.60, 'APPLICATION_GAP',
     '{"concept":3,"codeReading":3,"diagnosis":2,"design":2,"expression":2,"practice":3}'),
    ('00000000-0000-0000-0000-0000000000c4',
     '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-0000000000a1',
     'redis-cache-design', NULL, 0.10, 'EVIDENCE_GAP',
     '{"concept":0,"codeReading":0,"diagnosis":0,"design":0,"expression":0,"practice":0}')
ON CONFLICT (user_id, goal_id, skill_id) DO NOTHING;