package com.stockpulse.dto;

import java.util.List;
import java.util.Map;

public class DashboardMetrics {
    private long totalProducts;
    private long lowStockCount;
    private long outOfStockCount;
    private long pendingPricingSuggestions;
    private long pendingReorderSuggestions;
    private long demandSpikeCount;
    private Map<String, Long> inventoryStatusDistribution;
    private List<ActivityLogResponse> recentActivity;

    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }
    public long getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(long lowStockCount) { this.lowStockCount = lowStockCount; }
    public long getOutOfStockCount() { return outOfStockCount; }
    public void setOutOfStockCount(long outOfStockCount) { this.outOfStockCount = outOfStockCount; }
    public long getPendingPricingSuggestions() { return pendingPricingSuggestions; }
    public void setPendingPricingSuggestions(long pendingPricingSuggestions) { this.pendingPricingSuggestions = pendingPricingSuggestions; }
    public long getPendingReorderSuggestions() { return pendingReorderSuggestions; }
    public void setPendingReorderSuggestions(long pendingReorderSuggestions) { this.pendingReorderSuggestions = pendingReorderSuggestions; }
    public long getDemandSpikeCount() { return demandSpikeCount; }
    public void setDemandSpikeCount(long demandSpikeCount) { this.demandSpikeCount = demandSpikeCount; }
    public Map<String, Long> getInventoryStatusDistribution() { return inventoryStatusDistribution; }
    public void setInventoryStatusDistribution(Map<String, Long> inventoryStatusDistribution) { this.inventoryStatusDistribution = inventoryStatusDistribution; }
    public List<ActivityLogResponse> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<ActivityLogResponse> recentActivity) { this.recentActivity = recentActivity; }
}
