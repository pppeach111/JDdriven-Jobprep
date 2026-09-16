-- V2 种子数据：Java 后端能力标准库（首版岗位族）
-- 依据：README 单一事实来源「首版岗位族：Java 后端实习/校招」
--       15-codeartsdoer-backlog.md CP-004「15~25 项 Java 后端能力」
-- 说明：全部为虚构/通用技能定义，可重复执行。

INSERT INTO skill (id, name, aliases, domain, description) VALUES
    ('java-basics',              'Java 基础与集合',      'Java基础,Java语法,集合框架',       'LANGUAGE',  '语法、面向对象、集合框架、异常与泛型的理解与应用。'),
    ('java-concurrency',         'Java 并发编程',        '并发,多线程,JUC,线程池',           'LANGUAGE',  '线程模型、JUC 工具、锁、线程池与并发问题诊断。'),
    ('jvm',                      'JVM 原理与调优',       'JVM,GC,内存模型,垃圾回收',         'RUNTIME',   '内存结构、类加载、垃圾回收与常见性能问题定位。'),
    ('spring-framework',         'Spring 框架',          'Spring,IoC,AOP,Spring框架',        'FRAMEWORK', 'IoC/AOP 原理、Bean 生命周期与事务管理。'),
    ('spring-boot',              'Spring Boot',          'SpringBoot,自动配置,Starter',      'FRAMEWORK', '自动配置、Starter 机制、配置管理与工程组织。'),
    ('spring-mvc',               'Spring MVC 与 REST',   'SpringMVC,RESTful,接口设计',       'FRAMEWORK', '控制器设计、参数校验、统一响应与异常处理。'),
    ('persistence-mybatis',      '持久层与 ORM',         'MyBatis,JPA,ORM,持久层',           'DATA',      'SQL 映射、事务边界、分页与 N+1 问题处理。'),
    ('mysql',                    'MySQL 数据库',         'MySQL,数据库,索引,SQL优化',        'DATA',      '表设计、索引原理、执行计划与慢查询优化。'),
    ('redis-cache-design',       'Redis 缓存设计',       'Redis,缓存,缓存穿透,缓存击穿',     'DATA',      '缓存模型、失效策略、穿透/击穿/雪崩的应对。'),
    ('message-queue',            '消息队列',             'MQ,Kafka,RocketMQ,消息队列',       'MIDDLEWARE', '异步解耦、消息可靠性、顺序与幂等消费。'),
    ('microservice',             '微服务架构',           '微服务,服务治理,注册中心',         'ARCHITECTURE', '服务拆分、注册发现、配置中心与链路治理。'),
    ('http-network',             'HTTP 与网络基础',      'HTTP,TCP,网络,协议',               'FOUNDATION', 'HTTP 语义、TCP 基础、状态码与常见网络问题排查。'),
    ('linux-basics',             'Linux 基础',           'Linux,Shell,命令',                 'FOUNDATION', '常用命令、权限、进程与日志排查。'),
    ('git-version-control',      'Git 版本控制',         'Git,版本控制,分支',                'TOOLING',   '分支模型、合并与冲突处理、协作规范。'),
    ('maven-build',              'Maven 构建',           'Maven,构建,依赖管理',              'TOOLING',   '依赖管理、生命周期、多模块构建与私服配置。'),
    ('unit-testing',             '单元测试',             '单元测试,JUnit,Mock,测试',         'QUALITY',   '测试分层、Mock 策略、边界用例与覆盖率意识。'),
    ('design-pattern',           '设计模式',             '设计模式,重构,面向对象设计',       'DESIGN',    '常用设计模式场景落地与过度设计识别。'),
    ('dsa',                      '数据结构与算法',       '算法,数据结构,LeetCode',           'FOUNDATION', '常见数据结构、复杂度分析与典型算法题。'),
    ('distributed-system',       '分布式系统',           '分布式,一致性,分布式事务',         'ARCHITECTURE', '一致性、幂等、分布式事务与限流降级。'),
    ('docker-container',         'Docker 与容器化',      'Docker,容器,镜像',                 'DEVOPS',    '镜像构建、容器编排基础与部署实践。')
ON CONFLICT (id) DO NOTHING;

-- 演示用户（虚构数据）
INSERT INTO app_user (id, display_name, timezone, privacy_agreed) VALUES
    ('00000000-0000-0000-0000-000000000001', '演示用户-未验证', 'Asia/Shanghai', TRUE)
ON CONFLICT (id) DO NOTHING;