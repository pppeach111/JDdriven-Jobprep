package com.careerpath.common.web;

import org.slf4j.MDC;

public final class TraceId {

    private TraceId() {
    }

    public static String current() {
        String id = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return id == null || id.isBlank() ? "unknown" : id;
    }
}