package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.studio.repositories.model.RepositoryFeatures;

/**
 * The repository a project is stored in, as its own screens need it.
 *
 * <p>It travels with the project so that reading a project never depends on access to the repository as a
 * whole: a user may be granted a single project and still has to see where it lives and what it supports.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectRepositoryModel(
        @Parameter(description = "Repository id")
        String id,
        @Parameter(description = "Repository name as configured by an administrator")
        String name,
        @Parameter(description = "Repository type, such as repo-git or repo-jdbc")
        String type,
        @Parameter(description = "What the repository storage supports")
        RepositoryFeatures features) {
}
