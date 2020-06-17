package org.openl.rules.model.scaffolding;

public class FieldModel {
    private String name;
    private String type;
    private Object defaultValue;
    private String format;

    private FieldModel(String name, String type, Object defaultValue, String format) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.format = format;
    }

    public static final class Builder {
        private String name;
        private String type;
        private Object defaultValue;
        private String format;

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

        public Builder setFormat(String format) {
            this.format = format;
            return this;
        }

        public FieldModel build() {
            return new FieldModel(name, type, defaultValue, format);
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

    public String getFormat() {
        return format;
    }
}
