package org.openl.rules.model.scaffolding;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

public class StepModel {
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String type;
    @Getter
    @Setter
    private String description;
    @Getter
    @Setter
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StepModel stepModel)) {
            return false;
        }

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
