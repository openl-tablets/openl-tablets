package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record CompilationTables(
        @Parameter(description = "Total number of tables in the compiled project.")
        int total,

        @Parameter(description = "Number of tables that have at least one error.")
        int errors
) {
}
