package org.openl.studio.repositories.model;

import java.util.Map;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.rules.rest.validation.PathConstraint;

/**
 * Request to import an existing folder of a non-flat design repository as a project.
 */
public record CreateFromRepositoryModel(
        @Parameter(description = "Internal folder path to import as a project") @PathConstraint String path,
        @Parameter(description = "Tags to assign to the imported project") Map<String, String> tags) {
}
