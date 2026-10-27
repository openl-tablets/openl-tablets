package org.openl.studio.repositories.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.workspace.ProjectId;

/**
 * Request to copy an existing project into a design repository under a new name.
 */
public record CreateFromProjectModel(
        @Parameter(description = "Id of the source project to copy, as the project list gives it")
        @NotNull ProjectId sourceProject,
        @Parameter(description = "Path within the repository (non-flat repositories only)") String path,
        @Parameter(description = "Commit comment")
        @Size(max = CommentValidator.MAX_COMMENT_LENGTH, message = "{openl.constraints.comment.too-long.message}")
        String comment,
        @Parameter(description = "Revision of the source project to copy. The latest revision is copied when omitted")
        String revision,
        @Parameter(description = "Target branch. An absent branch is created from the repository base branch")
        String branch) {

    public CreateFromProjectModel(ProjectId sourceProject, String path, String comment, String revision) {
        this(sourceProject, path, comment, revision, null);
    }
}
