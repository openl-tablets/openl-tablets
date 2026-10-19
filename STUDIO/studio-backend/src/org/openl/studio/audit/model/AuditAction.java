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

    @Schema(description = "The audit log was read or exported.")
    @JsonProperty("auditLogRead")
    AUDIT_LOG_READ(AuditCategory.AUDIT),

    @Schema(description = "The events older than the retention period were removed from the audit log.")
    @JsonProperty("auditLogPurged")
    AUDIT_LOG_PURGED(AuditCategory.AUDIT);

    private final AuditCategory category;
}
