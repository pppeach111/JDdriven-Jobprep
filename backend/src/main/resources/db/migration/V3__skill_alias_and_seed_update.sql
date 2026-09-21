-- V3 技能词条补全
-- 背景：端到端联调实证，JD 原文"精通 Java"与"Kubernetes"无法命中任何技能词条，
--       导致要求被漏抽。V2 已由 Flyway 应用，禁止就地修改，故以新增迁移补足。
-- 说明：语句幂等，可安全重复执行。

-- 1. java-basics 补足裸 "Java" 别名，使"精通 Java"这类常见写法可被命中
UPDATE skill
   SET aliases    = 'Java基础,Java语法,集合框架,Java',
       updated_at = now()
 WHERE id = 'java-basics';

-- 2. 新增 Kubernetes 能力词条（首版岗位族仍落在 15~25 项要求范围内）
INSERT INTO skill (id, name, aliases, domain, description) VALUES
    ('kubernetes', 'Kubernetes 与容器编排', 'Kubernetes,K8s,容器编排', 'DEVOPS',
     'Pod/Deployment/Service 等核心对象、配置与集群运维基础。')
ON CONFLICT (id) DO NOTHING;