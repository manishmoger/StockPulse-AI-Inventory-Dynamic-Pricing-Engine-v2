package com.stockpulse.controller;

import com.stockpulse.dto.*;
import com.stockpulse.enums.ProductCategory;
import com.stockpulse.enums.ProductStatus;
import com.stockpulse.enums.TriggerReason;
import com.stockpulse.service.ProductService;
import com.stockpulse.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final RecommendationService recommendationService;

    public ProductController(ProductService productService, RecommendationService recommendationService) {
        this.productService = productService;
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(req));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.getProducts(status, category, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable Long id,
                                                        @Valid @RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(productService.updateStock(id, req.getStockLevel()));
    }

    @PostMapping("/{id}/orders")
    public ResponseEntity<ProductResponse> simulateOrder(@PathVariable Long id,
                                                          @Valid @RequestBody OrderRequest req) {
        return ResponseEntity.ok(productService.simulateOrder(id, req.getQuantity()));
    }

    @PostMapping("/{id}/suggest-pricing")
    public ResponseEntity<PricingSuggestionResponse> suggestPricing(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recommendationService.generatePricingSuggestion(id, TriggerReason.MANUAL));
    }

    @PostMapping("/{id}/suggest-reorder")
    public ResponseEntity<ReorderSuggestionResponse> suggestReorder(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recommendationService.generateReorderSuggestion(id, TriggerReason.MANUAL));
    }
}
