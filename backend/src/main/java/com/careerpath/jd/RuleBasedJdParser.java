package com.careerpath.jd;

import com.careerpath.jd.dto.JdParseResult;
import com.careerpath.skill.Skill;
import com.careerpath.skill.SkillRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
 *   <li>无法确定的字段进入 {@code unknowns}。</li>
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

    private static final Pattern LEVEL_ADVANCED = Pattern.compile("精通|深入|专家|底层原理");
    private static final Pattern LEVEL_FAMILIAR = Pattern.compile("熟悉|掌握|熟练");
    private static final Pattern LEVEL_BASIC = Pattern.compile("了解|知道|接触|基础");

    private static final Pattern PREFERRED = Pattern.compile("优先|加分|更佳|nice to have", Pattern.CASE_INSENSITIVE);

    /** 技能名匹配时向左右各取多少字符作为等级判断上下文 */
    private static final int CONTEXT_WINDOW = 24;

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

    private void extractSkills(String text, List<JdParseResult.RequirementView> out, int[] seq) {
        for (Skill skill : skillRepository.findAll()) {
            for (String keyword : keywordsOf(skill)) {
                int idx = text.indexOf(keyword);
                if (idx < 0) {
                    continue;
                }
                int contextStart = Math.max(0, idx - CONTEXT_WINDOW);
                int contextEnd = Math.min(text.length(), idx + keyword.length() + CONTEXT_WINDOW);
                String context = text.substring(contextStart, contextEnd);

                int requiredLevel = inferLevel(context);
                String requirementType = PREFERRED.matcher(context).find() ? "PREFERRED" : "CORE";
                double confidence = 0.75;

                Map<String, Object> sourceLocator = new LinkedHashMap<>();
                sourceLocator.put("charStart", idx);
                sourceLocator.put("charEnd", idx + keyword.length());

                out.add(new JdParseResult.RequirementView(
                        "req_" + seq[0]++,
                        requirementType,
                        "skill",
                        null,
                        skill.getId(),
                        requiredLevel,
                        0.8,
                        true,
                        keyword,
                        sourceLocator,
                        confidence));
                break;
            }
        }
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

    private int inferLevel(String context) {
        if (LEVEL_ADVANCED.matcher(context).find()) {
            return 4;
        }
        if (LEVEL_FAMILIAR.matcher(context).find()) {
            return 3;
        }
        if (LEVEL_BASIC.matcher(context).find()) {
            return 2;
        }
        return 3;
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
}