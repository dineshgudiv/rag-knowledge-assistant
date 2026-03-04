package com.companyname.ragassistant.controller;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Backward-compatible health endpoints for callers still using legacy paths.
 */
@RestController
public class LegacyHealthController {
    private final ApplicationAvailability availability;
    private final HealthEndpoint healthEndpoint;

    public LegacyHealthController(ApplicationAvailability availability, HealthEndpoint healthEndpoint) {
        this.availability = availability;
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        LivenessState state = availability.getLivenessState();
        String status = state == LivenessState.CORRECT ? "UP" : "DOWN";
        return ResponseEntity.ok(Map.of("status", status, "state", state.name()));
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        ReadinessState state = availability.getReadinessState();
        String status = state == ReadinessState.ACCEPTING_TRAFFIC ? "UP" : "OUT_OF_SERVICE";
        return ResponseEntity.ok(Map.of("status", status, "state", state.name()));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        HealthComponent health = healthEndpoint.health();
        String status = health != null && health.getStatus() != null ? health.getStatus().getCode() : "UNKNOWN";
        return ResponseEntity.ok(Map.of("status", status));
    }
}
