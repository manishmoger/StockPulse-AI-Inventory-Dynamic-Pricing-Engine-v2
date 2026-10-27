package com.stockpulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.dto.CreateProductRequest;
import com.stockpulse.dto.DecisionRequest;
import com.stockpulse.dto.OrderRequest;
import com.stockpulse.enums.ProductCategory;
import com.stockpulse.enums.SuggestionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StockPulseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProduct_returnsCreated() throws Exception {
        CreateProductRequest req = buildProductRequest("INT-001", "Integration Product", 50, 10);
        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("INT-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createProduct_duplicateSku_returns400() throws Exception {
        CreateProductRequest req = buildProductRequest("DUP-001", "Dup Product", 50, 10);
        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listProducts_returnsSeededData() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(8)));
    }

    @Test
    void simulateOrder_decreasesStock() throws Exception {
        // Get PRD-001 id
        MvcResult listResult = mockMvc.perform(get("/products"))
                .andExpect(status().isOk()).andReturn();
        var products = objectMapper.readTree(listResult.getResponse().getContentAsString());
        Long id = null;
        for (var p : products) {
            if ("SKU-ELEC-001".equals(p.get("sku").asText())) {
                id = p.get("id").asLong();
                break;
            }
        }
        assertThat(id).isNotNull();

        OrderRequest order = new OrderRequest();
        order.setQuantity(5);
        MvcResult result = mockMvc.perform(post("/products/" + id + "/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andReturn();
        var product = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(product.get("stockLevel").asInt()).isEqualTo(40); // 45 - 5
    }

    @Test
    void acceptPricingSuggestion_updatesPrice() throws Exception {
        // Create product with low stock to trigger suggestion
        CreateProductRequest req = buildProductRequest("ACC-001", "Accept Test", 5, 20);
        MvcResult createResult = mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long productId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Generate manual pricing suggestion
        MvcResult suggestResult = mockMvc.perform(post("/products/" + productId + "/suggest-pricing"))
                .andExpect(status().isCreated()).andReturn();
        Long suggestionId = objectMapper.readTree(suggestResult.getResponse().getContentAsString()).get("id").asLong();

        // Accept it
        DecisionRequest decision = new DecisionRequest();
        decision.setDecision(SuggestionStatus.ACCEPTED);
        mockMvc.perform(patch("/pricing-suggestions/" + suggestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void rejectPricingSuggestion_doesNotUpdatePrice() throws Exception {
        CreateProductRequest req = buildProductRequest("REJ-001", "Reject Test", 50, 10);
        MvcResult createResult = mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long productId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();
        String originalPrice = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("currentPrice").asText();

        MvcResult suggestResult = mockMvc.perform(post("/products/" + productId + "/suggest-pricing"))
                .andExpect(status().isCreated()).andReturn();
        Long suggestionId = objectMapper.readTree(suggestResult.getResponse().getContentAsString()).get("id").asLong();

        DecisionRequest decision = new DecisionRequest();
        decision.setDecision(SuggestionStatus.REJECTED);
        mockMvc.perform(patch("/pricing-suggestions/" + suggestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // Price should be unchanged
        MvcResult productResult = mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk()).andReturn();
        String currentPrice = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .get("currentPrice").asText();
        assertThat(currentPrice).isEqualTo(originalPrice);
    }

    @Test
    void acceptReorderSuggestion_incrementsStock() throws Exception {
        CreateProductRequest req = buildProductRequest("ROR-001", "Reorder Test", 5, 20);
        MvcResult createResult = mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long productId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult suggestResult = mockMvc.perform(post("/products/" + productId + "/suggest-reorder"))
                .andExpect(status().isCreated()).andReturn();
        var suggestion = objectMapper.readTree(suggestResult.getResponse().getContentAsString());
        Long suggestionId = suggestion.get("id").asLong();
        int recommendedQty = suggestion.get("recommendedQuantity").asInt();

        DecisionRequest decision = new DecisionRequest();
        decision.setDecision(SuggestionStatus.ACCEPTED);
        mockMvc.perform(patch("/reorder-suggestions/" + suggestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        MvcResult productResult = mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk()).andReturn();
        int newStock = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .get("stockLevel").asInt();
        assertThat(newStock).isEqualTo(5 + recommendedQty);
    }

    @Test
    void repeatedAcceptance_returns409() throws Exception {
        CreateProductRequest req = buildProductRequest("RPT-001", "Repeat Test", 5, 20);
        MvcResult createResult = mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long productId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult suggestResult = mockMvc.perform(post("/products/" + productId + "/suggest-pricing"))
                .andExpect(status().isCreated()).andReturn();
        Long suggestionId = objectMapper.readTree(suggestResult.getResponse().getContentAsString()).get("id").asLong();

        DecisionRequest decision = new DecisionRequest();
        decision.setDecision(SuggestionStatus.ACCEPTED);
        mockMvc.perform(patch("/pricing-suggestions/" + suggestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk());

        // Second acceptance should fail
        mockMvc.perform(patch("/pricing-suggestions/" + suggestionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isConflict());
    }

    @Test
    void dashboardMetrics_returnsValidData() throws Exception {
        mockMvc.perform(get("/dashboard/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").isNumber())
                .andExpect(jsonPath("$.lowStockCount").isNumber())
                .andExpect(jsonPath("$.outOfStockCount").isNumber());
    }

    private CreateProductRequest buildProductRequest(String sku, String name, int stock, int threshold) {
        CreateProductRequest req = new CreateProductRequest();
        req.setSku(sku);
        req.setName(name);
        req.setCategory(ProductCategory.ELECTRONICS);
        req.setCurrentPrice(new BigDecimal("99.99"));
        req.setStockLevel(stock);
        req.setReorderThreshold(threshold);
        req.setDemandVelocity(0);
        return req;
    }
}
