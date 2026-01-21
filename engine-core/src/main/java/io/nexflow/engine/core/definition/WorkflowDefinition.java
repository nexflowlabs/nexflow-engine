package io.nexflow.engine.core.definition;

import java.util.Map;

public class WorkflowDefinition {

    private String name;
    private int version;
    private String description;
    private String start;
    private Map<String, StepDefinition> steps;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public Map<String, StepDefinition> getSteps() {
        return steps;
    }

    public void setSteps(Map<String, StepDefinition> steps) {
        this.steps = steps;
    }
}
