package com.stockpulse.dto;

import jakarta.validation.constraints.NotBlank;

public class StrategyRequest {
    @NotBlank(message = "Strategy name is required")
    private String strategy;

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
}
