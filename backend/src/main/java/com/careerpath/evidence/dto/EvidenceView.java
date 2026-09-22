package com.careerpath.evidence.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 一条已登记证据及其受影响技能的重算结果，结构严格对齐 14-contracts-and-schemas.md 第 11.3 节。
 * updatedEstimates：POST 为本次登记触发重算后的结果；GET 为各技能当前估计现值。
 */
public record EvidenceView(
        String evidenceId,
        String type,
        String title,
        String contentSummary,
        BigDecimal credibility,
        Instant occurredAt,
        Instant createdAt,
        List<LinkView> links,
        List<UpdatedEstimate> updatedEstimates
) {

    public record LinkView(
            String skillId,
            String skillName,
            String direction,
            BigDecimal strength,
            Integer claimedLevel
    ) {
    }

    public record UpdatedEstimate(
            String skillId,
            BigDecimal estimatedLevel,
            BigDecimal confidence,
            String gapType
    ) {
    }
}