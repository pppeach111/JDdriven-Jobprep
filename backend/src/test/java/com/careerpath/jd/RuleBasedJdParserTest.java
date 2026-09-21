package com.careerpath.jd;

import com.careerpath.jd.dto.JdParseResult;
import com.careerpath.skill.Skill;
import com.careerpath.skill.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规则式 JD 解析器单元测试。
 *
 * <p>对齐 14-contracts-and-schemas.md 第 4 节与 16-model-prompt-contracts.md 3.1：
 * 只抽取文本明确表达的内容（explicit=true），无法确定的字段进入 unknowns，
 * 不产生任何语义猜测。全程不依赖数据库。
 */
@DisplayName("RuleBasedJdParser 规则式 JD 解析器")
class RuleBasedJdParserTest {

    private SkillRepository skillRepository;
    private RuleBasedJdParser parser;

    @BeforeEach
    void setUp() {
        skillRepository = Mockito.mock(SkillRepository.class);
        parser = new RuleBasedJdParser(skillRepository);
    }

    // ---------------------------------------------------------------- 辅助

    private static Skill skill(String id, String name, String aliases) {
        Skill s = new Skill();
        s.setId(id);
        s.setName(name);
        s.setAliases(aliases);
        return s;
    }

    private void givenSkills(Skill... skills) {
        Mockito.when(skillRepository.findAll()).thenReturn(new ArrayList<>(List.of(skills)));
    }

