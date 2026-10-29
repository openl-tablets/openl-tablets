package org.openl.studio.audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What an audit event records. Each action belongs to one {@link AuditCategory}.
 */
@Schema(description = "What an audit event records.")
@Getter
@RequiredArgsConstructor
public enum AuditAction {

    @Schema(description = "A user signed in, or failed to.")
    @JsonProperty("signIn")
    SIGN_IN(AuditCategory.AUTHENTICATION),

    @Schema(description = "A user signed out.")
    @JsonProperty("signOut")
    SIGN_OUT(AuditCategory.AUTHENTICATION),

    @Schema(description = "A session started for a client that sends its credentials with every request.")
    @JsonProperty("sessionStarted")
    SESSION_STARTED(AuditCategory.AUTHENTICATION),

    @Schema(description = "A session ended without a sign-out: it was left idle, or a restart ended it.")
    @JsonProperty("sessionEnded")
    SESSION_ENDED(AuditCategory.AUTHENTICATION),

    @Schema(description = "A user was created.")
    @JsonProperty("userCreated")
    USER_CREATED(AuditCategory.USER),

    @Schema(description = "The details of a user changed, such as the name or the e-mail.")
    @JsonProperty("userUpdated")
    USER_UPDATED(AuditCategory.USER),

    @Schema(description = "The password of a user was set.")
    @JsonProperty("passwordChanged")
    PASSWORD_CHANGED(AuditCategory.USER),

    @Schema(description = "A user was deleted.")
    @JsonProperty("userDeleted")
    USER_DELETED(AuditCategory.USER),

    @Schema(description = "A user was added to a group.")
    @JsonProperty("groupMemberAdded")
    GROUP_MEMBER_ADDED(AuditCategory.USER),

    @Schema(description = "A user was removed from a group.")
    @JsonProperty("groupMemberRemoved")
    GROUP_MEMBER_REMOVED(AuditCategory.USER),

    @Schema(description = "The groups the identity provider reports for a user changed.")
    @JsonProperty("externalGroupsChanged")
    EXTERNAL_GROUPS_CHANGED(AuditCategory.USER),

    @Schema(description = "A group was created.")
    @JsonProperty("groupCreated")
    GROUP_CREATED(AuditCategory.GROUP),

    @Schema(description = "A group was renamed or its description changed.")
    @JsonProperty("groupUpdated")
    GROUP_UPDATED(AuditCategory.GROUP),

    @Schema(description = "The privileges of a group changed, such as the administrator privilege.")
    @JsonProperty("groupPrivilegesChanged")
    GROUP_PRIVILEGES_CHANGED(AuditCategory.GROUP),

    @Schema(description = "A group was deleted.")
    @JsonProperty("groupDeleted")
    GROUP_DELETED(AuditCategory.GROUP),

    @Schema(description = "A personal access token was created.")
    @JsonProperty("tokenCreated")
    TOKEN_CREATED(AuditCategory.TOKEN),

    @Schema(description = "A personal access token was deleted.")
    @JsonProperty("tokenDeleted")
    TOKEN_DELETED(AuditCategory.TOKEN),

    @Schema(description = "A user or a group was given a role it did not hold.")
    @JsonProperty("roleGranted")
    ROLE_GRANTED(AuditCategory.PERMISSION),

    @Schema(description = "The role of a user or a group changed.")
    @JsonProperty("roleChanged")
    ROLE_CHANGED(AuditCategory.PERMISSION),

    @Schema(description = "The role of a user or a group was taken away.")
    @JsonProperty("roleRevoked")
    ROLE_REVOKED(AuditCategory.PERMISSION),

    @Schema(description = "A request was refused as forbidden: the user may not do what it asks.")
    @JsonProperty("accessDenied")
    ACCESS_DENIED(AuditCategory.ACCESS),

    @Schema(description = "A project was created in a design repository.")
    @JsonProperty("projectCreated")
    PROJECT_CREATED(AuditCategory.PROJECT),

    @Schema(description = "A project was created as a copy of another one.")
    @JsonProperty("projectCopied")
    PROJECT_COPIED(AuditCategory.PROJECT),

    @Schema(description = "The changes of a project were saved to its design repository.")
    @JsonProperty("projectSaved")
    PROJECT_SAVED(AuditCategory.PROJECT),

    @Schema(description = "A project was deleted.")
    @JsonProperty("projectDeleted")
    PROJECT_DELETED(AuditCategory.PROJECT),

    @Schema(description = "The lock another user held on a project was released by force.")
    @JsonProperty("projectUnlocked")
    PROJECT_UNLOCKED(AuditCategory.PROJECT),

    @Schema(description = "A branch was created in a design repository.")
    @JsonProperty("branchCreated")
    BRANCH_CREATED(AuditCategory.PROJECT),

    @Schema(description = "A branch was deleted from a design repository.")
    @JsonProperty("branchDeleted")
    BRANCH_DELETED(AuditCategory.PROJECT),

    @Schema(description = "A branch was merged into another one.")
    @JsonProperty("branchMerged")
    BRANCH_MERGED(AuditCategory.PROJECT),

    @Schema(description = "The protection of a branch was bypassed to change it.")
    @JsonProperty("protectionBypassed")
    PROTECTION_BYPASSED(AuditCategory.PROJECT),

    @Schema(description = "A project was deployed to a production repository.")
    @JsonProperty("projectDeployed")
    PROJECT_DEPLOYED(AuditCategory.DEPLOYMENT),

    @Schema(description = "System settings got other values, all those one save changed.")
    @JsonProperty("settingsChanged")
    SETTINGS_CHANGED(AuditCategory.SETTINGS),

    @Schema(description = "OpenL Studio started.")
    @JsonProperty("applicationStarted")
    APPLICATION_STARTED(AuditCategory.APPLICATION),

    @Schema(description = "OpenL Studio restarted without a stop to apply the settings saved.")
    @JsonProperty("applicationRestarted")
    APPLICATION_RESTARTED(AuditCategory.APPLICATION),

    @Schema(description = "OpenL Studio stopped.")
    @JsonProperty("applicationStopped")
    APPLICATION_STOPPED(AuditCategory.APPLICATION),

    @Schema(description = "The audit log was read or exported.")
    @JsonProperty("auditLogRead")
    AUDIT_LOG_READ(AuditCategory.AUDIT),

    @Schema(description = "The events older than the retention period were removed from the audit log.")
    @JsonProperty("auditLogPurged")
    AUDIT_LOG_PURGED(AuditCategory.AUDIT);

    private final AuditCategory category;
}
