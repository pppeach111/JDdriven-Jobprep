package com.careerpath.jd;

import com.careerpath.jd.dto.JdParseResult;

/**
 * JD 解析器抽象。
 *
 * <p>规则实现用于 MVP 与模型不可用时的降级（见 16-model-prompt-contracts.md 第 11 节）；
 * 后续接入 {@code jd_extract.v1} 模型实现，通过配置切换，业务层不感知。
 */
public interface JdParser {

    JdParseResult parse(String rawText, String sourceId);
}