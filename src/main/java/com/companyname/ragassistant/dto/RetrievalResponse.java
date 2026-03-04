package com.companyname.ragassistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RetrievalResponse(
        String q,
        Integer topK,
        Double minScore,
        @JsonProperty("hits") List<CitationDto> hits
) {
}
