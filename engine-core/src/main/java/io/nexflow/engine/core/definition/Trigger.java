package io.nexflow.engine.core.definition;

/**
 * v1 DSL trigger (e.g. MANUAL, EVENT).
 */
public class Trigger {
    private String type;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
