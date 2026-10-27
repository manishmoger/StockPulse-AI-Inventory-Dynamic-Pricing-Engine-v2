package com.stockpulse.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable point-in-time record of a product's stock and demand velocity.
 * Written on every stock change and order simulation.
 * Sprint-2 seam: feed into a price-history chart or trend analysis.
 */
@Entity
@Table(name = "inventory_snapshots")
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer stockLevel;

    @Column(nullable = false)
    private Integer demandVelocity;

    @Column(nullable = false)
    private String triggerEvent; // e.g. "ORDER_SIMULATED", "STOCK_UPDATED"

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }

    public static InventorySnapshot of(Product p, String event) {
        InventorySnapshot s = new InventorySnapshot();
        s.product = p;
        s.stockLevel = p.getStockLevel();
        s.demandVelocity = p.getDemandVelocity();
        s.triggerEvent = event;
        return s;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Integer getStockLevel() { return stockLevel; }
    public Integer getDemandVelocity() { return demandVelocity; }
    public String getTriggerEvent() { return triggerEvent; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
