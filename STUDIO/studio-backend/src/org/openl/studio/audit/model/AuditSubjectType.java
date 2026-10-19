package org.openl.studio.audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whose access or membership an audited action changed.")
public enum AuditSubjectType {

    @Schema(description = "A user, named by the login name.")
    @JsonProperty("user")
    USER,

    @Schema(description = "A group, named by its name.")
    @JsonProperty("group")
    GROUP
}
