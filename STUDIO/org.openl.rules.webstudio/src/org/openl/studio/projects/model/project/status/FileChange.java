package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record FileChange(
        @Parameter(description = "Path of the changed file in the form `<projectRealPath>/<file>` (forward slashes), matching the format used by the merge API.")
        String path,

        @Parameter(description = "Type of change applied to the file.")
        ChangeType type
) {
}
