package com.companyname.ragassistant.mapper;

import com.companyname.ragassistant.dto.EvalRunResponse;
import com.companyname.ragassistant.model.EvalRun;
import org.springframework.stereotype.Component;

@Component
public class EvalMapper {

    public EvalRunResponse toResponse(EvalRun run) {
        return new EvalRunResponse(
                run.getId(),
                run.getTotalCases(),
                run.getPassedCases(),
                run.getScore(),
                run.getAvgFaithfulness(),
                run.getAvgRelevancy(),
                run.getAvgRetrievalMs(),
                run.getAvgGenerationMs(),
                run.getAvgLatencyMs()
        );
    }
}
