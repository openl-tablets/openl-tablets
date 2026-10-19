package org.openl.studio.audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The area an audit event belongs to.")
public enum AuditCategory {

    @Schema(description = "Signing in and out.")
    @JsonProperty("authentication")
    AUTHENTICATION,

    @Schema(description = "Reading and cleaning the audit log itself.")
    @JsonProperty("audit")
    AUDIT
}
