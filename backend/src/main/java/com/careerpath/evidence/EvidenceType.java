package com.careerpath.evidence;

/**
 * 证据类型，取值严格对齐 14-contracts-and-schemas.md 第 2.1 节 EvidenceType，不得增删。
 */
public enum EvidenceType {
    SELF_CLAIM,
    RESUME_CLAIM,
    COURSE,
    CERTIFICATE,
    PROJECT,
    CODE,
    OBJECTIVE_TEST,
    SCENARIO_TEST,
    INTERVIEW,
    MICRO_PRACTICE,
    PROJECT_RESULT;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (EvidenceType type : values()) {
            if (type.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}