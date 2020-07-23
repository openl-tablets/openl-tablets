package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class FieldModel {

    private String name;
    private String type;
    private Object defaultValue;

    private FieldModel(String name, String type, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldModel that = (FieldModel) o;

        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(type, that.type)) return false;
        return Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private String name;
        private String type;
        private Object defaultValue;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public FieldModel build() {
            return new FieldModel(name, type, defaultValue);
        }
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

}
