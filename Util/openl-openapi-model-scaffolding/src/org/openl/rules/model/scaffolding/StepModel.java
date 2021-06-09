package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class StepModel {
    private String name;
    private String type;
    private String description;
    private String value;

    public StepModel() {
    }

    public StepModel(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public StepModel(String name, String type, String description, String value) {
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StepModel stepModel = (StepModel) o;

        if (!Objects.equals(name, stepModel.name)) {
            return false;
        }
        if (!Objects.equals(type, stepModel.type)) {
            return false;
        }
        if (!Objects.equals(description, stepModel.description)) {
            return false;
        }
        return Objects.equals(value, stepModel.value);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
