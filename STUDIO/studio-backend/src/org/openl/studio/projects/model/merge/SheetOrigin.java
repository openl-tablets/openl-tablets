package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Which branch a sheet of a workbook merged automatically was taken from.
 */
@Schema(description = "Which branch a sheet of a workbook merged automatically was taken from")
public enum SheetOrigin {

    @Schema(description = "Taken from the target branch, the branch the merge changes")
    @JsonProperty("target")
    TARGET,

    @Schema(description = "Taken from the source branch, the branch merged in")
    @JsonProperty("source")
    SOURCE,

    @Schema(description = "Combined from both branches")
    @JsonProperty("both")
    BOTH
}
