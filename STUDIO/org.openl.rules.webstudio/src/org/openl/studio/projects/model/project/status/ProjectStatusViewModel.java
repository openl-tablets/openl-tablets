package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

import org.openl.studio.projects.model.ProjectIdModel;

@Builder
public record ProjectStatusViewModel (
        @Parameter(description = "Project identifier.")
        ProjectIdModel projectId,

        @Parameter(description = "Branch name the project is currently opened on. Applicable only to repositories that support branches.")
        String branch,

        @Parameter(description = "Revision of the project currently opened.")
        String revision,

        @Parameter(description = "Current compilation state of the project.")
        CompileState compileState,

        @Parameter(description = "Information about the user who last modified the project.")
        ModifiedBy lastModifiedBy,

        @Parameter(description = "Compilation details: messages and module progress.")
        CompilationDetails compilation,

        @Parameter(description = "Files modified locally and not yet committed to the design repository.")
        PendingChanges pendingChanges
) {
}
