package com.stockpulse.repository;

import com.stockpulse.entity.PricingSuggestion;
import com.stockpulse.enums.SuggestionStatus;
import com.stockpulse.enums.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {
    List<PricingSuggestion> findByStatus(SuggestionStatus status);

    @Query("SELECT COUNT(ps) > 0 FROM PricingSuggestion ps WHERE ps.product.id = :productId AND ps.status = 'PENDING' AND ps.triggerReason = :triggerReason")
    boolean existsPendingForProductAndTrigger(@Param("productId") Long productId, @Param("triggerReason") TriggerReason triggerReason);

    @Query("SELECT ps FROM PricingSuggestion ps WHERE ps.product.id = :productId AND ps.status = 'PENDING' AND ps.triggerReason = :triggerReason ORDER BY ps.createdAt DESC")
    Optional<PricingSuggestion> findFirstPendingForProductAndTrigger(@Param("productId") Long productId, @Param("triggerReason") TriggerReason triggerReason);

    List<PricingSuggestion> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("SELECT COUNT(ps) FROM PricingSuggestion ps WHERE ps.status = 'PENDING'")
    long countPending();
}
