package org.openl.studio.repositories.model;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Request to copy an existing project into a design repository under a new name.
 */
public record CreateFromProjectModel(
        @Parameter(description = "Repository id of the source project")
        @NotBlank String sourceRepositoryId,
        @Parameter(description = "Identifier of the source project to copy: its id, or its name when one project of the source repository carries that name")
        @NotBlank String sourceProject,
        @Parameter(description = "Path within the repository (non-flat repositories only)") String path,
        @Parameter(description = "Commit comment") String comment,
        @Parameter(description = "Revision of the source project to copy. The latest revision is copied when omitted")
        String revision,
        @Parameter(description = "Target branch. An absent branch is created from the repository base branch")
        String branch) {

    public CreateFromProjectModel(String sourceRepositoryId,
                                  String sourceProject,
                                  String path,
                                  String comment,
                                  String revision) {
        this(sourceRepositoryId, sourceProject, path, comment, revision, null);
    }
}
