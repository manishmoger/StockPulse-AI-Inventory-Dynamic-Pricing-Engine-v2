package com.stockpulse.controller;

import com.stockpulse.dto.DecisionRequest;
import com.stockpulse.dto.PricingSuggestionResponse;
import com.stockpulse.dto.ReorderSuggestionResponse;
import com.stockpulse.enums.SuggestionStatus;
import com.stockpulse.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/pricing-suggestions")
    public ResponseEntity<List<PricingSuggestionResponse>> getPricingSuggestions(
            @RequestParam(required = false) SuggestionStatus status) {
        return ResponseEntity.ok(recommendationService.getPricingSuggestions(status));
    }

    @PatchMapping("/pricing-suggestions/{id}")
    public ResponseEntity<PricingSuggestionResponse> decidePricing(@PathVariable Long id,
                                                                     @Valid @RequestBody DecisionRequest req) {
        return ResponseEntity.ok(recommendationService.decidePricing(id, req.getDecision()));
    }

    @GetMapping("/reorder-suggestions")
    public ResponseEntity<List<ReorderSuggestionResponse>> getReorderSuggestions(
            @RequestParam(required = false) SuggestionStatus status) {
        return ResponseEntity.ok(recommendationService.getReorderSuggestions(status));
    }

    @PatchMapping("/reorder-suggestions/{id}")
    public ResponseEntity<ReorderSuggestionResponse> decideReorder(@PathVariable Long id,
                                                                     @Valid @RequestBody DecisionRequest req) {
        return ResponseEntity.ok(recommendationService.decideReorder(id, req.getDecision()));
    }
}
