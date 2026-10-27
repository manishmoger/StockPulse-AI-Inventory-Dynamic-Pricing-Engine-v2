package com.stockpulse.controller;

import com.stockpulse.ai.LLMGateway;
import com.stockpulse.config.StrategyConfig;
import com.stockpulse.dto.StrategyRequest;
import com.stockpulse.dto.StrategyResponse;
import com.stockpulse.enums.ActivityType;
import com.stockpulse.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final StrategyConfig strategyConfig;
    private final LLMGateway llmGateway;
    private final ActivityService activityService;

    public SettingsController(StrategyConfig strategyConfig, LLMGateway llmGateway,
                               ActivityService activityService) {
        this.strategyConfig = strategyConfig;
        this.llmGateway = llmGateway;
        this.activityService = activityService;
    }

    @GetMapping("/strategy")
    public ResponseEntity<StrategyResponse> getStrategy() {
        return ResponseEntity.ok(buildResponse());
    }

    @PutMapping("/strategy")
    public ResponseEntity<StrategyResponse> setStrategy(@Valid @RequestBody StrategyRequest req) {
        String previous = strategyConfig.getActiveStrategyName();
        strategyConfig.setActiveStrategy(req.getStrategy());
        activityService.log(ActivityType.STRATEGY_CHANGED, "SYSTEM", "System",
                "Strategy changed from " + previous + " to " + strategyConfig.getActiveStrategyName());
        return ResponseEntity.ok(buildResponse());
    }

    private StrategyResponse buildResponse() {
        StrategyResponse r = new StrategyResponse();
        r.setActiveStrategy(strategyConfig.getActiveStrategyName());
        r.setAiConfigured(llmGateway.isConfigured());
        r.setAiProvider(llmGateway.getProvider());
        r.setMessage(llmGateway.isConfigured()
                ? "AI provider (" + llmGateway.getProvider() + ") is configured and ready."
                : "AI provider is not configured. Set AI_API_KEY to enable AI mode. Rule-based mode works without credentials.");
        return r;
    }
}