    private static JdParseResult.RequirementView byField(JdParseResult result, String field) {
        return result.requirements().stream()
                .filter(r -> field.equals(r.field()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 field=" + field + " 的要求"));
    }

    private static JdParseResult.RequirementView bySkill(JdParseResult result, String skillId) {
        return result.requirements().stream()
                .filter(r -> skillId.equals(r.skillId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 skillId=" + skillId + " 的要求"));
    }

    // ---------------------------------------------------------------- 整体契约

    @Nested
    @DisplayName("结果外层契约")
    class OuterContract {

        @Test
        @DisplayName("完整 JD：schemaVersion/sourceId/jobFamily 符合契约且无 unknowns、warnings")
        void fullJdMatchesOuterContract() {
            givenSkills(
                    skill("java-basics", "Java 基础与集合", "Java基础,集合框架"),
                    skill("redis-cache-design", "Redis 缓存设计", "Redis,缓存"));

            String text = "面向2027-2028届本科生，熟悉 Redis 缓存，精通 Java 基础与集合。";
            JdParseResult result = parser.parse(text, "jd_001");

            assertEquals("1.0", result.schemaVersion());
            assertEquals(JdParseResult.SCHEMA_VERSION, result.schemaVersion());
            assertEquals("jd_001", result.sourceId());
            assertNotNull(result.jobFamily());
            assertEquals("JAVA_BACKEND", result.jobFamily().value());
            assertEquals(0.5, result.jobFamily().confidence(), 0.0);

            assertTrue(result.unknowns().isEmpty(), "字段齐全时 unknowns 必须为空");
            assertTrue(result.warnings().isEmpty(), "识别到要求时不应产生 warning");
            assertEquals(4, result.requirements().size());
        }

        @Test
        @DisplayName("规则解析只抽取明确内容：所有 requirement 的 explicit 必须为 true")
        void allRequirementsAreExplicit() {
            givenSkills(skill("jvm", "JVM 原理与调优", "JVM,GC"));

            JdParseResult result = parser.parse("2026届本科，熟悉 JVM 与 GC。", "jd_002");

            assertFalse(result.requirements().isEmpty());
            for (JdParseResult.RequirementView r : result.requirements()) {
                assertTrue(r.explicit(), "规则解析不产生推断内容，explicit 必须为 true：" + r.id());
            }
        }

        @Test
        @DisplayName("requirement id 按 req_1..req_n 递增且唯一")
        void requirementIdsAreSequentialAndUnique() {
            givenSkills(
                    skill("java-basics", "Java 基础与集合", null),
                    skill("redis-cache-design", "Redis 缓存设计", null));

            JdParseResult result = parser.parse("2026届本科，熟悉 Java 基础与集合，精通 Redis 缓存设计。", "jd_003");

            List<String> ids = result.requirements().stream()
                    .map(JdParseResult.RequirementView::id)
                    .collect(Collectors.toList());
            assertEquals(List.of("req_1", "req_2", "req_3", "req_4"), ids);
            assertEquals(ids.size(), ids.stream().distinct().count());
        }

        @Test
        @DisplayName("importance/confidence 落在 [0,1]，requiredLevel 落在 0..5")
        void numericFieldsRespectContractRanges() {
            givenSkills(
                    skill("java-basics", "Java 基础与集合", "Java基础"),
                    skill("redis-cache-design", "Redis 缓存设计", "Redis"));

            JdParseResult result = parser.parse("2027届硕士，精通 Java 基础与集合，了解 Redis 缓存设计。", "jd_004");

            assertFalse(result.requirements().isEmpty());
            for (JdParseResult.RequirementView r : result.requirements()) {
                assertNotNull(r.importance());
                assertTrue(r.importance() >= 0.0 && r.importance() <= 1.0, "importance 越界: " + r.importance());
                assertNotNull(r.confidence());
                assertTrue(r.confidence() >= 0.0 && r.confidence() <= 1.0, "confidence 越界: " + r.confidence());
                if (r.requiredLevel() != null) {
                    assertTrue(r.requiredLevel() >= 0 && r.requiredLevel() <= 5,
                            "requiredLevel 越界: " + r.requiredLevel());
                }
            }
        }
    }

    // ---------------------------------------------------------------- 毕业年份

    @Nested
    @DisplayName("毕业年份抽取")
    class GraduationYear {

        @Test
        @DisplayName("区间写法：2027-2028届 → HARD_GATE，min/max 正确")
        void rangeForm() {
            givenSkills();
            JdParseResult result = parser.parse("面向2027-2028届毕业生", "jd_010");

            JdParseResult.RequirementView r = byField(result, "graduationYear");
            assertEquals("HARD_GATE", r.type());
            assertEquals(Map.of("min", 2027, "max", 2028), r.value());
            assertEquals("2027-2028届", r.sourceQuote());
            assertEquals(1.0, r.importance(), 0.0);
            assertEquals(0.95, r.confidence(), 0.0);
            assertTrue(r.explicit());
            assertNull(r.skillId());
            // 文本未提学历，因此只有 graduationYear 被确定，educationLevel 进入 unknowns
            assertFalse(result.unknowns().contains("graduationYear"));
            assertEquals(List.of("educationLevel"), result.unknowns());
        }

        @Test
        @DisplayName("波浪线/至/到 等连接符均识别为区间")
        void rangeAcceptsAlternativeConnectors() {
            givenSkills();
            for (String connector : List.of("-", "~", "至", "到")) {
                JdParseResult result = parser.parse("面向2027" + connector + "2028届毕业生", "jd_011");
                JdParseResult.RequirementView r = byField(result, "graduationYear");
                assertEquals(Map.of("min", 2027, "max", 2028), r.value(), "连接符=" + connector);
            }
        }

        @Test
        @DisplayName("单年份写法：2026届 → min == max，confidence 0.9")
        void singleYearForm() {
            givenSkills();
            JdParseResult result = parser.parse("2026届毕业生优先", "jd_012");

            JdParseResult.RequirementView r = byField(result, "graduationYear");
            assertEquals("HARD_GATE", r.type());
            assertEquals(Map.of("min", 2026, "max", 2026), r.value());
            assertEquals("2026届", r.sourceQuote());
            assertEquals(0.9, r.confidence(), 0.0);
        }

        @Test
        @DisplayName("无法确定毕业年份时进入 unknowns，不猜测")
        void unknownWhenAbsent() {
            givenSkills();
            JdParseResult result = parser.parse("我们是一家快速发展的科技公司", "jd_013");

            assertTrue(result.unknowns().contains("graduationYear"));
            assertTrue(result.requirements().stream().noneMatch(r -> "graduationYear".equals(r.field())));
        }
    }

    // ---------------------------------------------------------------- 学历

    @Nested
    @DisplayName("学历抽取")
    class Education {

        @Test
        @DisplayName("硕士/研究生/博士 优先于本科，映射为 MASTER")
        void masterTakesPrecedence() {
            givenSkills();
            JdParseResult result = parser.parse("硕士研究生及以上，本科学历亦可", "jd_020");

            JdParseResult.RequirementView r = byField(result, "educationLevel");
            assertEquals("HARD_GATE", r.type());
            assertEquals(Map.of("min", "MASTER"), r.value());
            assertEquals(1.0, r.importance(), 0.0);
            assertEquals(0.9, r.confidence(), 0.0);
            assertTrue(r.explicit());
        }

        @Test
        @DisplayName("本科/学士 映射为 BACHELOR")
        void bachelor() {
            givenSkills();
            JdParseResult result = parser.parse("本科及以上学历", "jd_021");

            JdParseResult.RequirementView r = byField(result, "educationLevel");
            assertEquals(Map.of("min", "BACHELOR"), r.value());
        }

        @Test
        @DisplayName("无法确定学历时进入 unknowns")
        void unknownWhenAbsent() {
            givenSkills();
            JdParseResult result = parser.parse("熟悉 Redis 缓存设计", "jd_022");

            assertTrue(result.unknowns().contains("educationLevel"));
            assertTrue(result.requirements().stream().noneMatch(r -> "educationLevel".equals(r.field())));
        }
    }

    // ---------------------------------------------------------------- 技能与等级

    @Nested
    @DisplayName("技能抽取与等级推断")
    class SkillsAndLevels {

        @Test
        @DisplayName("精通 → requiredLevel 4")
        void advanced() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis"));
            JdParseResult result = parser.parse("精通 Redis 缓存设计", "jd_030");

            assertEquals(4, bySkill(result, "redis-cache-design").requiredLevel());
        }

        @Test
        @DisplayName("熟悉 → requiredLevel 3")
        void familiar() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis"));
            JdParseResult result = parser.parse("熟悉 Redis 缓存设计", "jd_031");

            assertEquals(3, bySkill(result, "redis-cache-design").requiredLevel());
        }

        @Test
        @DisplayName("了解 → requiredLevel 2")
        void basic() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis"));
            JdParseResult result = parser.parse("了解 Redis 缓存设计", "jd_032");

            assertEquals(2, bySkill(result, "redis-cache-design").requiredLevel());
        }

        @Test
        @DisplayName("未出现等级词时 requiredLevel 为 null，不得猜成\"熟悉\"对应的 3")
        void levelIsNullWhenNoLevelWord() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis"));
            JdParseResult result = parser.parse("使用 Redis 缓存设计", "jd_033");

            assertNull(bySkill(result, "redis-cache-design").requiredLevel());
        }

        @Test
        @DisplayName("技能要求字段契约：type/skillId/value/importance/confidence")
        void skillRequirementContract() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis"));
            JdParseResult result = parser.parse("熟悉 Redis 缓存设计", "jd_034");

            JdParseResult.RequirementView r = bySkill(result, "redis-cache-design");
            assertEquals("CORE", r.type());
            assertEquals("skill", r.field());
            assertNull(r.value(), "skill 类型要求通过 skillId 表达，value 应为 null");
            assertEquals(0.8, r.importance(), 0.0);
            assertEquals(0.75, r.confidence(), 0.0);
            assertTrue(r.explicit());
            assertEquals("Redis 缓存设计", r.sourceQuote());
        }

