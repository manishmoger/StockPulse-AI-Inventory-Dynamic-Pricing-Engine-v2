package com.stockpulse.dto;

import com.stockpulse.entity.Product;
import com.stockpulse.enums.ProductCategory;
import com.stockpulse.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponse {
    private Long id;
    private String sku;
    private String name;
    private ProductCategory category;
    private BigDecimal currentPrice;
    private BigDecimal costPrice;
    private Integer stockLevel;
    private Integer reorderThreshold;
    private Integer demandVelocity;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product p) {
        ProductResponse r = new ProductResponse();
        r.id = p.getId();
        r.sku = p.getSku();
        r.name = p.getName();
        r.category = p.getCategory();
        r.currentPrice = p.getCurrentPrice();
        r.costPrice = p.getCostPrice();
        r.stockLevel = p.getStockLevel();
        r.reorderThreshold = p.getReorderThreshold();
        r.demandVelocity = p.getDemandVelocity();
        r.status = p.getStatus();
        r.createdAt = p.getCreatedAt();
        r.updatedAt = p.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public ProductCategory getCategory() { return category; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public Integer getStockLevel() { return stockLevel; }
    public Integer getReorderThreshold() { return reorderThreshold; }
    public Integer getDemandVelocity() { return demandVelocity; }
    public ProductStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
