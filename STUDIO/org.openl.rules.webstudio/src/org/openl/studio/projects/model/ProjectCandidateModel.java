package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One of the projects an ambiguous project name matches, with what a client needs to offer it as a choice.
 *
 * <p>Candidates may carry the same name, so each also tells where it is stored: the repository, and the folder within
 * a repository with mapped folders, which may hold several projects of one name.
 *
 * <p>A project that is stored in no repository carries neither.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectCandidateModel(
        @Parameter(description = "Project ID to address the project by")
        @NonNull ProjectIdModel id,
        @Parameter(description = "Project name")
        @NonNull String name,
        @Parameter(description = "Id of the repository the project is stored in")
        @Nullable String repository,
        @Parameter(description = "Name of the repository the project is stored in, as configured by an administrator")
        @Nullable String repositoryName,
        @Parameter(description = "Folder the project occupies in a repository with mapped folders")
        @Nullable String path) {
}
