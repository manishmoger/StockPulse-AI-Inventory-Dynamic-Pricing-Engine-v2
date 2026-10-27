package com.stockpulse.service;

import com.stockpulse.dto.CreateProductRequest;
import com.stockpulse.dto.ProductResponse;
import com.stockpulse.entity.InventorySnapshot;
import com.stockpulse.entity.Product;
import com.stockpulse.enums.ActivityType;
import com.stockpulse.enums.ProductCategory;
import com.stockpulse.enums.ProductStatus;
import com.stockpulse.enums.TriggerReason;
import com.stockpulse.event.DemandSpikeEvent;
import com.stockpulse.event.InventoryLowEvent;
import com.stockpulse.exception.ResourceNotFoundException;
import com.stockpulse.repository.InventorySnapshotRepository;
import com.stockpulse.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepo;
    private final InventorySnapshotRepository snapshotRepo;
    private final ActivityService activityService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.demand-spike-multiplier:3.0}")
    private double demandSpikeMultiplier;

    public ProductService(ProductRepository productRepo,
                          InventorySnapshotRepository snapshotRepo,
                          ActivityService activityService,
                          ApplicationEventPublisher eventPublisher) {
        this.productRepo = productRepo;
        this.snapshotRepo = snapshotRepo;
        this.activityService = activityService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest req) {
        if (productRepo.existsBySku(req.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + req.getSku());
        }
        Product p = new Product();
        p.setSku(req.getSku());
        p.setName(req.getName());
        p.setCategory(req.getCategory());
        p.setCurrentPrice(req.getCurrentPrice());
        p.setCostPrice(req.getCostPrice());
        p.setStockLevel(req.getStockLevel());
        p.setReorderThreshold(req.getReorderThreshold());
        p.setDemandVelocity(req.getDemandVelocity() != null ? req.getDemandVelocity() : 0);
        p.setStatus(req.getStockLevel() == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE);
        Product saved = productRepo.save(p);
        activityService.log(ActivityType.PRODUCT_CREATED, saved.getSku(), saved.getName(),
                "Product created with stock=" + saved.getStockLevel() + ", price=" + saved.getCurrentPrice());
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(ProductStatus status, ProductCategory category, String search) {
        return productRepo.findWithFilters(status, category, search)
                .stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findById(id));
    }

    @Transactional
    public ProductResponse updateStock(Long id, int newStock) {
        Product p = findById(id);
        int oldStock = p.getStockLevel();
        p.setStockLevel(newStock);
        updateProductStatus(p);
        Product saved = productRepo.save(p);
        snapshotRepo.save(InventorySnapshot.of(saved, "STOCK_UPDATED"));
        activityService.log(ActivityType.STOCK_UPDATED, saved.getSku(), saved.getName(),
                "Stock updated from " + oldStock + " to " + newStock);
        publishEventsIfNeeded(saved);
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse simulateOrder(Long id, int quantity) {
        Product p = findById(id);
        if (p.getStockLevel() < quantity) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + p.getStockLevel());
        }
        p.setStockLevel(p.getStockLevel() - quantity);
        p.setDemandVelocity(p.getDemandVelocity() + quantity);
        updateProductStatus(p);
        Product saved = productRepo.save(p);
        snapshotRepo.save(InventorySnapshot.of(saved, "ORDER_SIMULATED"));
        activityService.log(ActivityType.ORDER_SIMULATED, saved.getSku(), saved.getName(),
                "Order simulated: qty=" + quantity + ", remaining stock=" + saved.getStockLevel());
        publishEventsIfNeeded(saved);
        return ProductResponse.from(saved);
    }

    /**
     * Called by RecommendationService after a PricingSuggestion is persisted.
     * Transitions ACTIVE → PRICE_REVIEW_PENDING so the state machine is correct.
     * OUT_OF_STOCK is not overwritten — it takes priority.
     */
    @Transactional
    public void markPriceReviewPending(Long productId) {
        Product p = findById(productId);
        if (p.getStatus() == ProductStatus.ACTIVE) {
            p.setStatus(ProductStatus.PRICE_REVIEW_PENDING);
            productRepo.save(p);
            log.debug("Product {} transitioned to PRICE_REVIEW_PENDING", p.getSku());
        }
    }

    private void publishEventsIfNeeded(Product p) {
        if (p.getStockLevel() < p.getReorderThreshold()) {
            log.info("Publishing InventoryLowEvent for product {}", p.getSku());
            eventPublisher.publishEvent(new InventoryLowEvent(this, p.getId(), TriggerReason.INVENTORY_LOW));
        }
        Double avgVelocity = productRepo.findAvgDemandVelocityByCategory(p.getCategory());
        if (avgVelocity != null && avgVelocity > 0 && p.getDemandVelocity() > demandSpikeMultiplier * avgVelocity) {
            log.info("Publishing DemandSpikeEvent for product {}", p.getSku());
            eventPublisher.publishEvent(new DemandSpikeEvent(this, p.getId()));
        }
    }

    private void updateProductStatus(Product p) {
        if (p.getStockLevel() == 0) {
            p.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (p.getStatus() == ProductStatus.OUT_OF_STOCK) {
            p.setStatus(ProductStatus.ACTIVE);
        }
        // PRICE_REVIEW_PENDING is set separately via markPriceReviewPending()
        // and cleared only when a pricing suggestion is accepted (back to ACTIVE).
    }

    public Product findById(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional
    public Product findByIdForUpdate(Long id) {
        return productRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public double getCategoryAvgVelocity(ProductCategory category) {
        Double avg = productRepo.findAvgDemandVelocityByCategory(category);
        return avg != null ? avg : 0.0;
    }
}
