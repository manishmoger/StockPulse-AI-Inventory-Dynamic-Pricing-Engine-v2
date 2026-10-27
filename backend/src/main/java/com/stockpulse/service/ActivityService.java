package com.stockpulse.service;

import com.stockpulse.entity.ActivityLog;
import com.stockpulse.enums.ActivityType;
import com.stockpulse.repository.ActivityLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ActivityService {

    private final ActivityLogRepository repo;

    public ActivityService(ActivityLogRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(ActivityType type, String sku, String name, String details) {
        ActivityLog log = new ActivityLog();
        log.setType(type);
        log.setProductSku(sku);
        log.setProductName(name);
        log.setDetails(details);
        repo.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> getRecent(int limit) {
        return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> getAll() {
        return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 200));
    }
}
