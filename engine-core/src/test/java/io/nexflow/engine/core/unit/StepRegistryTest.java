package io.nexflow.engine.core.unit;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;
import io.nexflow.engine.core.registry.StepRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StepRegistry: get by name, getAll unmodifiable.
 */
@DisplayName("StepRegistry unit tests")
class StepRegistryTest {

    private static final WorkflowStep STUB = ctx -> StepResult.success(Map.of());

    @Test
    @DisplayName("get returns step registered under name")
    void getReturnsStepRegisteredUnderName() {
        StepRegistry registry = new StepRegistry(Map.of("condition", STUB));
        assertSame(STUB, registry.get("condition"));
    }

    @Test
    @DisplayName("get returns null for unknown name")
    void getReturnsNullForUnknownName() {
        StepRegistry registry = new StepRegistry(Map.of("condition", STUB));
        assertNull(registry.get("unknown"));
    }

    @Test
    @DisplayName("getAll returns unmodifiable map")
    void getAllReturnsUnmodifiableMap() {
        StepRegistry registry = new StepRegistry(Map.of("a", STUB));
        assertThrows(UnsupportedOperationException.class, () -> registry.getAll().put("b", STUB));
    }

    @Test
    @DisplayName("null map constructor yields empty registry")
    void nullMapYieldsEmptyRegistry() {
        StepRegistry registry = new StepRegistry(null);
        assertNull(registry.get("any"));
        assertTrue(registry.getAll().isEmpty());
    }
}
