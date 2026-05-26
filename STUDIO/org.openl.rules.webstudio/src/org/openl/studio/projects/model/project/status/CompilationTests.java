package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record CompilationTests(
        @Parameter(description = "Total number of test methods discovered in the compiled project.")
        int total
) {
}
