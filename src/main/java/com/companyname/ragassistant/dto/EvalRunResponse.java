package com.companyname.ragassistant.dto;

public record EvalRunResponse(
        Long runId,
        Integer total,
        Integer passed,
        Double score,
        Double avgFaithfulness,
        Double avgRelevancy,
        Integer avgRetrievalMs,
        Integer avgGenerationMs,
        Integer avgLatencyMs
) {
}
