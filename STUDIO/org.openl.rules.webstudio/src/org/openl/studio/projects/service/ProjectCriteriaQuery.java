package org.openl.studio.projects.service;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Singular;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.studio.projects.model.ProjectIdModel;

/**
 * Project criteria query. Used to filter projects in {@link ProjectService}.
 *
 * @author Vladyslav Pikus
 */
@Builder
public record ProjectCriteriaQuery(
        @Schema(description = "Identifier of the repository to filter projects by.")
        String repositoryId,

        @Schema(description = "Status of the projects to filter by.")
        ProjectStatus status,

        @Schema(description = "Identifier of the project that the returned projects depend on.")
        ProjectIdModel dependsOn,

        @Schema(description = "Set of tags to filter projects by.")
        @Singular
        Map<String, String> tags,

        @Schema(description = "Project name to filter by (partial match, case-insensitive).")
        String name
) {

    public ProjectCriteriaQuery {
        tags = tags != null ? Map.copyOf(tags) : Map.of();
    }
}
