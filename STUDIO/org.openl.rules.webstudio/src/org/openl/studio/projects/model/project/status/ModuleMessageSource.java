package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record ModuleMessageSource(
        @Parameter(description = "Module name (workbook file name without extension) the message originates from.")
        String name
) implements MessageSource {
}
