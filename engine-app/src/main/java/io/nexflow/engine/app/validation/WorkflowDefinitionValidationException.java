package io.nexflow.engine.app.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when workflow definition fails validation before publish.
 * Contains one or more validation error messages.
 */
public class WorkflowDefinitionValidationException extends RuntimeException {

    private final List<String> messages;

    public WorkflowDefinitionValidationException(List<String> messages) {
        super(messages != null && !messages.isEmpty() ? String.join("; ", messages) : "Validation failed");
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }

    public List<String> getMessages() {
        return new ArrayList<>(messages);
    }
}
