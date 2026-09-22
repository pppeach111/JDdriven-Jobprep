package com.careerpath.evidence;

import com.careerpath.jd.JobRequirement;
import com.careerpath.jd.JobRequirementRepository;
import com.careerpath.skill.UserSkillEstimate;
import com.careerpath.skill.UserSkillEstimateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 证据聚合落库：把某用户在某目标某技能上的全部证据重算为估计行（upsert）。
 * 等级与置信度只由本类程序计算写入（14 号契约第 11.4 节），模型输出不得直接写入估计表。
 */
@Service
public class EstimateRecalculator {

    private final EvidenceSkillLinkRepository linkRepository;
    private final UserSkillEstimateRepository estimateRepository;
    private final JobRequirementRepository requirementRepository;

    public EstimateRecalculator(EvidenceSkillLinkRepository linkRepository,
                                UserSkillEstimateRepository estimateRepository,
                                JobRequirementRepository requirementRepository) {
        this.linkRepository = linkRepository;
        this.estimateRepository = estimateRepository;
        this.requirementRepository = requirementRepository;
    }

    @Transactional
    public UserSkillEstimate recalculate(UUID userId, UUID goalId, String skillId) {
        List<EvidenceAggregator.LinkInput> inputs = linkRepository
                .findBySkillIdAndEvidence_UserIdOrderByCreatedAtAsc(skillId, userId)
                .stream()
                .map(this::toInput)
                .toList();

        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(inputs);

        UserSkillEstimate estimate = estimateRepository
                .findByUserIdAndGoalIdAndSkillId(userId, goalId, skillId)
                .orElseGet(() -> {
                    UserSkillEstimate created = new UserSkillEstimate();
                    created.setUserId(userId);
                    created.setGoalId(goalId);
                    created.setSkillId(skillId);
                    return created;
                });

        estimate.setEstimatedLevel(result.estimatedLevel());
        estimate.setConfidence(result.confidence());
        estimate.setGapType(EvidenceAggregator.resolveGapType(result.estimatedLevel(), requiredLevel(goalId, skillId)));
        return estimateRepository.save(estimate);
    }

    /** 取该目标下该技能的最高明确要求等级；全部不明确时为 null（不猜测）。 */
    private Integer requiredLevel(UUID goalId, String skillId) {
        Integer required = null;
        for (JobRequirement requirement : requirementRepository.findByGoalId(goalId)) {
            if (!skillId.equals(requirement.getSkillId())) {
                continue;
            }
            Integer level = requirement.getRequiredLevel();
            if (level != null && (required == null || level > required)) {
                required = level;
            }
        }
        return required;
    }

    private EvidenceAggregator.LinkInput toInput(EvidenceSkillLink link) {
        Evidence evidence = link.getEvidence();
        return new EvidenceAggregator.LinkInput(
                link.getDirection(),
                evidence.getEvidenceType(),
                evidence.getCredibility(),
                link.getStrength(),
                link.getClaimedLevel());
    }
}