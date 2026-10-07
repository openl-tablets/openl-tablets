package org.openl.studio.projects.model.project.status;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of pending change applied to a file relative to the design revision")
public enum ChangeType {

    @Schema(description = "File exists locally but is not present in the design revision")
    @JsonProperty("added")
    ADDED,

    @Schema(description = "File content differs from the design revision")
    @JsonProperty("modified")
    MODIFIED,

    @Schema(description = "File is present in the design revision but has been removed locally")
    @JsonProperty("deleted")
    DELETED
}
