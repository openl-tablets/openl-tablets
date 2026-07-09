package org.openl.studio.projects.model;

import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.studio.projects.converter.ProjectStatusDeserializer;

/**
 * Model for updating project status
 *
 * @author Vladyslav Pikus
 */
@Builder
@Jacksonized
public record ProjectStatusUpdateModel(
        @Parameter(description = "Target project status to transition to.", schema = @Schema(allowableValues = {"OPENED", "CLOSED"}))
        @JsonDeserialize(using = ProjectStatusDeserializer.class)
        ProjectStatus status,

        @Parameter(description = "Branch name to switch the project to. Applicable only to repositories that support branches.")
        String branch,

        @Parameter(description = "Specific project revision to open. When provided, the project is opened at this historical version.")
        String revision,

        @Parameter(description = "Commit message used when the project has local modifications that must be saved "
                + "before the status change.")
        String comment,

        @Parameter(description = "Optional. Set to true only when comment is null and local modifications must be "
                + "saved using the repository's default comment generation rules.")
        Boolean save,

        @Parameter(description = "Discard local project modifications while closing, switching branches, "
                + "or opening a revision.")
        Boolean discardChanges,

        @Parameter(description = "List of branches selected by the user for this project.")
        Set<String> selectedBranches,

        @Parameter(description = "When opening a project, also open its dependency projects. Defaults to false.")
        Boolean openDependencies
) {
}
