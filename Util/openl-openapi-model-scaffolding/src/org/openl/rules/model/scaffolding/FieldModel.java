package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class FieldModel {

    private final String name;
    private final String type;
    private Object defaultValue;

    public FieldModel(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public FieldModel(String name, String type, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FieldModel that = (FieldModel) o;

        if (!Objects.equals(name, that.name)) {
            return false;
        }
        if (!Objects.equals(type, that.type)) {
            return false;
        }
        return Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        return result;
    }

}
