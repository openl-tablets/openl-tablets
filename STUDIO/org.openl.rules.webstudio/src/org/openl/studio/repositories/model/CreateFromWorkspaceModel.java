package org.openl.studio.repositories.model;

import java.util.List;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Request to publish one or more local workspace projects to a design repository.
 */
public record CreateFromWorkspaceModel(
        @Parameter(description = "Names of the local projects to publish")
        @Size(max = CreateFromWorkspaceModel.MAX_PROJECTS)
        List<String> names,
        @Parameter(description = "Path within the repository (non-flat repositories only)") String path,
        @Parameter(description = "Commit comment") String comment,
        @Parameter(description = "Target branch. An absent branch is created from the repository base branch")
        String branch) {

    public static final int MAX_PROJECTS = 100;

    public CreateFromWorkspaceModel(List<String> names, String path, String comment) {
        this(names, path, comment, null);
    }
}
