package com.stockpulse.dto;

public class StrategyResponse {
    private String activeStrategy;
    private boolean aiConfigured;
    private String aiProvider;
    private String message;

    public String getActiveStrategy() { return activeStrategy; }
    public void setActiveStrategy(String activeStrategy) { this.activeStrategy = activeStrategy; }
    public boolean isAiConfigured() { return aiConfigured; }
    public void setAiConfigured(boolean aiConfigured) { this.aiConfigured = aiConfigured; }
    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
