package org.openl.studio.projects.model.project.status;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record CompilationModules(
        @Parameter(description = "Names of modules that have been compiled so far.")
        List<String> compiledModules,

        @Parameter(description = "Total number of modules involved in the project compilation.")
        int total,

        @Parameter(description = "Number of modules that have already been compiled.")
        int compiled
) {

    public static CompilationModules empty() {
        return new CompilationModules(List.of(), 0, 0);
    }
}
