package com.stockpulse.dto;

import com.stockpulse.entity.ActivityLog;
import com.stockpulse.enums.ActivityType;
import java.time.LocalDateTime;

public class ActivityLogResponse {
    private Long id;
    private ActivityType type;
    private String productSku;
    private String productName;
    private String details;
    private LocalDateTime createdAt;

    public static ActivityLogResponse from(ActivityLog log) {
        ActivityLogResponse r = new ActivityLogResponse();
        r.id = log.getId();
        r.type = log.getType();
        r.productSku = log.getProductSku();
        r.productName = log.getProductName();
        r.details = log.getDetails();
        r.createdAt = log.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public ActivityType getType() { return type; }
    public String getProductSku() { return productSku; }
    public String getProductName() { return productName; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
