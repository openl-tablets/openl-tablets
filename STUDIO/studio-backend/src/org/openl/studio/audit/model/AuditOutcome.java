package org.openl.studio.audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "How the action of an audit event ended.")
public enum AuditOutcome {

    @Schema(description = "The action was done.")
    @JsonProperty("success")
    SUCCESS,

    @Schema(description = "The action failed, such as a sign-in with wrong credentials.")
    @JsonProperty("failure")
    FAILURE,

    @Schema(description = "The action was refused for lack of permission.")
    @JsonProperty("denied")
    DENIED
}
