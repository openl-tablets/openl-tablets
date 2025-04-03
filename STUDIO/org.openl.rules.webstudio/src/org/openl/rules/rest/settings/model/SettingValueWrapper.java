package org.openl.rules.rest.settings.model;

import io.swagger.v3.oas.annotations.Parameter;

public class SettingValueWrapper {

    @Parameter(description = "Setting value")
    private final Object value;

    @Parameter(description = "Is setting read-only. If true, the setting set by the system env and cannot be changed.")
    private final Boolean readOnly;

    @Parameter(description = "Is setting secret. If true, the setting value is not shown in the UI.")
    private final Boolean secret;

    public SettingValueWrapper(Builder builder) {
        this.readOnly = builder.readOnly;
        this.secret = builder.secret;
        this.value = Boolean.TRUE.equals(secret) ? null : builder.value;
    }

    public Object getValue() {
        return value;
    }

    public Boolean isReadOnly() {
        return readOnly;
    }

    public Boolean isSecret() {
        return secret;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Object value;
        private Boolean readOnly;
        private Boolean secret;

        private Builder() {
        }

        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly ? true : null;
            return this;
        }

        public Builder secret(boolean secret) {
            this.secret = secret ? true : null;
            return this;
        }

        public SettingValueWrapper build() {
            return new SettingValueWrapper(this);
        }
    }
}
