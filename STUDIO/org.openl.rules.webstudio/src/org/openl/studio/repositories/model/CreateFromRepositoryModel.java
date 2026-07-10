package org.openl.studio.repositories.model;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.rules.rest.validation.PathConstraint;

/**
 * Request to import an existing folder of a non-flat design repository as a project.
 */
public record CreateFromRepositoryModel(
        @Parameter(description = "Internal folder path to import as a project") @NotBlank @PathConstraint String path) {
}
