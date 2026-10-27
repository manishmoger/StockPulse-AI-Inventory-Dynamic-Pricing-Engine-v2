package com.stockpulse.strategy;

import com.stockpulse.enums.RecommendationSource;

public class ReorderRecommendation {
    private int recommendedQuantity;
    private int suggestedLeadTimeDays;
    private double confidence;
    private String reasoning;
    private RecommendationSource source;

    public int getRecommendedQuantity() { return recommendedQuantity; }
    public void setRecommendedQuantity(int recommendedQuantity) { this.recommendedQuantity = recommendedQuantity; }
    public int getSuggestedLeadTimeDays() { return suggestedLeadTimeDays; }
    public void setSuggestedLeadTimeDays(int suggestedLeadTimeDays) { this.suggestedLeadTimeDays = suggestedLeadTimeDays; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    public RecommendationSource getSource() { return source; }
    public void setSource(RecommendationSource source) { this.source = source; }
}
