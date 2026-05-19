package org.openl.studio.projects.model.project.status;

import java.util.LinkedHashMap;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

import org.openl.message.Severity;
import org.openl.rules.rest.compile.MessageDescription;

@Builder
public record ProjectStatusViewModel (
        @Parameter(description = "Project identifier.")
        String project,

        @Parameter(description = "Branch name the project is currently opened on. Applicable only to repositories that support branches.")
        String branch,

        @Parameter(description = "Revision of the project currently opened.")
        String revision,

        @Parameter(description = "Current compilation state of the project.")
        CompileState compileState,

        @Parameter(description = "Information about the user who last modified the project.")
        ModifiedBy author,

        @Parameter(description = "Compilation messages grouped by severity.")
        LinkedHashMap<Severity, List<MessageDescription>> messages
) {
}
