package com.stockpulse.dto;

import com.stockpulse.enums.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public class DecisionRequest {
    @NotNull(message = "Decision is required")
    private SuggestionStatus decision;

    public SuggestionStatus getDecision() { return decision; }
    public void setDecision(SuggestionStatus decision) { this.decision = decision; }
}
