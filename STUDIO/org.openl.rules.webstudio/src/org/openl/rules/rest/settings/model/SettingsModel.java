package org.openl.rules.rest.settings.model;

import io.swagger.v3.oas.annotations.Parameter;

public class SettingsModel {

    @Parameter(required = true)
    private final EntrypointSettingsModel entrypoint;

    @Parameter(description = "User management mode")
    private final UserManagementMode userMode;

    @Parameter(required = true)
    private final SupportedFeaturesModel supportedFeatures;

    private SettingsModel(Builder builder) {
        this.entrypoint = builder.entrypoint;
        this.userMode = builder.userMode;
        this.supportedFeatures = builder.supportedFeatures;
    }

    public EntrypointSettingsModel getEntrypoint() {
        return entrypoint;
    }

    public UserManagementMode getUserMode() {
        return userMode;
    }

    public SupportedFeaturesModel getSupportedFeatures() {
        return supportedFeatures;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EntrypointSettingsModel entrypoint;
        private UserManagementMode userMode;
        private SupportedFeaturesModel supportedFeatures;

        private Builder() {
        }

        public Builder entrypoint(EntrypointSettingsModel entrypoint) {
            this.entrypoint = entrypoint;
            return this;
        }

        public Builder userMode(UserManagementMode userMode) {
            this.userMode = userMode;
            return this;
        }

        public Builder supportedFeatures(SupportedFeaturesModel supportedFeatures) {
            this.supportedFeatures = supportedFeatures;
            return this;
        }

        public SettingsModel build() {
            return new SettingsModel(this);
        }
    }
}
