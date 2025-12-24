package org.openl.rules.rest.settings.model;

import io.swagger.v3.oas.annotations.Parameter;

public class SupportedFeaturesModel {

    @Parameter(description = "If groups management is enabled or not")
    private final boolean groupsManagement;

    @Parameter(description = "If user management is enabled or not")
    private final boolean userManagement;

    @Parameter(description = "If email verification is enabled or not")
    private final boolean emailVerification;

    @Parameter(description = "If personal access tokens are enabled or not")
    private final boolean personalAccessToken;

    private SupportedFeaturesModel(Builder builder) {
        this.groupsManagement = builder.groupsManagement;
        this.userManagement = builder.userManagement;
        this.emailVerification = builder.emailVerification;
        this.personalAccessToken = builder.personalAccessToken;
    }

    public boolean getGroupsManagement() {
        return groupsManagement;
    }

    public boolean getUserManagement() {
        return userManagement;
    }

    public boolean getEmailVerification() {
        return emailVerification;
    }

    public boolean getPersonalAccessToken() {
        return personalAccessToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean groupsManagement;
        private boolean userManagement;
        private boolean emailVerification;
        private boolean personalAccessToken;

        private Builder() {
        }

        public Builder groupsManagement(boolean groupsManagement) {
            this.groupsManagement = groupsManagement;
            return this;
        }

        public Builder userManagement(boolean userManagement) {
            this.userManagement = userManagement;
            return this;
        }

        public Builder emailVerification(boolean emailVerification) {
            this.emailVerification = emailVerification;
            return this;
        }

        public Builder personalAccessToken(boolean personalAccessToken) {
            this.personalAccessToken = personalAccessToken;
            return this;
        }

        public SupportedFeaturesModel build() {
            return new SupportedFeaturesModel(this);
        }
    }
}
