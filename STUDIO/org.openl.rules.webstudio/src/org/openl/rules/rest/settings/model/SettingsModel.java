package org.openl.rules.rest.settings.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Parameter;

public class SettingsModel {

    @Parameter(description = "User management mode")
    private final UserManagementMode userMode;

    @Parameter(required = true)
    private final SupportedFeaturesModel supportedFeatures;

    @Parameter(description = "List of JavaScript files to be loaded in the application")
    private final List<String> scripts;

    private SettingsModel(Builder builder) {
        this.userMode = builder.userMode;
        this.supportedFeatures = builder.supportedFeatures;
        this.scripts = Optional.ofNullable(builder.scripts).map(List::copyOf).orElseGet(Collections::emptyList);
    }

    public UserManagementMode getUserMode() {
        return userMode;
    }

    public SupportedFeaturesModel getSupportedFeatures() {
        return supportedFeatures;
    }

    public List<String> getScripts() {
        return scripts;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UserManagementMode userMode;
        private SupportedFeaturesModel supportedFeatures;
        private List<String> scripts;

        private Builder() {
        }

        public Builder userMode(UserManagementMode userMode) {
            this.userMode = userMode;
            return this;
        }

        public Builder supportedFeatures(SupportedFeaturesModel supportedFeatures) {
            this.supportedFeatures = supportedFeatures;
            return this;
        }

        public Builder scripts(List<String> scripts) {
            this.scripts = Optional.ofNullable(scripts)
                    .map(List::copyOf)
                    .orElseGet(Collections::emptyList);
            return this;
        }

        public SettingsModel build() {
            return new SettingsModel(this);
        }
    }
}
