package com.stockpulse.ai;

import com.stockpulse.enums.TriggerReason;
import com.stockpulse.strategy.AdvisorContext;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildPrompt(AdvisorContext ctx) {
        String productInfo = buildProductInfo(ctx);
        String triggerSection = buildTriggerSection(ctx);

        return """
            You are a commerce pricing and inventory advisor for an online retail store.
            
            PRODUCT CONTEXT:
            %s
            
            %s
            
            Respond ONLY with valid JSON in this exact structure:
            {
              "pricing": {
                "recommendedPrice": <number>,
                "direction": "<INCREASE|DECREASE|HOLD>",
                "confidence": <0.0-1.0>,
                "reasoning": "<explanation>"
              },
              "reorder": {
                "recommendedQuantity": <positive integer>,
                "suggestedLeadTimeDays": <1-90>,
                "confidence": <0.0-1.0>,
                "reasoning": "<explanation>"
              }
            }
            
            Constraints:
            - recommendedPrice must be between %.2f and %.2f (0.5x to 1.5x current price)
            - recommendedQuantity must be a positive integer
            - All reasoning fields must be non-empty
            - Return ONLY the JSON object, no markdown, no explanation outside JSON
            """.formatted(
                productInfo,
                triggerSection,
                ctx.getCurrentPrice().doubleValue() * 0.5,
                ctx.getCurrentPrice().doubleValue() * 1.5
            );
    }

    private String buildProductInfo(AdvisorContext ctx) {
        return """
            - Product: %s (SKU: %s)
            - Category: %s
            - Current Price: $%.2f
            - Stock Level: %d units
            - Reorder Threshold: %d units
            - Demand Velocity: %d orders in last 24h
            - Category Average Demand Velocity: %.1f orders/24h
            """.formatted(
                ctx.getProductName(), ctx.getSku(),
                ctx.getCategory(),
                ctx.getCurrentPrice().doubleValue(),
                ctx.getStockLevel(),
                ctx.getReorderThreshold(),
                ctx.getDemandVelocity(),
                ctx.getCategoryAvgDemandVelocity()
            );
    }

    private String buildTriggerSection(AdvisorContext ctx) {
        return switch (ctx.getTriggerReason()) {
            case INVENTORY_LOW -> """
                TRIGGER: INVENTORY LOW — MERCHANDISING DECISION REQUIRED
                Current stock: %d units. Reorder threshold: %d units. Stock is BELOW threshold.
                Demand velocity: %d orders/24h vs category average: %.1f orders/24h.

                This is a genuine merchandising tradeoff. You must weigh BOTH options and explain the tradeoff in your reasoning:

                OPTION A — Raise price: Protects remaining inventory, improves margin on each unit sold,
                slows sell-through to buy time for replenishment. Best when demand is inelastic.

                OPTION B — Clearance discount: Moves remaining units quickly, frees working capital,
                avoids stockout embarrassment. Best when demand is elastic or product is seasonal.

                In your reasoning field, explicitly state which option you recommend and WHY given the
                demand velocity vs category average above. Then recommend a reorder quantity to replenish.
                """.formatted(
                    ctx.getStockLevel(), ctx.getReorderThreshold(),
                    ctx.getDemandVelocity(), ctx.getCategoryAvgDemandVelocity());

            case DEMAND_SPIKE -> """
                TRIGGER: DEMAND SPIKE — VELOCITY SURGE DETECTED
                Current demand velocity: %d orders/24h. Category average: %.1f orders/24h.
                Velocity is %.1fx above category average. Current stock: %d units (threshold: %d).

                Evaluate ALL THREE risks and address each in your reasoning:

                RISK 1 — Overpricing: A price increase that is too aggressive will drive customers
                to competitors. Demand spikes can be temporary. Quantify how much increase is safe.

                RISK 2 — Underpricing: Failing to raise price during a spike leaves revenue on the
                table and may accelerate stockout.

                RISK 3 — Stockout: At current velocity (%d orders/24h) with %d units remaining,
                estimate days until stockout. If stockout is imminent, recommend urgent reorder.

                Recommend a modest, defensible price change and a reorder quantity that prevents stockout.
                """.formatted(
                    ctx.getDemandVelocity(), ctx.getCategoryAvgDemandVelocity(),
                    ctx.getCategoryAvgDemandVelocity() > 0 ? ctx.getDemandVelocity() / ctx.getCategoryAvgDemandVelocity() : 0,
                    ctx.getStockLevel(), ctx.getReorderThreshold(),
                    ctx.getDemandVelocity(), ctx.getStockLevel());

            case MANUAL -> """
                TRIGGER: MANUAL REVIEW — MERCHANDISER REQUESTED ANALYSIS
                No automatic signal fired. A merchandiser has requested a fresh assessment.

                Evaluate the product objectively using only the data above.
                Consider whether current price is appropriate given stock level and demand velocity.
                Consider whether a reorder is warranted given current stock vs threshold.
                Provide balanced recommendations with clear reasoning.
                """;

            default -> """
                TRIGGER: INITIAL ASSESSMENT
                Perform a baseline pricing and inventory assessment for this newly tracked product.
                Recommend whether the current price is appropriate and whether initial stock is sufficient.
                """;
        };
    }
}
