package com.stockpulse;

import com.stockpulse.ai.AiResponseValidator;
import com.stockpulse.enums.PriceDirection;
import com.stockpulse.enums.ProductCategory;
import com.stockpulse.enums.TriggerReason;
import com.stockpulse.strategy.AdvisorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiResponseValidatorTest {

    private AiResponseValidator validator;
    private AdvisorContext ctx;

    @BeforeEach
    void setUp() {
        validator = new AiResponseValidator();
        ctx = new AdvisorContext();
        ctx.setProductId(1L);
        ctx.setProductName("Test Product");
        ctx.setSku("TST-001");
        ctx.setCategory(ProductCategory.ELECTRONICS);
        ctx.setCurrentPrice(new BigDecimal("100.00"));
        ctx.setStockLevel(10);
        ctx.setReorderThreshold(20);
        ctx.setDemandVelocity(5);
        ctx.setCategoryAvgDemandVelocity(3.0);
        ctx.setTriggerReason(TriggerReason.MANUAL);
    }

    @Test
    void validResponse_parsesCorrectly() throws Exception {
        String json = """
            {
              "pricing": {
                "recommendedPrice": 110.00,
                "direction": "INCREASE",
                "confidence": 0.85,
                "reasoning": "Demand is high, increase price."
              },
              "reorder": {
                "recommendedQuantity": 50,
                "suggestedLeadTimeDays": 7,
                "confidence": 0.80,
                "reasoning": "Stock is low, reorder needed."
              }
            }
            """;
        var pair = validator.parse(json, ctx);
        assertThat(pair.pricing().getRecommendedPrice()).isEqualByComparingTo("110.00");
        assertThat(pair.pricing().getDirection()).isEqualTo(PriceDirection.INCREASE);
        assertThat(pair.pricing().getConfidence()).isEqualTo(0.85);
        assertThat(pair.reorder().getRecommendedQuantity()).isEqualTo(50);
        assertThat(pair.reorder().getSuggestedLeadTimeDays()).isEqualTo(7);
    }

    @Test
    void markdownFences_areStripped() throws Exception {
        String json = """
            ```json
            {
              "pricing": {"recommendedPrice": 105.00, "direction": "INCREASE", "confidence": 0.75, "reasoning": "ok"},
              "reorder": {"recommendedQuantity": 30, "suggestedLeadTimeDays": 5, "confidence": 0.70, "reasoning": "ok"}
            }
            ```
            """;
        var pair = validator.parse(json, ctx);
        assertThat(pair.pricing().getRecommendedPrice()).isEqualByComparingTo("105.00");
    }

    @Test
    void absurdPrice_tooHigh_throwsException() {
        String json = """
            {
              "pricing": {"recommendedPrice": 999999.00, "direction": "INCREASE", "confidence": 0.9, "reasoning": "ok"},
              "reorder": {"recommendedQuantity": 10, "suggestedLeadTimeDays": 7, "confidence": 0.8, "reasoning": "ok"}
            }
            """;
        assertThatThrownBy(() -> validator.parse(json, ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside safe bounds");
    }

    @Test
    void absurdPrice_zero_throwsException() {
        String json = """
            {
              "pricing": {"recommendedPrice": 0, "direction": "DECREASE", "confidence": 0.9, "reasoning": "ok"},
              "reorder": {"recommendedQuantity": 10, "suggestedLeadTimeDays": 7, "confidence": 0.8, "reasoning": "ok"}
            }
            """;
        assertThatThrownBy(() -> validator.parse(json, ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void negativeReorderQuantity_throwsException() {
        String json = """
            {
              "pricing": {"recommendedPrice": 105.00, "direction": "INCREASE", "confidence": 0.8, "reasoning": "ok"},
              "reorder": {"recommendedQuantity": -5, "suggestedLeadTimeDays": 7, "confidence": 0.8, "reasoning": "ok"}
            }
            """;
        assertThatThrownBy(() -> validator.parse(json, ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive integer");
    }

    @Test
    void emptyJson_throwsException() {
        assertThatThrownBy(() -> validator.parse("", ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No JSON");
    }

    @Test
    void missingPricingField_throwsException() {
        String json = """
            {
              "reorder": {"recommendedQuantity": 10, "suggestedLeadTimeDays": 7, "confidence": 0.8, "reasoning": "ok"}
            }
            """;
        assertThatThrownBy(() -> validator.parse(json, ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pricing");
    }

    @Test
    void directionInconsistentWithPrice_isCorrectedToHold() throws Exception {
        // AI says INCREASE but price is actually lower — should be corrected to HOLD
        String json = """
            {
              "pricing": {"recommendedPrice": 95.00, "direction": "INCREASE", "confidence": 0.7, "reasoning": "ok"},
              "reorder": {"recommendedQuantity": 10, "suggestedLeadTimeDays": 7, "confidence": 0.8, "reasoning": "ok"}
            }
            """;
        var pair = validator.parse(json, ctx);
        assertThat(pair.pricing().getDirection()).isEqualTo(PriceDirection.HOLD);
    }

    @Test
    void confidenceOutOfRange_throwsException() {
        String json = """
            {
              "pricing": {"recommendedPrice": 105.00, "direction": "INCREASE", "confidence": 1.5, "reasoning": "ok"},
              "reorder": {"recommendedQuantity": 10, "suggestedLeadTimeDays": 7, "confidence": 0.8, "reasoning": "ok"}
            }
            """;
        assertThatThrownBy(() -> validator.parse(json, ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confidence");
    }

    @Test
    void unparsableJson_throwsException() {
        assertThatThrownBy(() -> validator.parse("this is not json at all", ctx))
                .isInstanceOf(Exception.class);
    }
}
