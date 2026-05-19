package org.openl.studio.projects.model.project.status;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project compilation state")
public enum CompileState {

    @Schema(description = "Project is not opened and no compilation has been triggered")
    @JsonProperty("idle")
    IDLE,

    @Schema(description = "Project compilation is in progress")
    @JsonProperty("compiling")
    COMPILING,

    @Schema(description = "Project compiled successfully without errors or warnings")
    @JsonProperty("ok")
    OK,

    @Schema(description = "Project compiled with warnings")
    @JsonProperty("warnings")
    WARNINGS,

    @Schema(description = "Project compilation failed with errors")
    @JsonProperty("errors")
    ERRORS
}
