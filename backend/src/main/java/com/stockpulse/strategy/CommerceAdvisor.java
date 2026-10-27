package com.stockpulse.strategy;

public interface CommerceAdvisor {
    PricingRecommendation recommendPricing(AdvisorContext context);
    ReorderRecommendation recommendReorder(AdvisorContext context);
    String getName();
}
