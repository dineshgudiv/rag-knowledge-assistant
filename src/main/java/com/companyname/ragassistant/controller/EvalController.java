package com.companyname.ragassistant.controller;

import com.companyname.ragassistant.dto.EvalRunResponse;
import com.companyname.ragassistant.model.EvalRun;
import com.companyname.ragassistant.service.AuditService;
import com.companyname.ragassistant.service.EvalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/eval")
public class EvalController {
    private final EvalService evalService;
    private final AuditService auditService;

    public EvalController(EvalService evalService, AuditService auditService) {
        this.evalService = evalService;
        this.auditService = auditService;
    }

    @PostMapping("/run")
    public EvalRunResponse run(@RequestParam(required=false) Integer topK,
                               @RequestParam(required=false) Double minScore) {
        long start = System.currentTimeMillis();
        EvalRun r = evalService.run(topK, minScore);
        int latencyMs = (int) (System.currentTimeMillis() - start);
        auditService.logEval(latencyMs);
        return new EvalRunResponse(
                r.getId(),
                r.getTotalCases(),
                r.getPassedCases(),
                r.getScore(),
                r.getAvgFaithfulness(),
                r.getAvgRelevancy(),
                r.getAvgRetrievalMs(),
                r.getAvgGenerationMs(),
                r.getAvgLatencyMs()
        );
    }

    @GetMapping("/runs")
    public List<EvalRun> list(@RequestParam(defaultValue = "10") int limit) {
        return evalService.listRuns(limit);
    }

    @GetMapping("/runs/{id}")
    public EvalRun get(@PathVariable Long id) {
        return evalService.getRun(id);
    }
}
