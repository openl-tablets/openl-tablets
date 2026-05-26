package org.openl.studio.projects.model.project.status;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record PendingChanges(
        @Parameter(description = "Total number of pending file changes (added, modified and deleted).")
        int total,

        @Parameter(description = "Files changed locally and not yet committed to the design repository.")
        List<FileChange> files
) {
}
