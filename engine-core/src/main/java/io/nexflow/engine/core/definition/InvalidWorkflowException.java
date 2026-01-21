package io.nexflow.engine.core.definition;

import java.util.List;

public class InvalidWorkflowException extends RuntimeException {

    private final List<ValidationError> errors;

    public InvalidWorkflowException(List<ValidationError> errors) {
        super("Invalid workflow definition");
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}

