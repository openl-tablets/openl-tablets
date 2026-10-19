package org.openl.studio.audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The area an audit event belongs to.")
public enum AuditCategory {

    @Schema(description = "Signing in and out.")
    @JsonProperty("authentication")
    AUTHENTICATION,

    @Schema(description = "Users: their details, passwords and groups.")
    @JsonProperty("user")
    USER,

    @Schema(description = "Groups and their privileges.")
    @JsonProperty("group")
    GROUP,

    @Schema(description = "Personal access tokens.")
    @JsonProperty("token")
    TOKEN,

    @Schema(description = "Access rights: the roles of users and groups on repositories and projects.")
    @JsonProperty("permission")
    PERMISSION,

    @Schema(description = "Requests refused for lack of permission.")
    @JsonProperty("access")
    ACCESS,

    @Schema(description = "Reading and cleaning the audit log itself.")
    @JsonProperty("audit")
    AUDIT
}
