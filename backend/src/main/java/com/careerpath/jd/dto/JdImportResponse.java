package com.careerpath.jd.dto;

import java.util.UUID;

public record JdImportResponse(UUID sourceId, String parseStatus) {
}