-- V1 初始 Schema
-- 依据：02-system-architecture.md 核心领域对象、14-contracts-and-schemas.md 统一类型
-- 约定：时间统一 UTC (TIMESTAMPTZ)；主键 UUID；枚举以 VARCHAR + CHECK 表达；未知值使用 NULL。

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- 1. 用户与目标
-- ============================================================

CREATE TABLE app_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name    VARCHAR(120),
    timezone        VARCHAR(64)  NOT NULL DEFAULT 'Asia/Shanghai',
    privacy_agreed  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE job_goal (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES app_user(id),
    name             VARCHAR(200) NOT NULL,
    job_family       VARCHAR(60)  NOT NULL DEFAULT 'JAVA_BACKEND',
    city             VARCHAR(100),
    employment_type  VARCHAR(30),
    graduation_year  INTEGER,
    available_from   DATE,
    weekly_hours     INTEGER,
    status           VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_job_goal_weekly_hours CHECK (weekly_hours IS NULL OR (weekly_hours BETWEEN 1 AND 168)),
    CONSTRAINT ck_job_goal_graduation_year CHECK (graduation_year IS NULL OR (graduation_year BETWEEN 2000 AND 2100)),
    CONSTRAINT ck_job_goal_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_job_goal_user ON job_goal(user_id);

-- 原始 JD（导入即落盘，解析结果与之分离）
CREATE TABLE source_jd (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id         UUID         NOT NULL REFERENCES job_goal(id),
    raw_text        TEXT         NOT NULL,
    source_url      VARCHAR(1000),
    parse_version   VARCHAR(50),
    parse_status    VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    snapshot_url    VARCHAR(1000),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_source_jd_parse_status CHECK (parse_status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_source_jd_goal ON source_jd(goal_id);

-- 简历版本
CREATE TABLE resume_version (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID         NOT NULL REFERENCES app_user(id),
    original_file_url     VARCHAR(1000),
    structured_content    JSONB,
    version_no            INTEGER      NOT NULL DEFAULT 1,
    user_confirmed        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_resume_version_user ON resume_version(user_id);

-- ============================================================
-- 2. 岗位与能力
-- ============================================================

CREATE TABLE skill (
    id           VARCHAR(100) PRIMARY KEY,           -- 如 redis-cache-design
    name         VARCHAR(200) NOT NULL,
    aliases      TEXT,
    domain       VARCHAR(100),
    description  TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE skill_dependency (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prerequisite_id  VARCHAR(100) NOT NULL REFERENCES skill(id),
    dependent_id     VARCHAR(100) NOT NULL REFERENCES skill(id),
    strength         NUMERIC(3,2) NOT NULL DEFAULT 1.0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_skill_dependency_strength CHECK (strength BETWEEN 0.0 AND 1.0),
    CONSTRAINT uq_skill_dependency UNIQUE (prerequisite_id, dependent_id)
);

-- JD 结构化要求
CREATE TABLE job_requirement (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id         UUID         NOT NULL REFERENCES job_goal(id),
    source_jd_id    UUID         REFERENCES source_jd(id),
    requirement_type VARCHAR(30) NOT NULL,
    field           VARCHAR(100),
    skill_id        VARCHAR(100) REFERENCES skill(id),
    required_level  INTEGER,
    importance      NUMERIC(3,2),
    explicit        BOOLEAN      NOT NULL DEFAULT TRUE,
    source_quote    TEXT,
    source_locator  JSONB,
    confidence      NUMERIC(3,2),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_job_requirement_type CHECK (requirement_type IN ('HARD_GATE', 'CORE', 'PREFERRED', 'BONUS')),
    CONSTRAINT ck_job_requirement_required_level CHECK (required_level IS NULL OR (required_level BETWEEN 0 AND 5)),
    CONSTRAINT ck_job_requirement_importance CHECK (importance IS NULL OR (importance BETWEEN 0.0 AND 1.0)),
    CONSTRAINT ck_job_requirement_confidence CHECK (confidence IS NULL OR (confidence BETWEEN 0.0 AND 1.0))
);

CREATE INDEX idx_job_requirement_goal ON job_requirement(goal_id);
CREATE INDEX idx_job_requirement_skill ON job_requirement(skill_id);

-- ============================================================
-- 3. 能力证据与估计
-- ============================================================

CREATE TABLE evidence (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES app_user(id),
    evidence_type    VARCHAR(30)  NOT NULL,
    source           VARCHAR(100),
    content_summary  TEXT,
    credibility      NUMERIC(3,2),
    occurred_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_evidence_type CHECK (evidence_type IN (
        'SELF_CLAIM', 'RESUME_CLAIM', 'COURSE', 'CERTIFICATE', 'PROJECT', 'CODE',
        'OBJECTIVE_TEST', 'SCENARIO_TEST', 'INTERVIEW', 'MICRO_PRACTICE', 'PROJECT_RESULT')),
    CONSTRAINT ck_evidence_credibility CHECK (credibility IS NULL OR (credibility BETWEEN 0.0 AND 1.0))
);

CREATE INDEX idx_evidence_user ON evidence(user_id);

CREATE TABLE evidence_skill_link (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evidence_id      UUID         NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    skill_id         VARCHAR(100) NOT NULL REFERENCES skill(id),
    direction        VARCHAR(20)  NOT NULL,
    strength         NUMERIC(3,2),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_evidence_skill_direction CHECK (direction IN ('SUPPORTS', 'WEAKENS', 'NEUTRAL')),
    CONSTRAINT ck_evidence_skill_strength CHECK (strength IS NULL OR (strength BETWEEN 0.0 AND 1.0)),
    CONSTRAINT uq_evidence_skill UNIQUE (evidence_id, skill_id)
);

-- 用户对某技能在某个目标下的估计（程序计算，模型不得直接写入）
CREATE TABLE user_skill_estimate (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES app_user(id),
    goal_id          UUID         NOT NULL REFERENCES job_goal(id),
    skill_id         VARCHAR(100) NOT NULL REFERENCES skill(id),
    estimated_level  NUMERIC(3,1),
    confidence       NUMERIC(3,2),
    gap_type         VARCHAR(30),
    dimensions       JSONB,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_skill_estimate UNIQUE (user_id, goal_id, skill_id),
    CONSTRAINT ck_user_skill_estimate_level CHECK (estimated_level IS NULL OR (estimated_level BETWEEN 0.0 AND 5.0)),
    CONSTRAINT ck_user_skill_estimate_confidence CHECK (confidence IS NULL OR (confidence BETWEEN 0.0 AND 1.0)),
    CONSTRAINT ck_user_skill_estimate_gap CHECK (gap_type IS NULL OR gap_type IN (
        'QUALIFICATION_BLOCK', 'KNOWLEDGE_GAP', 'APPLICATION_GAP', 'PRACTICE_GAP',
        'EVIDENCE_GAP', 'EXPRESSION_GAP', 'CONFLICT'))
);

CREATE INDEX idx_user_skill_estimate_goal ON user_skill_estimate(goal_id);