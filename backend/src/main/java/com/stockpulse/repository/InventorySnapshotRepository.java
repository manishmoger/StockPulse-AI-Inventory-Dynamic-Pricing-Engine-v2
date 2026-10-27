package com.stockpulse.repository;

import com.stockpulse.entity.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, Long> {
    List<InventorySnapshot> findByProductIdOrderByRecordedAtDesc(Long productId, Pageable pageable);
}
