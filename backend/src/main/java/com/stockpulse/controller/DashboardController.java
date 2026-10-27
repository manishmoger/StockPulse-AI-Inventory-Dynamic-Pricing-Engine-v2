package com.stockpulse.controller;

import com.stockpulse.dto.ActivityLogResponse;
import com.stockpulse.dto.DashboardMetrics;
import com.stockpulse.service.ActivityService;
import com.stockpulse.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;
    private final ActivityService activityService;

    public DashboardController(DashboardService dashboardService, ActivityService activityService) {
        this.dashboardService = dashboardService;
        this.activityService = activityService;
    }

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<DashboardMetrics> getMetrics() {
        return ResponseEntity.ok(dashboardService.getMetrics());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<ActivityLogResponse>> getActivity() {
        return ResponseEntity.ok(
                activityService.getAll().stream().map(ActivityLogResponse::from).toList()
        );
    }
}
