package org.openl.studio.projects.model.project.status;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record CompilationMessages(
        @Parameter(description = "Compilation messages ordered by id (ascending).")
        List<DetailedMessageDescription> items,

        @Parameter(description = "Total number of compilation messages across all severities.")
        int total,

        @Parameter(description = "Number of messages with ERROR severity.")
        int errors,

        @Parameter(description = "Number of messages with WARN severity.")
        int warnings
) {
}
