package org.openl.studio.projects.model.project.status;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record ModifiedBy(
        @Parameter(description = "Username of the user who last modified the project.")
        String author,

        @Parameter(description = "Timestamp of the last modification.")
        ZonedDateTime date
) {
}
