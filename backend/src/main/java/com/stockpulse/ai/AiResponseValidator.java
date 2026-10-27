package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.strategy.AdvisorContext;
import com.stockpulse.strategy.PricingRecommendation;
import com.stockpulse.strategy.ReorderRecommendation;
import com.stockpulse.enums.PriceDirection;
import com.stockpulse.enums.RecommendationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component
public class AiResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(AiResponseValidator.class);
    private static final double MIN_PRICE_RATIO = 0.5;
    private static final double MAX_PRICE_RATIO = 1.5;
    private static final int MAX_LEAD_TIME = 90;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiRecommendationPair parse(String rawResponse, AdvisorContext ctx) throws Exception {
        String cleaned = extractJson(rawResponse);
        JsonNode root = objectMapper.readTree(cleaned);

        PricingRecommendation pricing = parsePricing(root.path("pricing"), ctx);
        ReorderRecommendation reorder = parseReorder(root.path("reorder"), ctx);

        return new AiRecommendationPair(pricing, reorder);
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start == -1 || end == -1) throw new IllegalArgumentException("No JSON object found in AI response");
        return raw.substring(start, end + 1);
    }

    private PricingRecommendation parsePricing(JsonNode node, AdvisorContext ctx) {
        if (node.isMissingNode()) throw new IllegalArgumentException("Missing 'pricing' field in AI response");

        double rawPrice = node.path("recommendedPrice").asDouble(-1);
        if (rawPrice <= 0) throw new IllegalArgumentException("AI pricing: recommendedPrice must be positive");

        BigDecimal current = ctx.getCurrentPrice();
        BigDecimal recommended = BigDecimal.valueOf(rawPrice).setScale(2, RoundingMode.HALF_UP);

        double ratio = recommended.doubleValue() / current.doubleValue();
        if (ratio < MIN_PRICE_RATIO || ratio > MAX_PRICE_RATIO) {
            throw new IllegalArgumentException(String.format(
                "AI pricing: recommendedPrice %.2f is outside safe bounds [%.2f, %.2f] relative to current %.2f",
                rawPrice, current.doubleValue() * MIN_PRICE_RATIO, current.doubleValue() * MAX_PRICE_RATIO, current.doubleValue()));
        }

        String directionStr = node.path("direction").asText("");
        PriceDirection direction;
        try {
            direction = PriceDirection.valueOf(directionStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("AI pricing: invalid direction: " + directionStr, e);
        }

        // Validate direction matches price comparison
        int cmp = recommended.compareTo(current);
        if (direction == PriceDirection.INCREASE && cmp <= 0) direction = PriceDirection.HOLD;
        if (direction == PriceDirection.DECREASE && cmp >= 0) direction = PriceDirection.HOLD;

        double confidence = node.path("confidence").asDouble(-1);
        if (confidence < 0 || confidence > 1) throw new IllegalArgumentException("AI pricing: confidence must be 0-1");

        String reasoning = node.path("reasoning").asText("").trim();
        if (reasoning.isEmpty()) throw new IllegalArgumentException("AI pricing: reasoning is required");

        PricingRecommendation rec = new PricingRecommendation();
        rec.setRecommendedPrice(recommended);
        rec.setDirection(direction);
        rec.setConfidence(confidence);
        rec.setReasoning(reasoning);
        rec.setSource(RecommendationSource.AI);
        return rec;
    }

    private ReorderRecommendation parseReorder(JsonNode node, AdvisorContext ctx) {
        if (node.isMissingNode()) throw new IllegalArgumentException("Missing 'reorder' field in AI response");

        int qty = node.path("recommendedQuantity").asInt(-1);
        if (qty < 1) throw new IllegalArgumentException("AI reorder: recommendedQuantity must be positive integer");

        int leadTime = node.path("suggestedLeadTimeDays").asInt(-1);
        if (leadTime < 1 || leadTime > MAX_LEAD_TIME) throw new IllegalArgumentException("AI reorder: invalid lead time: " + leadTime);

        double confidence = node.path("confidence").asDouble(-1);
        if (confidence < 0 || confidence > 1) throw new IllegalArgumentException("AI reorder: confidence must be 0-1");

        String reasoning = node.path("reorder").isMissingNode() ? "" : node.path("reasoning").asText("").trim();
        reasoning = node.path("reasoning").asText("").trim();
        if (reasoning.isEmpty()) throw new IllegalArgumentException("AI reorder: reasoning is required");

        ReorderRecommendation rec = new ReorderRecommendation();
        rec.setRecommendedQuantity(qty);
        rec.setSuggestedLeadTimeDays(leadTime);
        rec.setConfidence(confidence);
        rec.setReasoning(reasoning);
        rec.setSource(RecommendationSource.AI);
        return rec;
    }

    public record AiRecommendationPair(PricingRecommendation pricing, ReorderRecommendation reorder) {}
}
