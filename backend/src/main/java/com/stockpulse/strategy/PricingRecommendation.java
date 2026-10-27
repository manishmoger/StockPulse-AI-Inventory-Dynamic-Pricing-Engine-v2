package com.stockpulse.strategy;

import com.stockpulse.enums.PriceDirection;
import com.stockpulse.enums.RecommendationSource;
import java.math.BigDecimal;

public class PricingRecommendation {
    private BigDecimal recommendedPrice;
    private PriceDirection direction;
    private double confidence;
    private String reasoning;
    private RecommendationSource source;

    public BigDecimal getRecommendedPrice() { return recommendedPrice; }
    public void setRecommendedPrice(BigDecimal recommendedPrice) { this.recommendedPrice = recommendedPrice; }
    public PriceDirection getDirection() { return direction; }
    public void setDirection(PriceDirection direction) { this.direction = direction; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    public RecommendationSource getSource() { return source; }
    public void setSource(RecommendationSource source) { this.source = source; }
}
