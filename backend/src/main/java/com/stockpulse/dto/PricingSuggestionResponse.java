package com.stockpulse.dto;

import com.stockpulse.entity.PricingSuggestion;
import com.stockpulse.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PricingSuggestionResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal currentPrice;
    private BigDecimal recommendedPrice;
    private PriceDirection direction;
    private Double confidence;
    private String reasoning;
    private SuggestionStatus status;
    private TriggerReason triggerReason;
    private RecommendationSource source;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;

    public static PricingSuggestionResponse from(PricingSuggestion s) {
        PricingSuggestionResponse r = new PricingSuggestionResponse();
        r.id = s.getId();
        r.productId = s.getProduct().getId();
        r.productName = s.getProduct().getName();
        r.productSku = s.getProduct().getSku();
        r.currentPrice = s.getCurrentPrice();
        r.recommendedPrice = s.getRecommendedPrice();
        r.direction = s.getDirection();
        r.confidence = s.getConfidence();
        r.reasoning = s.getReasoning();
        r.status = s.getStatus();
        r.triggerReason = s.getTriggerReason();
        r.source = s.getSource();
        r.createdAt = s.getCreatedAt();
        r.decidedAt = s.getDecidedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSku() { return productSku; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getRecommendedPrice() { return recommendedPrice; }
    public PriceDirection getDirection() { return direction; }
    public Double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public SuggestionStatus getStatus() { return status; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public RecommendationSource getSource() { return source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
}
