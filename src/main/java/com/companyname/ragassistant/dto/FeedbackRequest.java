package com.companyname.ragassistant.dto;

import jakarta.validation.constraints.NotNull;

public record FeedbackRequest(
        @NotNull Long queryLogId,
        @NotNull Boolean helpful,
        String comment
) {
}
