package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record ModuleMessageSource(
        @Parameter(description = "Module name (workbook file name without extension) the message originates from.")
        String name,

        @Parameter(description = """
                Identifier of the project the module belongs to, which is not always the project being compiled: \
                a message can be raised in a project it depends on. Absent when the project could not be named.""")
        String projectId,

        @Parameter(description = "Display name of the project the module belongs to.")
        String project
) implements MessageSource {
}
