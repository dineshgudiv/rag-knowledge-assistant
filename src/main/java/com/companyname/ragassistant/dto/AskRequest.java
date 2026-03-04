package com.companyname.ragassistant.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AskRequest(
    @NotBlank @JsonAlias({"q"}) String question,
    Integer topK,
    Double minScore,
    @JsonAlias({"selected_doc_ids", "selectedDocIds"}) List<Long> allowedDocIds,
    Boolean redactPii
) {}
