package com.stockpulse.service;

import com.stockpulse.dto.ActivityLogResponse;
import com.stockpulse.dto.DashboardMetrics;
import com.stockpulse.entity.Product;
import com.stockpulse.enums.ProductCategory;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final ProductRepository productRepo;
    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final ActivityService activityService;

    @Value("${app.demand-spike-multiplier:3.0}")
    private double demandSpikeMultiplier;

    public DashboardService(ProductRepository productRepo,
                             PricingSuggestionRepository pricingRepo,
                             ReorderSuggestionRepository reorderRepo,
                             ActivityService activityService) {
        this.productRepo = productRepo;
        this.pricingRepo = pricingRepo;
        this.reorderRepo = reorderRepo;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public DashboardMetrics getMetrics() {
        DashboardMetrics m = new DashboardMetrics();
        m.setTotalProducts(productRepo.count());
        m.setLowStockCount(productRepo.countLowStock());
        m.setOutOfStockCount(productRepo.countOutOfStock());
        m.setPendingPricingSuggestions(pricingRepo.countPending());
        m.setPendingReorderSuggestions(reorderRepo.countPending());
        m.setDemandSpikeCount(countDemandSpikes());

        Map<String, Long> dist = new LinkedHashMap<>();
        dist.put("ACTIVE", productRepo.count() - productRepo.countLowStock() - productRepo.countOutOfStock());
        dist.put("LOW_STOCK", productRepo.countLowStock());
        dist.put("OUT_OF_STOCK", productRepo.countOutOfStock());
        m.setInventoryStatusDistribution(dist);

        List<ActivityLogResponse> recent = activityService.getRecent(10)
                .stream().map(ActivityLogResponse::from).toList();
        m.setRecentActivity(recent);
        return m;
    }

    private long countDemandSpikes() {
        List<Product> all = productRepo.findAll();
        long count = 0;
        for (ProductCategory cat : ProductCategory.values()) {
            Double avg = productRepo.findAvgDemandVelocityByCategory(cat);
            if (avg == null || avg == 0) continue;
            for (Product p : all) {
                if (p.getCategory() == cat && p.getDemandVelocity() > demandSpikeMultiplier * avg) {
                    count++;
                }
            }
        }
        return count;
    }
}
