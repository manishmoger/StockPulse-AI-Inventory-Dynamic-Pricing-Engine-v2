package com.stockpulse.strategy;

import com.stockpulse.enums.ProductCategory;
import com.stockpulse.enums.TriggerReason;
import java.math.BigDecimal;

public class AdvisorContext {
    private Long productId;
    private String productName;
    private String sku;
    private ProductCategory category;
    private BigDecimal currentPrice;
    private BigDecimal costPrice;
    private int stockLevel;
    private int reorderThreshold;
    private int demandVelocity;
    private double categoryAvgDemandVelocity;
    private TriggerReason triggerReason;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public int getStockLevel() { return stockLevel; }
    public void setStockLevel(int stockLevel) { this.stockLevel = stockLevel; }
    public int getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(int reorderThreshold) { this.reorderThreshold = reorderThreshold; }
    public int getDemandVelocity() { return demandVelocity; }
    public void setDemandVelocity(int demandVelocity) { this.demandVelocity = demandVelocity; }
    public double getCategoryAvgDemandVelocity() { return categoryAvgDemandVelocity; }
    public void setCategoryAvgDemandVelocity(double categoryAvgDemandVelocity) { this.categoryAvgDemandVelocity = categoryAvgDemandVelocity; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public void setTriggerReason(TriggerReason triggerReason) { this.triggerReason = triggerReason; }
}
