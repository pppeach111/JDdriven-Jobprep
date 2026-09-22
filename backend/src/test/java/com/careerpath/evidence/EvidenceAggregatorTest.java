package com.careerpath.evidence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 证据聚合纯函数回归测试（14-contracts-and-schemas.md 第 11.4 节 MVP 权重表）。
 * 权重表调整必须同步本测试与契约第 11.4 节，不得静默改值。
 */
class EvidenceAggregatorTest {

    private static EvidenceAggregator.LinkInput supports(String type, Integer claimedLevel) {
        return new EvidenceAggregator.LinkInput("SUPPORTS", type, null, null, claimedLevel);
    }

    private static EvidenceAggregator.LinkInput weakens() {
        return new EvidenceAggregator.LinkInput("WEAKENS", "PROJECT", null, null, 2);
    }

    @Test
    void noLinksReturnsPlaceholder() {
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(List.of());
        assertNull(result.estimatedLevel());
        assertEquals(0, new BigDecimal("0.10").compareTo(result.confidence()));
    }

    @Test
    void onlyWeakensKeepsPlaceholder() {
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(List.of(weakens()));
        assertNull(result.estimatedLevel());
        assertEquals(0, new BigDecimal("0.10").compareTo(result.confidence()));
    }

    @Test
    void supportsWithoutClaimedLevelIgnored() {
        EvidenceAggregator.LinkInput noClaim = new EvidenceAggregator.LinkInput(
                "SUPPORTS", "PROJECT", null, null, null);
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(List.of(noClaim));
        assertNull(result.estimatedLevel());
        assertEquals(0, new BigDecimal("0.10").compareTo(result.confidence()));
    }

    @Test
    void singleSelfClaimSupportsLevel3() {
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(List.of(supports("SELF_CLAIM", 3)));
        assertEquals(0, new BigDecimal("3.0").compareTo(result.estimatedLevel()));
        // w=0.30 → conf = 0.30 / 1.30 = 0.23
        assertEquals(0, new BigDecimal("0.23").compareTo(result.confidence()));
    }

    @Test
    void weightedAverageAcrossTypes() {
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(List.of(
                supports("PROJECT", 4),
                supports("COURSE", 2)));
        // (0.70×4 + 0.55×2) / 1.25 = 3.12 → 3.1
        assertEquals(0, new BigDecimal("3.1").compareTo(result.estimatedLevel()));
        // 1.25 / 2.25 = 0.56
        assertEquals(0, new BigDecimal("0.56").compareTo(result.confidence()));
    }

    @Test
    void explicitStrengthReducesWeight() {
        EvidenceAggregator.LinkInput weak = new EvidenceAggregator.LinkInput(
                "SUPPORTS", "PROJECT", null, new BigDecimal("0.5"), 4);
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(List.of(
                weak,
                supports("COURSE", 2)));
        // (0.70×0.5×4 + 0.55×2) / (0.35+0.55) = (1.4+1.1)/0.9 = 2.8
        assertEquals(0, new BigDecimal("2.8").compareTo(result.estimatedLevel()));
        // 0.9 / 1.9 = 0.47
        assertEquals(0, new BigDecimal("0.47").compareTo(result.confidence()));
    }

    @Test
    void explicitCredibilityOverridesTypeWeight() {
        EvidenceAggregator.LinkInput strongClaim = new EvidenceAggregator.LinkInput(
                "SUPPORTS", "SELF_CLAIM", new BigDecimal("0.9"), null, 3);
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(List.of(strongClaim));
        assertEquals(0, new BigDecimal("3.0").compareTo(result.estimatedLevel()));
        // 0.9 / 1.9 = 0.47
        assertEquals(0, new BigDecimal("0.47").compareTo(result.confidence()));
    }

    @Test
    void weakensReducesConfidenceButNotLevel() {
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(List.of(
                supports("PROJECT", 4),
                weakens()));
        assertEquals(0, new BigDecimal("4.0").compareTo(result.estimatedLevel()));
        // 0.70 / 1.70 = 0.41 → −0.15 = 0.26
        assertEquals(0, new BigDecimal("0.26").compareTo(result.confidence()));
    }

    @Test
    void confidenceFlooredAt005() {
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(List.of(
                supports("PROJECT", 4),
                weakens(), weakens(), weakens(), weakens(), weakens()));
        assertEquals(0, new BigDecimal("4.0").compareTo(result.estimatedLevel()));
        assertEquals(0, new BigDecimal("0.05").compareTo(result.confidence()));
    }

    @Test
    void confidenceCappedAt095() {
        List<EvidenceAggregator.LinkInput> manyStrong = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            manyStrong.add(new EvidenceAggregator.LinkInput(
                    "SUPPORTS", "OBJECTIVE_TEST", new BigDecimal("1.0"), null, 5));
        }
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(manyStrong);
        assertEquals(0, new BigDecimal("5.0").compareTo(result.estimatedLevel()));
        assertEquals(0, new BigDecimal("0.95").compareTo(result.confidence()));
    }

    @Test
    void neutralDoesNotAffectResult() {
        EvidenceAggregator.LinkInput neutral = new EvidenceAggregator.LinkInput(
                "NEUTRAL", "PROJECT", null, null, 5);
        EvidenceAggregator.Result result = EvidenceAggregator.aggregate(List.of(
                supports("SELF_CLAIM", 3),
                neutral));
        assertEquals(0, new BigDecimal("3.0").compareTo(result.estimatedLevel()));
        assertEquals(0, new BigDecimal("0.23").compareTo(result.confidence()));
    }

    @ParameterizedTest
    @CsvSource({
            ", , EVIDENCE_GAP",
            "2.5, 3, KNOWLEDGE_GAP",
            "3.0, 3, ",
            "4.0, 3, ",
            "3.0, , ",
    })
    void gapTypeRules(String estimated, Integer required, String expected) {
        BigDecimal level = estimated == null ? null : new BigDecimal(estimated);
        assertEquals(expected, EvidenceAggregator.resolveGapType(level, required));
    }
}