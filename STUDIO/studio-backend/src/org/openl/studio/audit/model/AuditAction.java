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

    @Schema(description = "The audit log was read or exported.")
    @JsonProperty("auditLogRead")
    AUDIT_LOG_READ(AuditCategory.AUDIT),

    @Schema(description = "The events older than the retention period were removed from the audit log.")
    @JsonProperty("auditLogPurged")
    AUDIT_LOG_PURGED(AuditCategory.AUDIT);

    private final AuditCategory category;
}
