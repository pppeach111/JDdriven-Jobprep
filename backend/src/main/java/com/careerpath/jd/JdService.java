package com.careerpath.jd;

import com.careerpath.common.error.BusinessException;
import com.careerpath.common.error.ErrorCode;
import com.careerpath.goal.JobGoalRepository;
import com.careerpath.jd.dto.ImportJdRequest;
import com.careerpath.jd.dto.JdParseResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * JD 导入与解析编排。
 *
 * <p>原始文本、解析状态与结构化要求分离存储，保证可追溯与可重解析。
 * 解析只写入 {@link JobRequirement}，不写入用户能力估计。
 */
@Service
public class JdService {

    private static final String PARSER_VERSION = "rule-based.v1";

    private final JobGoalRepository goalRepository;
    private final SourceJdRepository sourceJdRepository;
    private final JobRequirementRepository requirementRepository;
    private final JdParser jdParser;

    public JdService(JobGoalRepository goalRepository,
                     SourceJdRepository sourceJdRepository,
                     JobRequirementRepository requirementRepository,
                     JdParser jdParser) {
        this.goalRepository = goalRepository;
        this.sourceJdRepository = sourceJdRepository;
        this.requirementRepository = requirementRepository;
        this.jdParser = jdParser;
    }

    @Transactional
    public SourceJd importJd(UUID goalId, ImportJdRequest request) {
        requireGoal(goalId);
        SourceJd jd = new SourceJd();
        jd.setGoalId(goalId);
        jd.setRawText(request.rawText());
        jd.setSourceUrl(request.sourceUrl());
        jd.setParseStatus("PENDING");
        return sourceJdRepository.save(jd);
    }

    @Transactional
    public JdParseResult analyze(UUID goalId) {
        requireGoal(goalId);
        List<SourceJd> jds = sourceJdRepository.findByGoalIdOrderByCreatedAtDesc(goalId);
        if (jds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "尚未导入目标 JD，无法解析");
        }
        SourceJd jd = jds.get(0);

        JdParseResult result = jdParser.parse(jd.getRawText(), jd.getId().toString());

        requirementRepository.deleteBySourceJdId(jd.getId());
        for (JdParseResult.RequirementView view : result.requirements()) {
            requirementRepository.save(toEntity(goalId, jd.getId(), view));
        }

        jd.setParseVersion(PARSER_VERSION);
        jd.setParseStatus(result.requirements().isEmpty() ? "PARTIAL" : "SUCCEEDED");
        sourceJdRepository.save(jd);

        return result;
    }

    private void requireGoal(UUID goalId) {
        if (!goalRepository.existsById(goalId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "求职目标不存在");
        }
    }

    private JobRequirement toEntity(UUID goalId, UUID sourceJdId, JdParseResult.RequirementView view) {
        JobRequirement entity = new JobRequirement();
        entity.setGoalId(goalId);
        entity.setSourceJdId(sourceJdId);
        entity.setRequirementType(view.type());
        entity.setField(view.field());
        entity.setSkillId(view.skillId());
        entity.setRequiredLevel(view.requiredLevel());
        entity.setImportance(view.importance() == null ? null : BigDecimal.valueOf(view.importance()));
        entity.setExplicit(view.explicit());
        entity.setSourceQuote(view.sourceQuote());
        entity.setSourceLocator(view.sourceLocator());
        entity.setConfidence(view.confidence() == null ? null : BigDecimal.valueOf(view.confidence()));
        return entity;
    }
}