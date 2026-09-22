package com.careerpath.skill;

import com.careerpath.common.error.BusinessException;
import com.careerpath.common.error.ErrorCode;
import com.careerpath.evidence.Evidence;
import com.careerpath.evidence.EvidenceSkillLink;
import com.careerpath.evidence.EvidenceSkillLinkRepository;
import com.careerpath.goal.JobGoalRepository;
import com.careerpath.jd.JobRequirement;
import com.careerpath.jd.JobRequirementRepository;
import com.careerpath.skill.dto.CapabilityMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 能力图谱聚合。
 *
 * <p>关键边界（对齐 00-codeartsdoer-master.md 强约束 2 与 02-system-architecture.md 6.2）：
 * 等级与置信度由程序计算并写入，模型输出不得直接写入本表。
 *
 * <p>MVP 规则：
 * <ul>
 *   <li>无证据的技能 → 等级未知、置信度极低、差距类型 EVIDENCE_GAP；</li>
 *   <li>有证据但低于岗位要求 → 按维度差异归入知识/应用/实践差距。</li>
 * </ul>
 */
@Service
public class CapabilityService {

    private static final BigDecimal NO_EVIDENCE_CONFIDENCE = new BigDecimal("0.10");
    private static final String GAP_EVIDENCE = "EVIDENCE_GAP";
    private static final String GAP_KNOWLEDGE = "KNOWLEDGE_GAP";

    private final JobGoalRepository goalRepository;
    private final JobRequirementRepository requirementRepository;
    private final SkillRepository skillRepository;
    private final UserSkillEstimateRepository estimateRepository;
    private final EvidenceSkillLinkRepository evidenceLinkRepository;

    public CapabilityService(JobGoalRepository goalRepository,
                             JobRequirementRepository requirementRepository,
                             SkillRepository skillRepository,
                             UserSkillEstimateRepository estimateRepository,
                             EvidenceSkillLinkRepository evidenceLinkRepository) {
        this.goalRepository = goalRepository;
        this.requirementRepository = requirementRepository;
        this.skillRepository = skillRepository;
        this.estimateRepository = estimateRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
    }

    @Transactional
    public CapabilityMap buildMap(UUID userId, UUID goalId) {
        if (!goalRepository.existsById(goalId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "求职目标不存在");
        }

        Map<String, Integer> requiredLevels = collectRequiredLevels(goalId);
        List<CapabilityMap.SkillCard> cards = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : requiredLevels.entrySet()) {
            String skillId = entry.getKey();
            Integer requiredLevel = entry.getValue();

            UserSkillEstimate estimate = estimateRepository
                    .findByUserIdAndGoalIdAndSkillId(userId, goalId, skillId)
                    .orElseGet(() -> createPlaceholder(userId, goalId, skillId));

            String name = skillRepository.findById(skillId)
                    .map(Skill::getName)
                    .orElse(skillId);

            List<EvidenceSkillLink> links = evidenceLinkRepository
                    .findBySkillIdAndEvidence_UserIdOrderByCreatedAtAsc(skillId, userId);
            List<String> evidenceIds = links.stream()
                    .map(link -> link.getEvidence().getId().toString())
                    .toList();
            List<String> sourceQuotes = links.stream()
                    .map(link -> quoteOf(link.getEvidence()))
                    .filter(quote -> !quote.isEmpty())
                    .toList();

            cards.add(new CapabilityMap.SkillCard(
                    skillId,
                    name,
                    requiredLevel,
                    estimate.getEstimatedLevel(),
                    estimate.getConfidence(),
                    estimate.getGapType(),
                    estimate.getDimensions(),
                    evidenceIds,
                    sourceQuotes,
                    buildNextAction(estimate.getGapType()),
                    estimate.getUpdatedAt()));
        }

        return new CapabilityMap(CapabilityMap.SCHEMA_VERSION, goalId.toString(), cards);
    }

    /**
     * 汇总每个技能的岗位要求等级。
     *
     * <p>JD 未明确等级（requiredLevel 为 null）时不猜测默认值：若该技能存在任何明确等级，
     * 取最高者；全部不明确时保留 null，由界面按"未知"呈现（14-contracts-and-schemas.md 第 1 节）。
     */
    private Map<String, Integer> collectRequiredLevels(UUID goalId) {
        Map<String, Integer> required = new LinkedHashMap<>();
        for (JobRequirement requirement : requirementRepository.findByGoalId(goalId)) {
            String skillId = requirement.getSkillId();
            if (skillId == null) {
                continue;
            }
            Integer level = requirement.getRequiredLevel();
            if (required.containsKey(skillId)) {
                required.put(skillId, higherLevel(required.get(skillId), level));
            } else {
                required.put(skillId, level);
            }
        }
        return required;
    }

    private static Integer higherLevel(Integer left, Integer right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return Math.max(left, right);
    }

    private UserSkillEstimate createPlaceholder(UUID userId, UUID goalId, String skillId) {
        UserSkillEstimate estimate = new UserSkillEstimate();
        estimate.setUserId(userId);
        estimate.setGoalId(goalId);
        estimate.setSkillId(skillId);
        estimate.setEstimatedLevel(null);
        estimate.setConfidence(NO_EVIDENCE_CONFIDENCE);
        estimate.setGapType(GAP_EVIDENCE);
        estimate.setDimensions(new HashMap<>(Map.of(
                "concept", 0,
                "codeReading", 0,
                "diagnosis", 0,
                "design", 0,
                "expression", 0,
                "practice", 0)));
        return estimateRepository.save(estimate);
    }

    private CapabilityMap.NextAction buildNextAction(String gapType) {
        if (GAP_EVIDENCE.equals(gapType)) {
            return new CapabilityMap.NextAction("ASSESSMENT", null,
                    "该能力缺少可验证证据，建议完成一次自适应测评以建立基线");
        }
        if (GAP_KNOWLEDGE.equals(gapType)) {
            return new CapabilityMap.NextAction("MICRO_PRACTICE", null,
                    "存在知识或实践差距，建议通过学习任务补齐并复测");
        }
        return null;
    }

    /**
     * 证据引用原文（契约第 5 节 sourceQuotes）：优先证据说明，缺失时回退标题；最长保留 80 字符。
     */
    private static String quoteOf(Evidence evidence) {
        String summary = evidence.getContentSummary();
        if (summary != null && !summary.isBlank()) {
            return summary.length() <= QUOTE_MAX_LENGTH ? summary : summary.substring(0, QUOTE_MAX_LENGTH);
        }
        String title = evidence.getTitle();
        if (title == null) {
            return "";
        }
        return title.length() <= QUOTE_MAX_LENGTH ? title : title.substring(0, QUOTE_MAX_LENGTH);
    }

    private static final int QUOTE_MAX_LENGTH = 80;

    public Optional<UserSkillEstimate> findEstimate(UUID userId, UUID goalId, String skillId) {
        return estimateRepository.findByUserIdAndGoalIdAndSkillId(userId, goalId, skillId);
    }
}