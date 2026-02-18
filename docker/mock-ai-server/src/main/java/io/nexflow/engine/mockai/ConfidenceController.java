package io.nexflow.engine.mockai;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mock endpoint for engine aiDecision step. Returns a configurable confidence (default 0.9).
 */
@RestController
@RequestMapping("/confidence")
public class ConfidenceController {

    @PostMapping
    public Map<String, Object> confidence(@RequestBody Map<String, Object> body) {
        // Default high confidence so workflows take onHighConfidence branch in examples
        double confidence = 0.9;
        if (body != null && body.containsKey("confidenceOverride")) {
            Object o = body.get("confidenceOverride");
            if (o instanceof Number) confidence = ((Number) o).doubleValue();
        }
        return Map.of("confidence", confidence);
    }
}
