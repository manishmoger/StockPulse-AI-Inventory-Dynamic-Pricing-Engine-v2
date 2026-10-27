package com.stockpulse.service;

import com.stockpulse.enums.TriggerReason;
import com.stockpulse.event.DemandSpikeEvent;
import com.stockpulse.event.InventoryLowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class RecommendationEventListener {

    private static final Logger log = LoggerFactory.getLogger(RecommendationEventListener.class);

    private final RecommendationService recommendationService;

    public RecommendationEventListener(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInventoryLow(InventoryLowEvent event) {
        handleEvent(event.getProductId(), event.getTriggerReason(), "InventoryLowEvent");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDemandSpike(DemandSpikeEvent event) {
        handleEvent(event.getProductId(), TriggerReason.DEMAND_SPIKE, "DemandSpikeEvent");
    }

    private void handleEvent(Long productId, TriggerReason trigger, String eventName) {
        log.info("Handling {} for product {}", eventName, productId);
        try {
            recommendationService.generatePricingSuggestion(productId, trigger);
            recommendationService.generateReorderSuggestion(productId, trigger);
        } catch (Exception e) {
            log.error("Error handling {} for product {}", eventName, productId, e);
        }
    }
}
