package com.careerpath.evidence;

/**
 * 证据方向，取值严格对齐 14-contracts-and-schemas.md 第 2.1 节 EvidenceDirection，不得增删。
 */
public enum EvidenceDirection {
    SUPPORTS,
    WEAKENS,
    NEUTRAL;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (EvidenceDirection direction : values()) {
            if (direction.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}