package org.openl.studio.repositories.model;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Request to copy an existing project into a design repository under a new name.
 */
public record CreateFromProjectModel(
        @Parameter(description = "Repository id of the source project") @NotBlank String sourceRepositoryId,
        @Parameter(description = "Name of the source project to copy") @NotBlank String sourceProjectName,
        @Parameter(description = "Path within the repository (non-flat repositories only)") String path,
        @Parameter(description = "Commit comment") String comment) {
}
