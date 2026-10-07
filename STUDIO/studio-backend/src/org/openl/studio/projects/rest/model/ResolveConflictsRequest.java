package org.openl.studio.projects.rest.model;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record ResolveConflictsRequest(
        @Schema(description = "projects.merge.param.resolutions.desc")
        @NotNull
        @Size(min = 1)
        List<FileConflictResolution> resolutions,

        @Schema(description = "projects.merge.param.message.desc")
        String message
) {
}
