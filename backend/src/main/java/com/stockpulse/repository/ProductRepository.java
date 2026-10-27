package com.stockpulse.repository;

import com.stockpulse.entity.Product;
import com.stockpulse.enums.ProductCategory;
import com.stockpulse.enums.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Product> findWithFilters(@Param("status") ProductStatus status,
                                   @Param("category") ProductCategory category,
                                   @Param("search") String search);

    @Query("SELECT AVG(p.demandVelocity) FROM Product p WHERE p.category = :category")
    Double findAvgDemandVelocityByCategory(@Param("category") ProductCategory category);

    List<Product> findByStatus(ProductStatus status);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockLevel < p.reorderThreshold AND p.stockLevel > 0")
    long countLowStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockLevel = 0")
    long countOutOfStock();
}
