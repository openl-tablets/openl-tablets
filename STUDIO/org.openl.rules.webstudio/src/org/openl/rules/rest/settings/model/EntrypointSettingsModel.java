package org.openl.rules.rest.settings.model;

import io.swagger.v3.oas.annotations.Parameter;

public class EntrypointSettingsModel {

    @Parameter(description = "Logout URL")
    private final String logoutUrl;

    @Parameter(description = "Login URL")
    private final String loginUrl;

    private EntrypointSettingsModel(Builder builder) {
        this.logoutUrl = builder.logoutUrl;
        this.loginUrl = builder.loginUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String logoutUrl;
        private String loginUrl;

        private Builder() {
        }

        public Builder logoutUrl(String logoutUrl) {
            this.logoutUrl = logoutUrl;
            return this;
        }

        public Builder loginUrl(String loginUrl) {
            this.loginUrl = loginUrl;
            return this;
        }

        public EntrypointSettingsModel build() {
            return new EntrypointSettingsModel(this);
        }
    }
}
