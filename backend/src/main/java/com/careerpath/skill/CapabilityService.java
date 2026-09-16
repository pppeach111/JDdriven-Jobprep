package com.careerpath.skill;

import com.careerpath.common.error.BusinessException;
import com.careerpath.common.error.ErrorCode;
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

    public CapabilityService(JobGoalRepository goalRepository,
                             JobRequirementRepository requirementRepository,
                             SkillRepository skillRepository,
                             UserSkillEstimateRepository estimateRepository) {
        this.goalRepository = goalRepository;
        this.requirementRepository = requirementRepository;
        this.skillRepository = skillRepository;
        this.estimateRepository = estimateRepository;
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
            int requiredLevel = entry.getValue();

            UserSkillEstimate estimate = estimateRepository
                    .findByUserIdAndGoalIdAndSkillId(userId, goalId, skillId)
                    .orElseGet(() -> createPlaceholder(userId, goalId, skillId));

            String name = skillRepository.findById(skillId)
                    .map(Skill::getName)
                    .orElse(skillId);

            cards.add(new CapabilityMap.SkillCard(
                    skillId,
                    name,
                    requiredLevel,
                    estimate.getEstimatedLevel(),
                    estimate.getConfidence(),
                    estimate.getGapType(),
                    estimate.getDimensions(),
                    List.of(),
                    List.of(),
                    buildNextAction(estimate.getGapType()),
                    estimate.getUpdatedAt()));
        }

        return new CapabilityMap(CapabilityMap.SCHEMA_VERSION, goalId.toString(), cards);
    }

    private Map<String, Integer> collectRequiredLevels(UUID goalId) {
        Map<String, Integer> required = new LinkedHashMap<>();
        for (JobRequirement requirement : requirementRepository.findByGoalId(goalId)) {
            String skillId = requirement.getSkillId();
            if (skillId == null) {
                continue;
            }
            int level = requirement.getRequiredLevel() == null ? 3 : requirement.getRequiredLevel();
            required.merge(skillId, level, Math::max);
        }
        return required;
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
            return new CapabilityMap.NextAction("ASSESSMENT",
                    "该能力缺少可验证证据，建议完成一次自适应测评以建立基线");
        }
        if (GAP_KNOWLEDGE.equals(gapType)) {
            return new CapabilityMap.NextAction("MICRO_PRACTICE",
                    "存在知识或实践差距，建议通过学习任务补齐并复测");
        }
        return null;
    }

    public Optional<UserSkillEstimate> findEstimate(UUID userId, UUID goalId, String skillId) {
        return estimateRepository.findByUserIdAndGoalIdAndSkillId(userId, goalId, skillId);
    }
}