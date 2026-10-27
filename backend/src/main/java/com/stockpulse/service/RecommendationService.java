package com.stockpulse.service;

import com.stockpulse.config.StrategyConfig;
import com.stockpulse.dto.PricingSuggestionResponse;
import com.stockpulse.dto.ReorderSuggestionResponse;
import com.stockpulse.entity.PricingSuggestion;
import com.stockpulse.entity.Product;
import com.stockpulse.entity.ReorderSuggestion;
import com.stockpulse.enums.ActivityType;
import com.stockpulse.enums.SuggestionStatus;
import com.stockpulse.enums.TriggerReason;
import com.stockpulse.exception.ResourceNotFoundException;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.strategy.AdvisorContext;
import com.stockpulse.strategy.CommerceAdvisor;
import com.stockpulse.strategy.PricingRecommendation;
import com.stockpulse.strategy.ReorderRecommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final ProductService productService;
    private final ActivityService activityService;
    private final StrategyConfig strategyConfig;

    public RecommendationService(PricingSuggestionRepository pricingRepo,
                                  ReorderSuggestionRepository reorderRepo,
                                  ProductService productService,
                                  ActivityService activityService,
                                  StrategyConfig strategyConfig) {
        this.pricingRepo = pricingRepo;
        this.reorderRepo = reorderRepo;
        this.productService = productService;
        this.activityService = activityService;
        this.strategyConfig = strategyConfig;
    }

    @Transactional
    public PricingSuggestionResponse generatePricingSuggestion(Long productId, TriggerReason trigger) {
        Product p = productService.findByIdForUpdate(productId);
        // Idempotency: check + insert in the same transaction to prevent duplicate PENDING suggestions
        if (pricingRepo.existsPendingForProductAndTrigger(productId, trigger)) {
            log.info("Skipping duplicate PENDING pricing suggestion for product {} trigger {}", productId, trigger);
            return pricingRepo.findFirstPendingForProductAndTrigger(productId, trigger)
                    .map(PricingSuggestionResponse::from)
                    .orElseThrow();
        }
        AdvisorContext ctx = buildContext(p, trigger);
        CommerceAdvisor advisor = strategyConfig.getActiveAdvisor();
        PricingRecommendation rec = advisor.recommendPricing(ctx);

        PricingSuggestion suggestion = new PricingSuggestion();
        suggestion.setProduct(p);
        suggestion.setCurrentPrice(p.getCurrentPrice());
        suggestion.setRecommendedPrice(rec.getRecommendedPrice());
        suggestion.setDirection(rec.getDirection());
        suggestion.setConfidence(rec.getConfidence());
        suggestion.setReasoning(rec.getReasoning());
        suggestion.setTriggerReason(trigger);
        suggestion.setSource(rec.getSource());
        suggestion.setStatus(SuggestionStatus.PENDING);

        PricingSuggestion saved = pricingRepo.save(suggestion);
        // State machine: ACTIVE → PRICE_REVIEW_PENDING when a suggestion is queued
        productService.markPriceReviewPending(p.getId());
        activityService.log(ActivityType.PRICING_RECOMMENDATION_GENERATED, p.getSku(), p.getName(),
                "Pricing suggestion generated: " + rec.getDirection() + " to " + rec.getRecommendedPrice()
                        + " [" + rec.getSource() + "]");
        return PricingSuggestionResponse.from(saved);
    }

    @Transactional
    public ReorderSuggestionResponse generateReorderSuggestion(Long productId, TriggerReason trigger) {
        Product p = productService.findByIdForUpdate(productId);
        // Idempotency: check + insert in the same transaction to prevent duplicate PENDING suggestions
        if (reorderRepo.existsPendingForProductAndTrigger(productId, trigger)) {
            log.info("Skipping duplicate PENDING reorder suggestion for product {} trigger {}", productId, trigger);
            return reorderRepo.findFirstPendingForProductAndTrigger(productId, trigger)
                    .map(ReorderSuggestionResponse::from)
                    .orElseThrow();
        }
        AdvisorContext ctx = buildContext(p, trigger);
        CommerceAdvisor advisor = strategyConfig.getActiveAdvisor();
        ReorderRecommendation rec = advisor.recommendReorder(ctx);

        ReorderSuggestion suggestion = new ReorderSuggestion();
        suggestion.setProduct(p);
        suggestion.setCurrentStock(p.getStockLevel());
        suggestion.setRecommendedQuantity(rec.getRecommendedQuantity());
        suggestion.setSuggestedLeadTimeDays(rec.getSuggestedLeadTimeDays());
        suggestion.setConfidence(rec.getConfidence());
        suggestion.setReasoning(rec.getReasoning());
        suggestion.setTriggerReason(trigger);
        suggestion.setSource(rec.getSource());
        suggestion.setStatus(SuggestionStatus.PENDING);

        ReorderSuggestion saved = reorderRepo.save(suggestion);
        activityService.log(ActivityType.REORDER_RECOMMENDATION_GENERATED, p.getSku(), p.getName(),
                "Reorder suggestion generated: qty=" + rec.getRecommendedQuantity()
                        + ", leadTime=" + rec.getSuggestedLeadTimeDays() + "d [" + rec.getSource() + "]");
        return ReorderSuggestionResponse.from(saved);
    }

    @Transactional
    public PricingSuggestionResponse decidePricing(Long suggestionId, SuggestionStatus decision) {
        PricingSuggestion s = pricingRepo.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing suggestion not found: " + suggestionId));

        if (s.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion " + suggestionId + " is already " + s.getStatus());
        }
        if (decision != SuggestionStatus.ACCEPTED && decision != SuggestionStatus.REJECTED) {
            throw new IllegalArgumentException("Decision must be ACCEPTED or REJECTED");
        }

        s.setStatus(decision);
        s.setDecidedAt(LocalDateTime.now());

        if (decision == SuggestionStatus.ACCEPTED) {
            Product p = s.getProduct();
            p.setCurrentPrice(s.getRecommendedPrice());
            // State machine: PRICE_REVIEW_PENDING → ACTIVE on accept (unless OUT_OF_STOCK)
            if (p.getStatus() != com.stockpulse.enums.ProductStatus.OUT_OF_STOCK) {
                p.setStatus(com.stockpulse.enums.ProductStatus.ACTIVE);
            }
            pricingRepo.save(s);
            activityService.log(ActivityType.RECOMMENDATION_ACCEPTED, p.getSku(), p.getName(),
                    "Pricing accepted: new price=" + s.getRecommendedPrice());
        } else {
            Product p = s.getProduct();
            pricingRepo.save(s);
            activityService.log(ActivityType.RECOMMENDATION_REJECTED, p.getSku(), p.getName(),
                    "Pricing rejected: recommended price=" + s.getRecommendedPrice() + " not applied");
        }
        return PricingSuggestionResponse.from(s);
    }

    @Transactional
    public ReorderSuggestionResponse decideReorder(Long suggestionId, SuggestionStatus decision) {
        ReorderSuggestion s = reorderRepo.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder suggestion not found: " + suggestionId));

        if (s.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion " + suggestionId + " is already " + s.getStatus());
        }
        if (decision != SuggestionStatus.ACCEPTED && decision != SuggestionStatus.REJECTED) {
            throw new IllegalArgumentException("Decision must be ACCEPTED or REJECTED");
        }

        s.setStatus(decision);
        s.setDecidedAt(LocalDateTime.now());

        if (decision == SuggestionStatus.ACCEPTED) {
            Product p = s.getProduct();
            p.setStockLevel(p.getStockLevel() + s.getRecommendedQuantity());
            if (p.getStockLevel() > 0 && p.getStatus() == com.stockpulse.enums.ProductStatus.OUT_OF_STOCK) {
                p.setStatus(com.stockpulse.enums.ProductStatus.ACTIVE);
            }
            reorderRepo.save(s);
            activityService.log(ActivityType.RECOMMENDATION_ACCEPTED, p.getSku(), p.getName(),
                    "Reorder accepted: added " + s.getRecommendedQuantity() + " units, new stock=" + p.getStockLevel());
        } else {
            reorderRepo.save(s);
            Product p = s.getProduct();
            activityService.log(ActivityType.RECOMMENDATION_REJECTED, p.getSku(), p.getName(),
                    "Reorder rejected: qty=" + s.getRecommendedQuantity() + " not applied");
        }
        return ReorderSuggestionResponse.from(s);
    }

    @Transactional(readOnly = true)
    public List<PricingSuggestionResponse> getPricingSuggestions(SuggestionStatus status) {
        List<PricingSuggestion> list = status != null
                ? pricingRepo.findByStatus(status)
                : pricingRepo.findAll();
        return list.stream().map(PricingSuggestionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestionResponse> getReorderSuggestions(SuggestionStatus status) {
        List<ReorderSuggestion> list = status != null
                ? reorderRepo.findByStatus(status)
                : reorderRepo.findAll();
        return list.stream().map(ReorderSuggestionResponse::from).toList();
    }

    public boolean hasPendingPricingSuggestion(Long productId, TriggerReason trigger) {
        return pricingRepo.existsPendingForProductAndTrigger(productId, trigger);
    }

    public boolean hasPendingReorderSuggestion(Long productId, TriggerReason trigger) {
        return reorderRepo.existsPendingForProductAndTrigger(productId, trigger);
    }

    private AdvisorContext buildContext(Product p, TriggerReason trigger) {
        AdvisorContext ctx = new AdvisorContext();
        ctx.setProductId(p.getId());
        ctx.setProductName(p.getName());
        ctx.setSku(p.getSku());
        ctx.setCategory(p.getCategory());
        ctx.setCurrentPrice(p.getCurrentPrice());
        ctx.setCostPrice(p.getCostPrice());
        ctx.setStockLevel(p.getStockLevel());
        ctx.setReorderThreshold(p.getReorderThreshold());
        ctx.setDemandVelocity(p.getDemandVelocity());
        ctx.setCategoryAvgDemandVelocity(productService.getCategoryAvgVelocity(p.getCategory()));
        ctx.setTriggerReason(trigger);
        return ctx;
    }
}
