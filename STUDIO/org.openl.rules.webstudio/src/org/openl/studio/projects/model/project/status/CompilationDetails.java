package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record CompilationDetails(
        @Parameter(description = "Compilation messages produced by the project together with aggregate counts.")
        CompilationMessages messages,

        @Parameter(description = "Module compilation progress and the list of already-compiled module names.")
        CompilationModules modules,

        @Parameter(description = "Tests discovered in the compiled project.")
        CompilationTests tests
) {
}
