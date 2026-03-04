package com.companyname.ragassistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CitationDto(
        @JsonProperty("chunk_id") Long chunkId,
        @JsonProperty("doc_id") Long documentId,
        @JsonProperty("doc_name") String docName,
        @JsonProperty("parent_id") Long parentId,
        @JsonProperty("chunk_index") Integer chunkIndex,
        @JsonProperty("final_score") Double score,
        @JsonProperty("vec_score") Double scoreVec,
        @JsonProperty("kw_score") Double scoreKw,
        @JsonProperty("rerank_score") Double scoreRerank,
        @JsonProperty("support_span") String supportSpan,
        @JsonProperty("text") String text,
        @JsonProperty("char_start") Integer startOffset,
        @JsonProperty("char_end") Integer endOffset,
        @JsonProperty("page_start") Integer pageStart,
        @JsonProperty("page_end") Integer pageEnd
) {
}
