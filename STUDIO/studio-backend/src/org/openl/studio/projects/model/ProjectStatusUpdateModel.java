package org.openl.studio.projects.model;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.webstudio.web.repository.CommentValidator;
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

        @Parameter(description = """
                Change comment used when the project has modifications to save and its repository \
                supports comments.""")
        @Size(max = CommentValidator.MAX_COMMENT_LENGTH, message = "{openl.constraints.comment.too-long.message}")
        String comment,

        @Parameter(description = """
                Optional. Set to true when comment is null and local modifications must be saved. \
                The repository's default comment is generated only when comments are supported.""")
        Boolean save,

        @Parameter(description = """
                Discard local project modifications while closing, switching branches, \
                or opening a revision.""")
        Boolean discardChanges,

        @Parameter(description = "When opening a project, also open its dependency projects. Defaults to false.")
        Boolean openDependencies
) {
}
