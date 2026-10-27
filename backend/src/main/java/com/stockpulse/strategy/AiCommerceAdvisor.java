package com.stockpulse.strategy;

import com.stockpulse.ai.AiResponseValidator;
import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("AI")
public class AiCommerceAdvisor implements CommerceAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AiCommerceAdvisor.class);

    private final LLMGateway llmGateway;
    private final PromptBuilder promptBuilder;
    private final AiResponseValidator validator;
    private final RuleBasedCommerceAdvisor fallback;

    public AiCommerceAdvisor(LLMGateway llmGateway, PromptBuilder promptBuilder,
                              AiResponseValidator validator, RuleBasedCommerceAdvisor fallback) {
        this.llmGateway = llmGateway;
        this.promptBuilder = promptBuilder;
        this.validator = validator;
        this.fallback = fallback;
    }

    @Override
    public String getName() {
        return "AI";
    }

    /**
     * The gateway response contains both recommendation sections. Each contract
     * method validates and returns only the section requested by its caller.
     */
    @Override
    public PricingRecommendation recommendPricing(AdvisorContext ctx) {
        if (!llmGateway.isConfigured()) {
            log.warn("AI not configured — falling back to rule-based for pricing");
            return withFallbackNote(fallback.recommendPricing(ctx));
        }
        try {
            return fetchFromAi(ctx).pricing();
        } catch (Exception e) {
            log.error("AI pricing failed, falling back to rule-based", e);
            return withFallbackNote(fallback.recommendPricing(ctx));
        }
    }

    @Override
    public ReorderRecommendation recommendReorder(AdvisorContext ctx) {
        if (!llmGateway.isConfigured()) {
            log.warn("AI not configured — falling back to rule-based for reorder");
            return withFallbackNote(fallback.recommendReorder(ctx));
        }
        try {
            return fetchFromAi(ctx).reorder();
        } catch (Exception e) {
            log.error("AI reorder failed, falling back to rule-based", e);
            return withFallbackNote(fallback.recommendReorder(ctx));
        }
    }

    private AiResponseValidator.AiRecommendationPair fetchFromAi(AdvisorContext ctx) throws Exception {
        String prompt = promptBuilder.buildPrompt(ctx);
        String rawResponse = llmGateway.complete(prompt);
        log.debug("AI raw response received, validating...");
        return validator.parse(rawResponse, ctx);
    }

    private PricingRecommendation withFallbackNote(PricingRecommendation rec) {
        rec.setReasoning("[AI unavailable — rule-based fallback applied] " + rec.getReasoning());
        return rec;
    }

    private ReorderRecommendation withFallbackNote(ReorderRecommendation rec) {
        rec.setReasoning("[AI unavailable — rule-based fallback applied] " + rec.getReasoning());
        return rec;
    }
}
