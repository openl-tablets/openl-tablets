package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record ModuleTablesSummary(
        @Parameter(description = "Module name (as defined in the project descriptor).")
        String name,

        @Parameter(description = "Number of tables in the module (excluding OTHER tables).")
        int total,

        @Parameter(description = "Number of tables in the module that have at least one error.")
        int errors
) {
}
