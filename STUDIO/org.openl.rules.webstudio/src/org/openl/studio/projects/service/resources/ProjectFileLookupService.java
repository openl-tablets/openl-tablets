package org.openl.studio.projects.service.resources;

import java.io.IOException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.project.abstraction.AProject;
import org.openl.studio.projects.model.resources.ProjectFileLookupResponse;

/**
 * Resolves a file path relative to an OpenL project.
 *
 * <p>Lookup is read-only and never recursive. By default only the file located directly
 * under the project root is considered. When {@code searchParents} is {@code true} the
 * lookup additionally walks up the parent directories to the repository root and collects
 * every match from nearest to farthest ancestor.
 *
 * <p>Parent traversal is supported only for repositories with a folder structure; for
 * flat repositories the result is the same as a project-root-only lookup.
 */
public interface ProjectFileLookupService {

    /**
     * Resolves the given file path within the project.
     *
     * @param project        the rules project the lookup is anchored to
     * @param path           repository-safe relative file path (e.g. {@code "AGENTS.md"})
     * @param searchParents  if {@code true}, walk up parent directories to the repository root
     * @param includeContent whether to read and include the raw file content (UTF-8) in the response
     * @return matching files ordered from nearest to farthest ancestor; empty if nothing was found
     */
    ProjectFileLookupResponse lookup(@NotNull AProject project,
                                     @NotBlank String path,
                                     boolean searchParents,
                                     boolean includeContent) throws IOException;
}
