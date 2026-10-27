package com.stockpulse.event;

import org.springframework.context.ApplicationEvent;

public class DemandSpikeEvent extends ApplicationEvent {
    private final Long productId;

    public DemandSpikeEvent(Object source, Long productId) {
        super(source);
        this.productId = productId;
    }

    public Long getProductId() { return productId; }
}
