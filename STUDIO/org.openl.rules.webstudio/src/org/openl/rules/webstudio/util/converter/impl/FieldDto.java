package org.openl.rules.webstudio.util.converter.impl;

public class FieldDto {
    private String name;
    private String type;
    private String defaultValue;
    private String format;

    private FieldDto(String name, String type, String defaultValue, String format) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.format = format;
    }

    public static final class Builder {
        private String name;
        private String type;
        private String defaultValue;
        private String format;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder setFormat(String format) {
            this.format = format;
            return this;
        }

        public FieldDto build() {
            return new FieldDto(name, type, defaultValue, format);
        }
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getFormat() {
        return format;
    }
}
