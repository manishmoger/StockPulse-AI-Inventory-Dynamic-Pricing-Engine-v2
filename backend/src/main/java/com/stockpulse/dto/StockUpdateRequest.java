package com.stockpulse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockUpdateRequest {
    @NotNull(message = "Stock level is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockLevel;

    public Integer getStockLevel() { return stockLevel; }
    public void setStockLevel(Integer stockLevel) { this.stockLevel = stockLevel; }
}
