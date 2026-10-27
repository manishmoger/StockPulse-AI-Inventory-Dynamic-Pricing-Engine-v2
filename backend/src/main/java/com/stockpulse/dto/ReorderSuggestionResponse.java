package com.stockpulse.dto;

import com.stockpulse.entity.ReorderSuggestion;
import com.stockpulse.enums.*;
import java.time.LocalDateTime;

public class ReorderSuggestionResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer currentStock;
    private Integer recommendedQuantity;
    private Integer suggestedLeadTimeDays;
    private Double confidence;
    private String reasoning;
    private SuggestionStatus status;
    private TriggerReason triggerReason;
    private RecommendationSource source;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;

    public static ReorderSuggestionResponse from(ReorderSuggestion s) {
        ReorderSuggestionResponse r = new ReorderSuggestionResponse();
        r.id = s.getId();
        r.productId = s.getProduct().getId();
        r.productName = s.getProduct().getName();
        r.productSku = s.getProduct().getSku();
        r.currentStock = s.getCurrentStock();
        r.recommendedQuantity = s.getRecommendedQuantity();
        r.suggestedLeadTimeDays = s.getSuggestedLeadTimeDays();
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
    public Integer getCurrentStock() { return currentStock; }
    public Integer getRecommendedQuantity() { return recommendedQuantity; }
    public Integer getSuggestedLeadTimeDays() { return suggestedLeadTimeDays; }
    public Double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public SuggestionStatus getStatus() { return status; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public RecommendationSource getSource() { return source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
}
