package com.companyname.ragassistant.dto;

import java.util.List;

public record AskResponse(
        String answer,
        List<CitationDto> citations,
        Double bestScore,
        Long queryLogId,
        long latencyMs,
        String mode,
        String requestId
) {
}
