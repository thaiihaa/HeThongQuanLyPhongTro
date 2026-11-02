package com.quanlyphongtro.health;

import com.quanlyphongtro.health.dto.DatabaseHealthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class DatabaseHealthController {

    private final DatabaseHealthService databaseHealthService;

    public DatabaseHealthController(DatabaseHealthService databaseHealthService) {
        this.databaseHealthService = databaseHealthService;
    }

    @GetMapping("/database")
    public ResponseEntity<DatabaseHealthResponse> databaseHealth() {
        DatabaseHealthResponse response = databaseHealthService.check();
        if ("UP".equals(response.status())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
