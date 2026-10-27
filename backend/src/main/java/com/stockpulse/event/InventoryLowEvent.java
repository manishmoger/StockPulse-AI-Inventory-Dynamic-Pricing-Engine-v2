package com.stockpulse.event;

import com.stockpulse.enums.TriggerReason;
import org.springframework.context.ApplicationEvent;

public class InventoryLowEvent extends ApplicationEvent {
    private final Long productId;
    private final TriggerReason triggerReason;

    public InventoryLowEvent(Object source, Long productId, TriggerReason triggerReason) {
        super(source);
        this.productId = productId;
        this.triggerReason = triggerReason;
    }

    public Long getProductId() { return productId; }
    public TriggerReason getTriggerReason() { return triggerReason; }
}
