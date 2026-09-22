package com.careerpath.evidence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 证据聚合的纯函数实现（MVP 权重表）。
 *
 * <p>规则严格对齐 14-contracts-and-schemas.md 第 11.4 节，权重表调整必须同步该节与回归测试，
 * 不得在实现里静默改值。等级与置信度由本类程序计算，模型输出不得直接写入估计表。
 */
public final class EvidenceAggregator {

    private EvidenceAggregator() {
    }

    /** 无任何 SUPPORTS 证据时的固定置信度（契约 11.4）。 */
    public static final BigDecimal NO_SUPPORT_CONFIDENCE = new BigDecimal("0.10");

    /** 置信度上限与 WEAKENS 惩罚、下限（契约 11.4）。 */
    private static final BigDecimal CONFIDENCE_CAP = new BigDecimal("0.95");
    private static final BigDecimal CONFIDENCE_FLOOR = new BigDecimal("0.05");
    private static final BigDecimal WEAKENS_PENALTY = new BigDecimal("0.15");

    /** 类型基准权重表（契约 11.4）。 */
    private static final Map<String, BigDecimal> TYPE_WEIGHTS = Map.ofEntries(
            Map.entry("SELF_CLAIM", new BigDecimal("0.30")),
            Map.entry("RESUME_CLAIM", new BigDecimal("0.45")),
            Map.entry("COURSE", new BigDecimal("0.55")),
            Map.entry("MICRO_PRACTICE", new BigDecimal("0.60")),
            Map.entry("CERTIFICATE", new BigDecimal("0.65")),
            Map.entry("PROJECT", new BigDecimal("0.70")),
            Map.entry("CODE", new BigDecimal("0.70")),
            Map.entry("PROJECT_RESULT", new BigDecimal("0.80")),
            Map.entry("INTERVIEW", new BigDecimal("0.80")),
            Map.entry("OBJECTIVE_TEST", new BigDecimal("0.85")),
            Map.entry("SCENARIO_TEST", new BigDecimal("0.85")));

    /**
     * 聚合输入：一条证据-技能链接。
     *
     * @param direction    EvidenceDirection 字符串
     * @param evidenceType EvidenceType 字符串（上游已校验合法）
     * @param credibility  证据显式可信度，null 时用类型基准权重
     * @param strength     链接关联强度，null 视为 1.0
     * @param claimedLevel 声称等级 0..5，SUPPORTS 必填
     */
    public record LinkInput(String direction, String evidenceType, BigDecimal credibility, BigDecimal strength,
                            Integer claimedLevel) {
    }

    /**
     * 聚合结果。
     *
     * @param estimatedLevel 加权声称等级（0.0..5.0，一位小数）；无有效支持证据时为 null
     * @param confidence     置信度（0.05..0.95，两位小数）
     */
    public record Result(BigDecimal estimatedLevel, BigDecimal confidence) {
    }

    public static Result aggregate(List<LinkInput> links) {
        BigDecimal supportedWeight = BigDecimal.ZERO;
        BigDecimal weightedLevelSum = BigDecimal.ZERO;
        int weakensCount = 0;

        for (LinkInput link : links) {
            BigDecimal weight = effectiveWeight(link);
            BigDecimal strength = link.strength() == null ? BigDecimal.ONE : link.strength();

            if (EvidenceDirection.WEAKENS.name().equals(link.direction())) {
                weakensCount++;
                continue;
            }
            if (!EvidenceDirection.SUPPORTS.name().equals(link.direction()) || link.claimedLevel() == null) {
                continue;
            }

            BigDecimal effective = weight.multiply(strength);
            supportedWeight = supportedWeight.add(effective);
            weightedLevelSum = weightedLevelSum.add(effective.multiply(BigDecimal.valueOf(link.claimedLevel())));
        }

        if (supportedWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return new Result(null, NO_SUPPORT_CONFIDENCE);
        }

        BigDecimal level = weightedLevelSum
                .divide(supportedWeight, 3, RoundingMode.HALF_UP)
                .setScale(1, RoundingMode.HALF_UP);

        BigDecimal confidence = supportedWeight
                .divide(supportedWeight.add(BigDecimal.ONE), 4, RoundingMode.HALF_UP);
        if (weakensCount > 0) {
            confidence = confidence.subtract(WEAKENS_PENALTY.multiply(BigDecimal.valueOf(weakensCount)));
        }
        confidence = confidence.max(CONFIDENCE_FLOOR).min(CONFIDENCE_CAP).setScale(2, RoundingMode.HALF_UP);

        return new Result(level, confidence);
    }

    /**
     * 差距类型决策（契约 11.3）：估计缺失 → EVIDENCE_GAP；低于要求 → KNOWLEDGE_GAP；
     * 达到要求或要求未知 → null（无明显差距）。取值仍在第 2.1 节 GapType 枚举内。
     */
    public static String resolveGapType(BigDecimal estimatedLevel, Integer requiredLevel) {
        if (estimatedLevel == null) {
            return "EVIDENCE_GAP";
        }
        if (requiredLevel == null) {
            return null;
        }
        return estimatedLevel.compareTo(BigDecimal.valueOf(requiredLevel)) < 0 ? "KNOWLEDGE_GAP" : null;
    }

    private static BigDecimal effectiveWeight(LinkInput link) {
        if (link.credibility() != null) {
            return link.credibility();
        }
        // 类型合法性已在上游校验；兜底 1.0 仅防御未登记的新类型，正常不触达。
        return TYPE_WEIGHTS.getOrDefault(link.evidenceType(), BigDecimal.ONE);
    }
}