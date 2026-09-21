package com.careerpath.jd;

import com.careerpath.jd.dto.JdParseResult;
import com.careerpath.skill.Skill;
import com.careerpath.skill.SkillRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MVP 规则式 JD 解析器。
 *
 * <p>设计约束（对齐 00-codeartsdoer-master.md 强约束 3 与 16-model-prompt-contracts.md 3.1）：
 * <ul>
 *   <li>只抽取文本明确表达的内容，不做语义猜测；</li>
 *   <li>推断内容显式标记 {@code explicit=false}，且不升级为 HARD_GATE；</li>
 *   <li>无法确定的字段进入 {@code unknowns}，等级无法确定时返回 {@code null} 而不是默认值。</li>
 * </ul>
 *
 * <p>这是确定性程序，不调用大模型，因此结果可重复。
 */
@Component
public class RuleBasedJdParser implements JdParser {

    private static final Pattern GRADUATION_RANGE =
            Pattern.compile("(20\\d{2})\\s*[-~至到]\\s*(20\\d{2})\\s*届");
    private static final Pattern GRADUATION_SINGLE =
            Pattern.compile("(20\\d{2})\\s*届");
    private static final Pattern BACHELOR = Pattern.compile("本科|学士");
    private static final Pattern MASTER = Pattern.compile("硕士|研究生|博士");

    /** 等级词：精通档。 */
    private static final Pattern LEVEL_ADVANCED = Pattern.compile("精通|深入|专家|底层原理");
    /** 等级词：熟悉档。 */
    private static final Pattern LEVEL_FAMILIAR = Pattern.compile("熟悉|掌握|熟练");
    /** 等级词：了解档。 */
    private static final Pattern LEVEL_BASIC = Pattern.compile("了解|知道|接触|基础");

    /** 加分项，对应契约 RequirementType 的 BONUS。 */
    private static final Pattern BONUS =
            Pattern.compile("加分|bonus|nice\\s*to\\s*have", Pattern.CASE_INSENSITIVE);
    /** 优先项，对应契约 RequirementType 的 PREFERRED。 */
    private static final Pattern PREFERRED = Pattern.compile("优先|更佳");

    /**
     * 分句分隔符：中英文逗号、顿号、分号、句号、问号、叹号与换行。
     *
     * <p>等级词只在同一分句内归属技能，避免"精通 Java，熟悉 Spring"这类写法中
     * "精通"越界污染 Spring 的等级判断。
     */
    private static final Pattern CLAUSE_DELIMITER = Pattern.compile("[，,、；;。！？!?\\n\\r]+");

    /** 并列项分隔符：只有顿号连接的后续分句才视为省略了等级词。 */
    private static final char ENUMERATION_SEPARATOR = '、';

    /** 同一分句内直接命中等级词时的置信度。 */
    private static final double CONFIDENCE_DIRECT = 0.75;
    /** 分句内无等级词、沿用前文等级时的置信度，低于直接命中以体现推断成分。 */
    private static final double CONFIDENCE_INHERITED = 0.6;

    private final SkillRepository skillRepository;

    public RuleBasedJdParser(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Override
    public JdParseResult parse(String rawText, String sourceId) {
        String text = rawText == null ? "" : rawText;
        List<JdParseResult.RequirementView> requirements = new ArrayList<>();
        List<String> unknowns = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int[] seq = {1};

        extractGraduationYear(text, requirements, unknowns, seq);
        extractEducation(text, requirements, unknowns, seq);
        extractSkills(text, requirements, seq);

        if (requirements.isEmpty()) {
            warnings.add("未从 JD 中识别出任何结构化要求，请人工确认或补充文本");
        }

        return new JdParseResult(
                JdParseResult.SCHEMA_VERSION,
                sourceId,
                new JdParseResult.JobFamilyView("JAVA_BACKEND", 0.5),
                requirements,
                unknowns,
                warnings);
    }

    private void extractGraduationYear(String text, List<JdParseResult.RequirementView> out,
                                       List<String> unknowns, int[] seq) {
        Matcher range = GRADUATION_RANGE.matcher(text);
        if (range.find()) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("min", Integer.parseInt(range.group(1)));
            value.put("max", Integer.parseInt(range.group(2)));
            out.add(requirement(seq, "HARD_GATE", "graduationYear", value, null, null,
                    1.0, true, range.group(), range.start(), range.end(), 0.95));
            return;
        }
        Matcher single = GRADUATION_SINGLE.matcher(text);
        if (single.find()) {
            Map<String, Object> value = new LinkedHashMap<>();
            int year = Integer.parseInt(single.group(1));
            value.put("min", year);
            value.put("max", year);
            out.add(requirement(seq, "HARD_GATE", "graduationYear", value, null, null,
                    1.0, true, single.group(), single.start(), single.end(), 0.9));
            return;
        }
        unknowns.add("graduationYear");
    }

