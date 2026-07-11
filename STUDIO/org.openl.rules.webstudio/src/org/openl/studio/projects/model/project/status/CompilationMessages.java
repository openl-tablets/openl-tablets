package org.openl.studio.projects.model.project.status;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

import org.openl.studio.common.model.GenericView;

@Builder
public record CompilationMessages(
        @Parameter(description = "Compilation messages ordered by id (ascending). Present only on the single-project detail and status responses, not in the projects list.")
        @JsonView(GenericView.Detailed.class)
        List<DetailedMessageDescription> items,

        @Parameter(description = "Total number of compilation messages across all severities.")
        int total,

        @Parameter(description = "Number of messages with ERROR severity.")
        int errors,

        @Parameter(description = "Number of messages with WARN severity.")
        int warnings
) {
}
