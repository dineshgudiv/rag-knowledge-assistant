package com.companyname.ragassistant.controller;

import com.companyname.ragassistant.dto.CitationDto;
import com.companyname.ragassistant.dto.RetrievalResponse;
import com.companyname.ragassistant.service.RetrievalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/retrieval")
public class RetrievalController {

    private final RetrievalService retrievalService;

    public RetrievalController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @GetMapping("/search")
    public RetrievalResponse search(@RequestParam("q") String q,
                                    @RequestParam(defaultValue = "3") Integer topK,
                                    @RequestParam(defaultValue = "0.2") Double minScore,
                                    @RequestParam(name = "selected_doc_ids", required = false) String selectedDocIds) {
        int safeTopK = topK == null ? 3 : Math.max(1, Math.min(5, topK));
        List<Long> allowedDocIds = parseSelected(selectedDocIds);
        List<CitationDto> hits = retrievalService.retrieve(q, safeTopK, allowedDocIds, true).stream()
                .filter(c -> c.score() != null && c.score() >= (minScore == null ? 0.2 : minScore))
                .toList();
        return new RetrievalResponse(q, safeTopK, minScore, hits);
    }

    private List<Long> parseSelected(String selectedDocIds) {
        if (selectedDocIds == null || selectedDocIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(selectedDocIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }
}
