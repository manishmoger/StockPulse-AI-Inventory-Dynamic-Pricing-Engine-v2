package com.stockpulse.seed;

import com.stockpulse.entity.Product;
import com.stockpulse.enums.ProductCategory;
import com.stockpulse.enums.ProductStatus;
import com.stockpulse.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ProductRepository productRepo;

    public DataSeeder(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedProducts();
    }

    private void seedProducts() {
        List<Object[]> seeds = List.of(
            new Object[]{"SKU-ELEC-001", "Wireless Earbuds Pro",       ProductCategory.ELECTRONICS, "79.99",  45, 20, 3,  ProductStatus.ACTIVE},
            new Object[]{"SKU-ELEC-002", "USB-C Hub 7-Port",           ProductCategory.ELECTRONICS, "34.99", 120, 30, 1,  ProductStatus.ACTIVE},
            new Object[]{"SKU-APP-001",  "Organic Cotton T-Shirt",     ProductCategory.APPAREL,     "24.99",   8, 15, 12, ProductStatus.PRICE_REVIEW_PENDING},
            new Object[]{"SKU-APP-002",  "Running Shorts - Navy",      ProductCategory.APPAREL,     "39.99",  55, 20, 2,  ProductStatus.ACTIVE},
            new Object[]{"SKU-HOME-001", "Ceramic Pour-Over Set",      ProductCategory.HOME,        "49.99",  22, 10, 4,  ProductStatus.ACTIVE},
            new Object[]{"SKU-HOME-002", "LED Desk Lamp - Dimmable",   ProductCategory.HOME,        "59.99",   0, 15, 0,  ProductStatus.OUT_OF_STOCK},
            new Object[]{"SKU-ELEC-003", "Portable Charger 20K",       ProductCategory.ELECTRONICS, "44.99",  18, 25, 8,  ProductStatus.ACTIVE},
            new Object[]{"SKU-APP-003",  "Hoodie - Heather Grey",      ProductCategory.APPAREL,     "54.99",  11, 12, 15, ProductStatus.ACTIVE}
        );

        for (Object[] seed : seeds) {
            String sku = (String) seed[0];
            if (productRepo.existsBySku(sku)) {
                log.debug("Skipping seed for existing SKU: {}", sku);
                continue;
            }
            Product p = new Product();
            p.setSku(sku);
            p.setName((String) seed[1]);
            p.setCategory((ProductCategory) seed[2]);
            p.setCurrentPrice(new BigDecimal((String) seed[3]));
            int stock = (int) seed[4];
            p.setStockLevel(stock);
            p.setReorderThreshold((int) seed[5]);
            p.setDemandVelocity((int) seed[6]);
            p.setStatus((ProductStatus) seed[7]);
            productRepo.save(p);
            log.info("Seeded product: {} - {}", sku, seed[1]);
        }
    }
}
