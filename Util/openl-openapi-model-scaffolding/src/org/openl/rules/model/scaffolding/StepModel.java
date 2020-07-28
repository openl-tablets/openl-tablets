package org.openl.rules.model.scaffolding;

public class StepModel {
    private String name;
    private String type;
    private String description;
    private Object value;

    public StepModel() {
    }

    public StepModel(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public StepModel(String name, String type, String description, Object value) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
