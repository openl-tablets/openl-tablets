package org.openl.rules.rest.settings.model;

import io.swagger.v3.oas.annotations.Parameter;

public class EntrypointSettingsModel {

    @Parameter(description = "Logout URL")
    private final String logoutUrl;

    private EntrypointSettingsModel(Builder builder) {
        this.logoutUrl = builder.logoutUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String logoutUrl;

        private Builder() {
        }

        public Builder logoutUrl(String logoutUrl) {
            this.logoutUrl = logoutUrl;
            return this;
        }

        public EntrypointSettingsModel build() {
            return new EntrypointSettingsModel(this);
        }
    }
}
