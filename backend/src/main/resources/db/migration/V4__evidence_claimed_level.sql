-- V4 证据登记支撑（14-contracts-and-schemas.md 第 11 节，2026-09-22 新增，用户批准 P0 范围）
-- 1) evidence 补充 title：表单登记的证据需要人类可读标题，契约 11.2/11.3 均为必填字段；
-- 2) evidence_skill_link 补充 claimed_level：用户对"这项证据表明我达到哪一级"的回答。
-- 兼容性：均为新增可空列，不改已有列语义；校验由应用层执行（V1 时 evidence 表无存量行）。

ALTER TABLE evidence
    ADD COLUMN title VARCHAR(200);

ALTER TABLE evidence_skill_link
    ADD COLUMN claimed_level INTEGER;

ALTER TABLE evidence_skill_link
    ADD CONSTRAINT ck_evidence_skill_claimed_level
    CHECK (claimed_level IS NULL OR (claimed_level BETWEEN 0 AND 5));
