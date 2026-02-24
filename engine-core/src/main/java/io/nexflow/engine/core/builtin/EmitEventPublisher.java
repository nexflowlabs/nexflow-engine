package io.nexflow.engine.core.builtin;

/**
 * Abstraction for emitting events. Engine-app provides implementation (e.g. message broker).
 */
public interface EmitEventPublisher {

    void emit(String eventType, Object payload);
}
