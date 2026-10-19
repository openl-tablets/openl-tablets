package org.openl.studio.settings.model;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public final class SupportedFeaturesModel {

    @Parameter(description = "If groups management is enabled or not")
    private final boolean groupsManagement;

    @Parameter(description = "If user management is enabled or not")
    private final boolean userManagement;

    @Parameter(description = "If email verification is enabled or not")
    private final boolean emailVerification;

    @Parameter(description = "If personal access tokens are enabled or not")
    private final boolean personalAccessToken;

    @Parameter(description = "If the audit log is kept or not")
    private final boolean auditLog;

    private SupportedFeaturesModel(Builder builder) {
        this.groupsManagement = builder.groupsManagement;
        this.userManagement = builder.userManagement;
        this.emailVerification = builder.emailVerification;
        this.personalAccessToken = builder.personalAccessToken;
        this.auditLog = builder.auditLog;
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

    public boolean getAuditLog() {
        return auditLog;
    }

    public static Builder builder() {
        return new Builder();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder {
        private boolean groupsManagement;
        private boolean userManagement;
        private boolean emailVerification;
        private boolean personalAccessToken;
        private boolean auditLog;

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

        public Builder auditLog(boolean auditLog) {
            this.auditLog = auditLog;
            return this;
        }

        public SupportedFeaturesModel build() {
            return new SupportedFeaturesModel(this);
        }
    }
}
