package org.openl.studio.projects.model.merge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

public record MergeRequest(

        @Schema(description = "Merge operation mode.")
        @NotNull
        MergeOpMode mode,

        @Schema(description = "Target branch name to merge into or from which to merge. Depends on the merge operation mode.")
        @NotBlank
        String otherBranch
) {
}
