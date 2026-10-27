package com.stockpulse.strategy;

import com.stockpulse.enums.PriceDirection;
import com.stockpulse.enums.RecommendationSource;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("RULE_BASED")
public class RuleBasedCommerceAdvisor implements CommerceAdvisor {

    @Override
    public String getName() {
        return "RULE_BASED";
    }

    @Override
    public PricingRecommendation recommendPricing(AdvisorContext ctx) {
        PricingRecommendation rec = new PricingRecommendation();
        rec.setSource(RecommendationSource.RULE_BASED);

        BigDecimal current = ctx.getCurrentPrice();
        double avgVelocity = ctx.getCategoryAvgDemandVelocity();

        if (ctx.getStockLevel() < ctx.getReorderThreshold()) {
            BigDecimal recommended = current.multiply(BigDecimal.valueOf(1.10))
                    .setScale(2, RoundingMode.HALF_UP);
            rec.setRecommendedPrice(recommended);
            rec.setDirection(PriceDirection.INCREASE);
            rec.setConfidence(0.80);
            rec.setReasoning(String.format(
                "Stock level (%d) is below reorder threshold (%d). Increasing price by 10%% to protect limited inventory and improve margin on remaining units.",
                ctx.getStockLevel(), ctx.getReorderThreshold()));
        } else if (avgVelocity > 0 && ctx.getDemandVelocity() > 2 * avgVelocity) {
            BigDecimal recommended = current.multiply(BigDecimal.valueOf(1.05))
                    .setScale(2, RoundingMode.HALF_UP);
            rec.setRecommendedPrice(recommended);
            rec.setDirection(PriceDirection.INCREASE);
            rec.setConfidence(0.70);
            rec.setReasoning(String.format(
                "Demand velocity (%d orders/24h) exceeds twice the category average (%.1f). Applying a 5%% price increase to capture demand premium.",
                ctx.getDemandVelocity(), avgVelocity));
        } else {
            rec.setRecommendedPrice(current.setScale(2, RoundingMode.HALF_UP));
            rec.setDirection(PriceDirection.HOLD);
            rec.setConfidence(0.90);
            rec.setReasoning(String.format(
                "Stock level (%d) is adequate and demand velocity (%d) is within normal range. Holding current price.",
                ctx.getStockLevel(), ctx.getDemandVelocity()));
        }
        return rec;
    }

    @Override
    public ReorderRecommendation recommendReorder(AdvisorContext ctx) {
        ReorderRecommendation rec = new ReorderRecommendation();
        rec.setSource(RecommendationSource.RULE_BASED);

        int qty = Math.max(1, ctx.getReorderThreshold() * 3 - ctx.getStockLevel());
        rec.setRecommendedQuantity(qty);
        rec.setSuggestedLeadTimeDays(7);
        rec.setConfidence(0.85);
        rec.setReasoning(String.format(
            "Calculated reorder quantity: max(1, threshold(%d) * 3 - stock(%d)) = %d units. Standard 7-day lead time applied.",
            ctx.getReorderThreshold(), ctx.getStockLevel(), qty));
        return rec;
    }
}
