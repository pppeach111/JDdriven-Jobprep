package com.careerpath.evidence;

import com.careerpath.common.error.BusinessException;
import com.careerpath.common.error.ErrorCode;
import com.careerpath.evidence.dto.EvidenceView;
import com.careerpath.evidence.dto.RegisterEvidenceRequest;
import com.careerpath.goal.JobGoalRepository;
import com.careerpath.skill.Skill;
import com.careerpath.skill.SkillRepository;
import com.careerpath.skill.UserSkillEstimate;
import com.careerpath.skill.UserSkillEstimateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 证据登记编排：校验 → 落库证据与链接 → 触发受影响技能重算。
 * 证据归属用户；登记接口挂载在目标路径下，用于把证据录入动作锚定到目标流程。
 */
@Service
public class EvidenceService {

    /** 无估计行时的占位语义，与 CapabilityService 占位一致（契约 11.4）。 */
    private static final BigDecimal PLACEHOLDER_CONFIDENCE = new BigDecimal("0.10");
    private static final String PLACEHOLDER_GAP = "EVIDENCE_GAP";

    private final EvidenceRepository evidenceRepository;
    private final EvidenceSkillLinkRepository linkRepository;
    private final JobGoalRepository goalRepository;
    private final SkillRepository skillRepository;
    private final UserSkillEstimateRepository estimateRepository;
    private final EstimateRecalculator recalculator;

    public EvidenceService(EvidenceRepository evidenceRepository,
                           EvidenceSkillLinkRepository linkRepository,
                           JobGoalRepository goalRepository,
                           SkillRepository skillRepository,
                           UserSkillEstimateRepository estimateRepository,
                           EstimateRecalculator recalculator) {
        this.evidenceRepository = evidenceRepository;
        this.linkRepository = linkRepository;
        this.goalRepository = goalRepository;
        this.skillRepository = skillRepository;
        this.estimateRepository = estimateRepository;
        this.recalculator = recalculator;
    }

    @Transactional
    public EvidenceView register(UUID userId, UUID goalId, RegisterEvidenceRequest request) {
        requireGoal(goalId);
        validateEnums(request);

        Evidence evidence = new Evidence();
        evidence.setUserId(userId);
        evidence.setEvidenceType(request.type());
        evidence.setTitle(request.title());
        evidence.setSource(Evidence.SOURCE_USER_FORM);
        evidence.setContentSummary(request.contentSummary());
        evidence.setCredibility(request.credibility());
        evidence.setOccurredAt(null);
        evidenceRepository.save(evidence);

        Set<String> seenSkillIds = new HashSet<>();
        List<EvidenceView.LinkView> linkViews = new ArrayList<>();
        for (RegisterEvidenceRequest.Link input : request.links()) {
            if (!seenSkillIds.add(input.skillId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "同一条证据中技能 " + input.skillId() + " 重复出现");
            }
            Skill skill = skillRepository.findById(input.skillId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "技能不存在: " + input.skillId()));
            if (EvidenceDirection.SUPPORTS.name().equals(input.direction()) && input.claimedLevel() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "方向为 SUPPORTS 的链接必须提供 claimedLevel（0..5）");
            }

            EvidenceSkillLink link = new EvidenceSkillLink();
            link.setEvidence(evidence);
            link.setSkillId(skill.getId());
            link.setDirection(input.direction());
            link.setStrength(input.strength());
            link.setClaimedLevel(input.claimedLevel());
            linkRepository.save(link);

            linkViews.add(new EvidenceView.LinkView(
                    skill.getId(), skill.getName(), input.direction(), input.strength(), input.claimedLevel()));
        }

        List<EvidenceView.UpdatedEstimate> updatedEstimates = new ArrayList<>();
        for (String skillId : seenSkillIds) {
            UserSkillEstimate estimate = recalculator.recalculate(userId, goalId, skillId);
            updatedEstimates.add(new EvidenceView.UpdatedEstimate(
                    skillId, estimate.getEstimatedLevel(), estimate.getConfidence(), estimate.getGapType()));
        }

        return toView(evidence, linkViews, updatedEstimates);
    }

    @Transactional(readOnly = true)
    public List<EvidenceView> list(UUID userId, UUID goalId) {
        requireGoal(goalId);
        List<EvidenceView> views = new ArrayList<>();
        for (Evidence evidence : evidenceRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            List<EvidenceSkillLink> links = linkRepository.findByEvidence_IdOrderByCreatedAtAsc(evidence.getId());
            List<EvidenceView.LinkView> linkViews = new ArrayList<>();
            Set<String> skillIds = new LinkedHashSet<>();
            for (EvidenceSkillLink link : links) {
                String skillName = skillRepository.findById(link.getSkillId())
                        .map(Skill::getName)
                        .orElse(link.getSkillId());
                linkViews.add(new EvidenceView.LinkView(
                        link.getSkillId(), skillName, link.getDirection(), link.getStrength(), link.getClaimedLevel()));
                skillIds.add(link.getSkillId());
            }
            views.add(toView(evidence, linkViews, currentEstimates(userId, goalId, skillIds)));
        }
        return views;
    }

    private void validateEnums(RegisterEvidenceRequest request) {
        if (!EvidenceType.isValid(request.type())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "未知的证据类型: " + request.type());
        }
        for (RegisterEvidenceRequest.Link link : request.links()) {
            if (!EvidenceDirection.isValid(link.direction())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "未知的证据方向: " + link.direction());
            }
        }
    }

    private void requireGoal(UUID goalId) {
        if (!goalRepository.existsById(goalId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "求职目标不存在");
        }
    }

    /** GET 场景的 updatedEstimates：受影响技能的当前估计现值；行不存在时按占位语义呈现（契约 11.3）。 */
    private List<EvidenceView.UpdatedEstimate> currentEstimates(UUID userId, UUID goalId, Set<String> skillIds) {
        List<EvidenceView.UpdatedEstimate> estimates = new ArrayList<>();
        for (String skillId : skillIds) {
            UserSkillEstimate estimate = estimateRepository
                    .findByUserIdAndGoalIdAndSkillId(userId, goalId, skillId)
                    .orElse(null);
            estimates.add(new EvidenceView.UpdatedEstimate(
                    skillId,
                    estimate == null ? null : estimate.getEstimatedLevel(),
                    estimate == null ? PLACEHOLDER_CONFIDENCE : estimate.getConfidence(),
                    estimate == null ? PLACEHOLDER_GAP : estimate.getGapType()));
        }
        return estimates;
    }

    private EvidenceView toView(Evidence evidence,
                                List<EvidenceView.LinkView> links,
                                List<EvidenceView.UpdatedEstimate> updatedEstimates) {
        return new EvidenceView(
                evidence.getId().toString(),
                evidence.getEvidenceType(),
                evidence.getTitle(),
                evidence.getContentSummary(),
                evidence.getCredibility(),
                evidence.getOccurredAt(),
                evidence.getCreatedAt(),
                links,
                updatedEstimates);
    }
}
