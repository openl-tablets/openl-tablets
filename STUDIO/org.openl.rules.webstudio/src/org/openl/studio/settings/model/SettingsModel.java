package org.openl.studio.settings.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;

public class SettingsModel {

    @Getter
    @Parameter(description = "User management mode")
    private final UserManagementMode userMode;

    @Getter
    @Parameter(required = true)
    private final SupportedFeaturesModel supportedFeatures;

    @Getter
    @Parameter(description = "List of JavaScript files to be loaded in the application")
    private final List<String> scripts;

    private SettingsModel(Builder builder) {
        this.userMode = builder.userMode;
        this.supportedFeatures = builder.supportedFeatures;
        this.scripts = Optional.ofNullable(builder.scripts).map(List::copyOf).orElseGet(Collections::emptyList);
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
