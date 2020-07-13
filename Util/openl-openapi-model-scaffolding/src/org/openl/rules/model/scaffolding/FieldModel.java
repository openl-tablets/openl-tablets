package org.openl.rules.model.scaffolding;

public class FieldModel {

    private String name;
    private String type;
    private Object defaultValue;

    private FieldModel(String name, String type, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
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