    private void extractEducation(String text, List<JdParseResult.RequirementView> out,
                                  List<String> unknowns, int[] seq) {
        Matcher master = MASTER.matcher(text);
        if (master.find()) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("min", "MASTER");
            out.add(requirement(seq, "HARD_GATE", "educationLevel", value, null, null,
                    1.0, true, master.group(), master.start(), master.end(), 0.9));
            return;
        }
        Matcher bachelor = BACHELOR.matcher(text);
        if (bachelor.find()) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("min", "BACHELOR");
            out.add(requirement(seq, "HARD_GATE", "educationLevel", value, null, null,
                    1.0, true, bachelor.group(), bachelor.start(), bachelor.end(), 0.9));
            return;
        }
        unknowns.add("educationLevel");
    }

    /**
     * 抽取技能要求。
     *
     * <p>三个关键规则：
     * <ol>
     *   <li>同一技能只产出最早且最长的关键词命中，避免重复要求；</li>
     *   <li>同一段文字被多个技能命中时只保留最长者（"Spring Boot" 不再额外产出 "Spring"）；</li>
     *   <li>等级词只在其所属分句内就近归属技能，分句无等级词时沿用前文等级并降低置信度，
     *       完全无线索时 requiredLevel 为 {@code null}（契约要求未知不得猜成已知）。</li>
     * </ol>
     */
    private void extractSkills(String text, List<JdParseResult.RequirementView> out, int[] seq) {
        List<SkillHit> candidates = new ArrayList<>();
        for (Skill skill : skillRepository.findAll()) {
            SkillHit hit = longestHit(text, skill);
            if (hit != null) {
                candidates.add(hit);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        candidates.sort(Comparator.comparingInt(SkillHit::start)
                .thenComparingInt(hit -> -hit.length()));
        List<SkillHit> hits = dropCovered(candidates);

        List<Clause> clauses = splitClauses(text);
        List<Integer> inheritedLevels = resolveInheritedLevels(text, clauses, hits);

        for (SkillHit hit : hits) {
            int clauseIndex = clauseIndexOf(clauses, hit.start());
            Integer requiredLevel = null;
            double confidence = CONFIDENCE_DIRECT;

            if (clauseIndex >= 0) {
                Integer direct = nearestLevel(text, clauses.get(clauseIndex), hit, hits);
                if (direct != null) {
                    requiredLevel = direct;
                } else {
                    Integer inherited = inheritedLevels.get(clauseIndex);
                    if (inherited != null) {
                        requiredLevel = inherited;
                        confidence = CONFIDENCE_INHERITED;
                    }
                }
            }

            Map<String, Object> sourceLocator = new LinkedHashMap<>();
            sourceLocator.put("charStart", hit.start());
            sourceLocator.put("charEnd", hit.end());

            out.add(new JdParseResult.RequirementView(
                    "req_" + seq[0]++,
                    classifyType(clauses, clauseIndex),
                    "skill",
                    null,
                    hit.skillId(),
                    requiredLevel,
                    0.8,
                    true,
                    hit.keyword(),
                    sourceLocator,
                    confidence));
        }
    }

    /** 在技能的全部关键词中取最早出现且最长的一个命中，没有命中时返回 null。 */
    private SkillHit longestHit(String text, Skill skill) {
        SkillHit best = null;
        for (String keyword : keywordsOf(skill)) {
            int idx = text.indexOf(keyword);
            if (idx < 0) {
                continue;
            }
            if (best == null || keyword.length() > best.keyword().length()) {
                best = new SkillHit(skill.getId(), keyword, idx, idx + keyword.length());
            }
        }
        return best;
    }

    /** 丢弃被更长命中区间完全覆盖的候选（输入需按 start 升序、长度降序排列）。 */
    private List<SkillHit> dropCovered(List<SkillHit> sorted) {
        List<SkillHit> kept = new ArrayList<>();
        for (SkillHit candidate : sorted) {
            boolean covered = false;
            for (SkillHit existing : kept) {
                if (existing.start() <= candidate.start()
                        && existing.end() >= candidate.end()
                        && existing.length() > candidate.length()) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    private List<Clause> splitClauses(String text) {
        List<Clause> clauses = new ArrayList<>();
        Matcher matcher = CLAUSE_DELIMITER.matcher(text);
        int cursor = 0;
        char separatorBefore = '\0';
        while (matcher.find()) {
            addClause(clauses, text, cursor, matcher.start(), separatorBefore);
            separatorBefore = text.charAt(matcher.end() - 1);
            cursor = matcher.end();
        }
        addClause(clauses, text, cursor, text.length(), separatorBefore);
        return clauses;
    }

    private void addClause(List<Clause> clauses, String text, int start, int end, char separatorBefore) {
        if (end <= start) {
            return;
        }
        String raw = text.substring(start, end);
        if (raw.isBlank()) {
            return;
        }
        clauses.add(new Clause(start, end, raw, separatorBefore));
    }

    private int clauseIndexOf(List<Clause> clauses, int position) {
        for (int i = 0; i < clauses.size(); i++) {
            Clause clause = clauses.get(i);
            if (position >= clause.start() && position < clause.end()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 计算每个分句可沿用的等级。
     *
     * <p>仅当分句由顿号承接前一项时才沿用前文等级，用于"熟悉 Spring Boot、MyBatis"
     * 这类并列项省略等级词的写法。分号、句号等新分句不继承，原文未给等级时为 {@code null}。
     */
    private List<Integer> resolveInheritedLevels(String text, List<Clause> clauses, List<SkillHit> hits) {
        List<Integer> inherited = new ArrayList<>(clauses.size());
        Integer carried = null;
        for (Clause clause : clauses) {
            Integer leading = leadingLevel(text, clause, hits);
            if (leading != null) {
                carried = leading;
                inherited.add(leading);
                continue;
            }
            inherited.add(clause.separatorBefore() == ENUMERATION_SEPARATOR ? carried : null);
        }
        return inherited;
    }

    private Integer leadingLevel(String text, Clause clause, List<SkillHit> hits) {
        int bestPosition = Integer.MAX_VALUE;
        Integer bestLevel = null;
        for (LevelMark mark : levelMarks(text, clause, hits)) {
            if (mark.position() < bestPosition) {
                bestPosition = mark.position();
                bestLevel = mark.level();
            }
        }
        return bestLevel;
    }

    private Integer nearestLevel(String text, Clause clause, SkillHit hit, List<SkillHit> hits) {
        Integer bestLevel = null;
        int bestDistance = Integer.MAX_VALUE;
        for (LevelMark mark : levelMarks(text, clause, hits)) {
            int distance = distanceTo(mark, hit);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestLevel = mark.level();
            }
        }
        return bestLevel;
    }

    private int distanceTo(LevelMark mark, SkillHit hit) {
        if (mark.end() <= hit.start()) {
            return hit.start() - mark.end();
        }
        if (mark.position() >= hit.end()) {
            return mark.position() - hit.end();
        }
        return 0;
    }

    /**
     * 收集分句内的等级词。
     *
     * <p>落在任何技能关键词区间内的等级词会被排除，否则技能名自带的"基础"
     * （如"Java 基础与集合"）会被误当成本身要求"了解"。
     */
    private List<LevelMark> levelMarks(String text, Clause clause, List<SkillHit> hits) {
        List<LevelMark> marks = new ArrayList<>();
        collectMarks(LEVEL_ADVANCED, 4, clause, hits, marks);
        collectMarks(LEVEL_FAMILIAR, 3, clause, hits, marks);
        collectMarks(LEVEL_BASIC, 2, clause, hits, marks);
        return marks;
    }

    private void collectMarks(Pattern pattern, int level, Clause clause,
                              List<SkillHit> hits, List<LevelMark> marks) {
        Matcher matcher = pattern.matcher(clause.text());
        while (matcher.find()) {
            int start = clause.start() + matcher.start();
            int end = clause.start() + matcher.end();
            if (!insideAnyHit(start, end, hits)) {
                marks.add(new LevelMark(start, end, level));
            }
        }
    }

    private boolean insideAnyHit(int start, int end, List<SkillHit> hits) {
        for (SkillHit hit : hits) {
            if (start >= hit.start() && end <= hit.end()) {
                return true;
            }
        }
        return false;
    }

    private String classifyType(List<Clause> clauses, int clauseIndex) {
        if (clauseIndex < 0) {
            return "CORE";
        }
        String clauseText = clauses.get(clauseIndex).text();
        if (BONUS.matcher(clauseText).find()) {
            return "BONUS";
        }
        if (PREFERRED.matcher(clauseText).find()) {
            return "PREFERRED";
        }
        return "CORE";
    }

    private List<String> keywordsOf(Skill skill) {
        List<String> keywords = new ArrayList<>();
        if (skill.getName() != null && !skill.getName().isBlank()) {
            keywords.add(skill.getName());
        }
        if (skill.getAliases() != null) {
            for (String alias : skill.getAliases().split("[,，]")) {
                String trimmed = alias.trim();
                if (!trimmed.isEmpty()) {
                    keywords.add(trimmed);
                }
            }
        }
        return keywords;
    }

    private JdParseResult.RequirementView requirement(int[] seq, String type, String field,
                                                      Map<String, Object> value, String skillId, Integer requiredLevel,
                                                      Double importance, boolean explicit, String quote,
                                                      int charStart, int charEnd, Double confidence) {
        Map<String, Object> locator = new LinkedHashMap<>();
        locator.put("charStart", charStart);
        locator.put("charEnd", charEnd);
        return new JdParseResult.RequirementView(
                "req_" + seq[0]++, type, field, value, skillId, requiredLevel,
                importance, explicit, quote, locator, confidence);
    }

    /** 技能在原文中的一次命中。 */
    private record SkillHit(String skillId, String keyword, int start, int end) {
        int length() {
            return end - start;
        }
    }

    /** 分句内一个未被技能关键词覆盖的等级词。 */
    private record LevelMark(int position, int end, int level) {
    }

    /** 由分隔符切分出的原文片段，保留绝对字符区间与紧邻的前置分隔符。 */
    private record Clause(int start, int end, String text, char separatorBefore) {
    }
}
