package com.stockpulse.repository;

import com.stockpulse.entity.ReorderSuggestion;
import com.stockpulse.enums.SuggestionStatus;
import com.stockpulse.enums.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {
    List<ReorderSuggestion> findByStatus(SuggestionStatus status);

    @Query("SELECT COUNT(rs) > 0 FROM ReorderSuggestion rs WHERE rs.product.id = :productId AND rs.status = 'PENDING' AND rs.triggerReason = :triggerReason")
    boolean existsPendingForProductAndTrigger(@Param("productId") Long productId, @Param("triggerReason") TriggerReason triggerReason);

    @Query("SELECT rs FROM ReorderSuggestion rs WHERE rs.product.id = :productId AND rs.status = 'PENDING' AND rs.triggerReason = :triggerReason ORDER BY rs.createdAt DESC")
    Optional<ReorderSuggestion> findFirstPendingForProductAndTrigger(@Param("productId") Long productId, @Param("triggerReason") TriggerReason triggerReason);

    List<ReorderSuggestion> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("SELECT COUNT(rs) FROM ReorderSuggestion rs WHERE rs.status = 'PENDING'")
    long countPending();
}