        @Test
        @DisplayName("上下文中出现 优先/更佳 时标记为 PREFERRED")
        void preferredMarkers() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis"));
            for (String marker : List.of("优先", "更佳")) {
                JdParseResult result = parser.parse("有 Redis 缓存设计经验者" + marker, "jd_035");
                assertEquals("PREFERRED", bySkill(result, "redis-cache-design").type(), "标记=" + marker);
            }
        }

        @Test
        @DisplayName("\"加分\"属于 BONUS，与 PREFERRED 区分")
        void bonusMarkers() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis"));

            for (String marker : List.of("加分", "nice to have")) {
                JdParseResult result = parser.parse("有 Redis 缓存设计经验者" + marker, "jd_044");
                assertEquals("BONUS", bySkill(result, "redis-cache-design").type(), "标记=" + marker);
            }
        }

        @Test
        @DisplayName("等级词不跨分句串扰：\"精通 Java，熟悉 Spring Boot、MyBatis\" 各自定级")
        void levelDoesNotBleedAcrossClauses() {
            givenSkills(
                    skill("java-basics", "Java 基础与集合", "Java基础,Java语法,集合框架,Java"),
                    skill("spring-boot", "Spring Boot", "SpringBoot"),
                    skill("persistence-mybatis", "持久层与 ORM", "MyBatis"));

            JdParseResult result = parser.parse("精通 Java，熟悉 Spring Boot、MyBatis", "jd_070");

            assertEquals(4, bySkill(result, "java-basics").requiredLevel());
            assertEquals(3, bySkill(result, "spring-boot").requiredLevel());
            assertEquals(3, bySkill(result, "persistence-mybatis").requiredLevel(),
                    "省略等级词的并列项应沿用同组前文等级，而非被\"精通\"抬高");
        }

        @Test
        @DisplayName("同分句内的等级词作用于该分句的全部技能")
        void levelWordAppliesToListInSameClause() {
            givenSkills(
                    skill("java-basics", "Java 基础与集合", "Java基础,Java语法,集合框架,Java"),
                    skill("redis-cache-design", "Redis 缓存设计", "Redis"));

            JdParseResult result = parser.parse("精通 Java 和 Redis 缓存设计", "jd_071");

            assertEquals(4, bySkill(result, "java-basics").requiredLevel());
            assertEquals(4, bySkill(result, "redis-cache-design").requiredLevel());
        }

        @Test
        @DisplayName("技能名自带的等级词不参与判断：\"Java 基础与集合\"中的\"基础\"不使其降为 2")
        void levelWordInsideSkillNameIsIgnored() {
            givenSkills(skill("java-basics", "Java 基础与集合", "Java基础"));

            JdParseResult result = parser.parse("熟悉 Java 基础与集合", "jd_072");

            assertEquals(3, bySkill(result, "java-basics").requiredLevel());
        }

        @Test
        @DisplayName("非顿号承接的新分句不继承等级：原文未给等级时为 null")
        void newSentenceDoesNotInheritLevel() {
            givenSkills(
                    skill("docker-container", "Docker 与容器化", "Docker,容器"),
                    skill("microservice", "微服务架构", "微服务"));

            JdParseResult result = parser.parse("了解 Docker；有微服务架构经验者优先", "jd_074");

            assertEquals(2, bySkill(result, "docker-container").requiredLevel());
            assertNull(bySkill(result, "microservice").requiredLevel(),
                    "分号后的新分句不得沿用前一等级");
            assertEquals("PREFERRED", bySkill(result, "microservice").type());
        }

        @Test
        @DisplayName("嵌套关键词只保留最长命中：Spring Boot 不再额外产出 Spring")
        void longestKeywordWinsOverNestedKeyword() {
            givenSkills(
                    skill("spring-framework", "Spring 框架", "Spring,IoC,AOP"),
                    skill("spring-boot", "Spring Boot", "SpringBoot,自动配置"));

            JdParseResult result = parser.parse("熟悉 Spring Boot 自动配置", "jd_073");

            assertTrue(result.requirements().stream()
                            .noneMatch(r -> "spring-framework".equals(r.skillId())),
                    "被更长关键词覆盖的短命中不应产出要求");
            JdParseResult.RequirementView boot = bySkill(result, "spring-boot");
            assertEquals("Spring Boot", boot.sourceQuote());
            assertEquals(3, boot.requiredLevel());
        }

        @Test
        @DisplayName("alias 匹配：技能名未出现但别名命中")
        void matchesAlias() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis,缓存"));
            JdParseResult result = parser.parse("熟悉缓存击穿与穿透处理", "jd_036");

            JdParseResult.RequirementView r = bySkill(result, "redis-cache-design");
            assertEquals("缓存", r.sourceQuote());
        }

        @Test
        @DisplayName("alias 支持中英文逗号分割并忽略空白项")
        void aliasSplitAndTrim() {
            givenSkills(skill("spring-boot", "Spring Boot", "SpringBoot,, Spring Boot ，自动配置"));
            JdParseResult result = parser.parse("熟悉SpringBoot自动配置", "jd_037");

            JdParseResult.RequirementView r = bySkill(result, "spring-boot");
            assertEquals("SpringBoot", r.sourceQuote());
            assertEquals(1, result.requirements().stream()
                    .filter(x -> "spring-boot".equals(x.skillId())).count());
        }

        @Test
        @DisplayName("技能名为 null 时不抛异常，仅依赖别名匹配")
        void nullSkillNameIsTolerated() {
            givenSkills(skill("git-version-control", null, "Git,版本控制"));
            JdParseResult result = parser.parse("熟悉 Git 分支模型", "jd_038");

            assertEquals("Git", bySkill(result, "git-version-control").sourceQuote());
        }

        @Test
        @DisplayName("同一技能重复出现只产生一条要求")
        void duplicateSkillYieldSingleRequirement() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis"));
            JdParseResult result = parser.parse("Redis Redis 缓存设计", "jd_039");

            assertEquals(1, result.requirements().size());
        }

        @Test
        @DisplayName("sourceLocator 精确定位技能关键词在原文中的位置")
        void sourceLocatorIsPrecise() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis"));
            JdParseResult result = parser.parse("熟悉 Redis", "jd_040");

            JdParseResult.RequirementView r = bySkill(result, "redis-cache-design");
            assertEquals(Integer.valueOf(3), r.sourceLocator().get("charStart"));
            assertEquals(Integer.valueOf(8), r.sourceLocator().get("charEnd"));
        }

        @Test
        @DisplayName("技能名为空白字符串时忽略名称，仅依赖别名匹配")
        void blankSkillNameIsIgnored() {
            givenSkills(skill("linux-basics", "   ", "Linux,Shell"));
            JdParseResult result = parser.parse("熟悉 Linux 常用命令", "jd_042");

            assertEquals("Linux", bySkill(result, "linux-basics").sourceQuote());
        }

        @Test
        @DisplayName("技能名与别名均缺失时跳过该技能，不影响其他技能匹配")
        void skillWithoutAnyKeywordIsSkipped() {
            givenSkills(skill("empty-skill", null, null),
                    skill("redis-cache-design", "Redis 缓存设计", "Redis"));
            JdParseResult result = parser.parse("熟悉 Redis 缓存设计", "jd_043");

            assertTrue(result.requirements().stream().noneMatch(r -> "empty-skill".equals(r.skillId())));
            assertEquals("Redis 缓存设计", bySkill(result, "redis-cache-design").sourceQuote());
        }

        @Test
        @DisplayName("技能库为空时不产生技能要求")
        void emptySkillLibrary() {
            givenSkills();
            JdParseResult result = parser.parse("熟悉 Redis 缓存设计", "jd_041");

            assertTrue(result.requirements().stream().noneMatch(r -> "skill".equals(r.field())));
        }
    }

    // ---------------------------------------------------------------- 错误/边界路径

    @Nested
    @DisplayName("错误与边界路径")
    class ErrorPaths {

        @Test
        @DisplayName("无任何可识别内容：requirements 为空，产出 warning 且两项 unknown")
        void nothingRecognized() {
            givenSkills();
            JdParseResult result = parser.parse("我们是一家快速发展的互联网公司，欢迎投递", "jd_050");

            assertTrue(result.requirements().isEmpty());
            assertEquals(List.of("graduationYear", "educationLevel"), result.unknowns());
            assertFalse(result.warnings().isEmpty());
            assertTrue(result.warnings().get(0).contains("未从 JD 中识别出任何结构化要求"));
        }

        @Test
        @DisplayName("rawText 为 null 时降级为空文本，不抛异常")
        void nullRawText() {
            givenSkills();
            JdParseResult result = parser.parse(null, "jd_051");

            assertNotNull(result);
            assertEquals("jd_051", result.sourceId());
            assertTrue(result.requirements().isEmpty());
            assertEquals(List.of("graduationYear", "educationLevel"), result.unknowns());
            assertFalse(result.warnings().isEmpty());
        }

        @Test
        @DisplayName("rawText 为空字符串与纯空白时不抛异常")
        void blankRawText() {
            givenSkills();
            for (String text : List.of("", "   ", "\n\t")) {
                JdParseResult result = parser.parse(text, "jd_052");
                assertTrue(result.requirements().isEmpty());
                assertFalse(result.warnings().isEmpty());
            }
        }

        @Test
        @DisplayName("sourceId 原样透传，包括 null")
        void sourceIdIsPassedThrough() {
            givenSkills();
            assertEquals("jd_053", parser.parse("2026届", "jd_053").sourceId());
            assertNull(parser.parse("2026届", null).sourceId());
        }

        @Test
        @DisplayName("解析是确定性的：同一输入两次结果完全一致")
        void parsingIsDeterministic() {
            givenSkills(skill("redis-cache-design", "Redis 缓存设计", "Redis"));
            String text = "2026届本科，精通 Redis 缓存设计";

            JdParseResult first = parser.parse(text, "jd_054");
            JdParseResult second = parser.parse(text, "jd_054");

            assertEquals(first, second, "规则解析器不依赖随机性，结果必须可重复");
        }

        @Test
        @DisplayName("技能等级词在不同技能间互不串扰（各取自身上下文）")
        void levelsAreScopedToSkillContext() {
            givenSkills(
                    skill("java-basics", "Java 基础与集合", "Java基础"),
                    skill("redis-cache-design", "Redis 缓存设计", "Redis"));

            // 用中性填充把两个技能隔开，确保第二个技能的等级上下文不包含第一个技能的等级词
            String text = "精通 Java 基础与集合。" + "。".repeat(30) + "了解 Redis 缓存设计";
            JdParseResult result = parser.parse(text, "jd_055");

            assertEquals(4, bySkill(result, "java-basics").requiredLevel());
            assertEquals(2, bySkill(result, "redis-cache-design").requiredLevel());
        }
    }

    // ---------------------------------------------------------------- 接口契约

    @Test
    @DisplayName("解析器实现 JdParser 抽象，可经接口多态调用")
    void implementsJdParserAbstraction() {
        givenSkills();
        JdParser abstraction = parser;
        JdParseResult result = abstraction.parse("2026届本科生", "jd_060");

        assertEquals("jd_060", result.sourceId());
        assertEquals(Map.of("min", 2026, "max", 2026), byField(result, "graduationYear").value());
    }
}