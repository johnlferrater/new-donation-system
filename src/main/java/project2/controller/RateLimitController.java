package project2.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Simple endpoint used to verify that {@link project2.config.RateLimitFilter}
 * is enforced. It only reports that the request was accepted.
 */
@RestController
@RequestMapping("/api/rate-limit-test")
public class RateLimitController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> test() {
        return ResponseEntity.ok(Map.of(
                "message", "Request accepted",
                "timestamp", Instant.now().toString()));
    }
}
