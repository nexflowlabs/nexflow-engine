package io.nexflow.engine.core.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkflowValidator {

    public static void validate(WorkflowDefinition workflow) {

        List<ValidationError> errors = new ArrayList<>();

        if (workflow.getName() == null || workflow.getName().isBlank()) {
            errors.add(new ValidationError("Workflow name is required"));
        }

        if (workflow.getVersion() <= 0) {
            errors.add(new ValidationError("Workflow version must be > 0"));
        }

        if (workflow.getStart() == null) {
            errors.add(new ValidationError("Start step is required"));
        }

        Map<String, StepDefinition> steps = workflow.getSteps();
        if (steps == null || steps.isEmpty()) {
            errors.add(new ValidationError("At least one step is required"));
        }

        if (steps != null && workflow.getStart() != null &&
                !steps.containsKey(workflow.getStart())) {
            errors.add(new ValidationError("Start step does not exist: " + workflow.getStart()));
        }

        if (steps != null) {
            steps.forEach((name, step) -> validateStep(name, step, steps, errors));
        }

        if (!errors.isEmpty()) {
            throw new InvalidWorkflowException(errors);
        }
    }

    private static void validateStep(
            String name,
            StepDefinition step,
            Map<String, StepDefinition> steps,
            List<ValidationError> errors
    ) {

        if (step.getType() == null) {
            errors.add(new ValidationError("Step type missing: " + name));
            return;
        }

        switch (step.getType()) {

            case TASK -> {
                if (step.getTask() == null) {
                    errors.add(new ValidationError("Task name missing: " + name));
                }
                validateTransition(step.getOnSuccess(), steps, name, "onSuccess", errors);
                validateTransition(step.getOnFailure(), steps, name, "onFailure", errors);
            }

            case DECISION -> {
                if (step.getExpression() == null) {
                    errors.add(new ValidationError("Decision expression missing: " + name));
                }
                validateTransition(step.getOnTrue(), steps, name, "onTrue", errors);
                validateTransition(step.getOnFalse(), steps, name, "onFalse", errors);
            }

            case WAIT -> {
                if (step.getDuration() == null && step.getEvent() == null) {
                    errors.add(new ValidationError(
                            "WAIT step must have duration or event: " + name));
                }
                validateTransition(step.getNext(), steps, name, "next", errors);
            }

            case END -> {
                if (step.getStatus() == null) {
                    errors.add(new ValidationError("END step must define status: " + name));
                }
            }
        }
    }

    private static void validateTransition(
            String target,
            Map<String, StepDefinition> steps,
            String stepName,
            String field,
            List<ValidationError> errors
    ) {
        if (target != null && !steps.containsKey(target)) {
            errors.add(new ValidationError(
                    "Invalid transition in step '" + stepName +
                            "' (" + field + " -> " + target + ")"));
        }
    }
}

