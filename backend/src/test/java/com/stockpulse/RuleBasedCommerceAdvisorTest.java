package com.stockpulse;

import com.stockpulse.enums.PriceDirection;
import com.stockpulse.enums.ProductCategory;
import com.stockpulse.enums.TriggerReason;
import com.stockpulse.strategy.AdvisorContext;
import com.stockpulse.strategy.PricingRecommendation;
import com.stockpulse.strategy.ReorderRecommendation;
import com.stockpulse.strategy.RuleBasedCommerceAdvisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedCommerceAdvisorTest {

    private RuleBasedCommerceAdvisor advisor;

    @BeforeEach
    void setUp() {
        advisor = new RuleBasedCommerceAdvisor();
    }

    private AdvisorContext ctx(int stock, int threshold, int velocity, double avgVelocity) {
        AdvisorContext ctx = new AdvisorContext();
        ctx.setProductId(1L);
        ctx.setProductName("Test Product");
        ctx.setSku("TST-001");
        ctx.setCategory(ProductCategory.ELECTRONICS);
        ctx.setCurrentPrice(new BigDecimal("100.00"));
        ctx.setStockLevel(stock);
        ctx.setReorderThreshold(threshold);
        ctx.setDemandVelocity(velocity);
        ctx.setCategoryAvgDemandVelocity(avgVelocity);
        ctx.setTriggerReason(TriggerReason.MANUAL);
        return ctx;
    }

    @Test
    void lowStock_recommendsPriceIncrease() {
        AdvisorContext ctx = ctx(5, 20, 3, 5.0);
        PricingRecommendation rec = advisor.recommendPricing(ctx);
        assertThat(rec.getDirection()).isEqualTo(PriceDirection.INCREASE);
        assertThat(rec.getRecommendedPrice()).isEqualByComparingTo("110.00");
        assertThat(rec.getConfidence()).isEqualTo(0.80);
    }

    @Test
    void highDemand_recommendsPriceIncrease() {
        AdvisorContext ctx = ctx(50, 20, 25, 5.0); // 25 > 2*5
        PricingRecommendation rec = advisor.recommendPricing(ctx);
        assertThat(rec.getDirection()).isEqualTo(PriceDirection.INCREASE);
        assertThat(rec.getRecommendedPrice()).isEqualByComparingTo("105.00");
        assertThat(rec.getConfidence()).isEqualTo(0.70);
    }

    @Test
    void normalDemand_recommendsHold() {
        AdvisorContext ctx = ctx(50, 20, 5, 5.0); // 5 not > 2*5
        PricingRecommendation rec = advisor.recommendPricing(ctx);
        assertThat(rec.getDirection()).isEqualTo(PriceDirection.HOLD);
        assertThat(rec.getRecommendedPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void zeroCategoryAverage_doesNotTriggerDemandSpike() {
        AdvisorContext ctx = ctx(50, 20, 10, 0.0);
        PricingRecommendation rec = advisor.recommendPricing(ctx);
        assertThat(rec.getDirection()).isEqualTo(PriceDirection.HOLD);
    }

    @Test
    void zeroStock_recommendsPriceIncrease() {
        AdvisorContext ctx = ctx(0, 20, 0, 5.0);
        PricingRecommendation rec = advisor.recommendPricing(ctx);
        assertThat(rec.getDirection()).isEqualTo(PriceDirection.INCREASE);
    }

    @Test
    void reorderQuantity_calculatedCorrectly() {
        AdvisorContext ctx = ctx(10, 20, 5, 5.0);
        ReorderRecommendation rec = advisor.recommendReorder(ctx);
        // max(1, 20*3 - 10) = max(1, 50) = 50
        assertThat(rec.getRecommendedQuantity()).isEqualTo(50);
        assertThat(rec.getSuggestedLeadTimeDays()).isEqualTo(7);
    }

    @Test
    void reorderQuantity_minimumOne() {
        AdvisorContext ctx = ctx(1000, 5, 0, 0.0);
        ReorderRecommendation rec = advisor.recommendReorder(ctx);
        // max(1, 5*3 - 1000) = max(1, -985) = 1
        assertThat(rec.getRecommendedQuantity()).isEqualTo(1);
    }
}
