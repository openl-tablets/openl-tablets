package org.openl.studio.projects.service.resources;

import java.io.InputStream;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.resources.Resource;

/**
 * Service for retrieving project resources (files and folders).
 *
 */
public interface ProjectResourcesService {

    /**
     * Get resources from a project.
     *
     * @param project   the rules project to get resources from
     * @param query     filtering criteria
     * @param recursive whether to include nested resources recursively
     * @param viewMode  FLAT returns a flat list, NESTED returns tree structure
     * @return list of resources matching the criteria
     */
    List<Resource> getResources(@NotNull RulesProject project,
                                @NotNull ResourceCriteriaQuery query,
                                boolean recursive,
                                @NotNull ResourceViewMode viewMode);

    /**
     * Get a file resource by its path.
     *
     * @param project the rules project
     * @param path    project-relative path to the resource (e.g. "folder/rules.xlsx")
     * @return the project resource
     */
    AProjectResource getResource(@NotNull RulesProject project, @NotBlank String path);

    /**
     * Update a file resource with new content.
     *
     * @param project the rules project
     * @param path    project-relative path to the resource (e.g. "folder/rules.xlsx")
     * @param content new file content
     */
    void updateResource(@NotNull RulesProject project, @NotBlank String path, @NotNull InputStream content);

    /**
     * Delete a resource (file or folder) by its path.
     *
     * @param project the rules project
     * @param path    project-relative path to the resource (e.g. "folder/rules.xlsx")
     */
    void deleteResource(@NotNull RulesProject project, @NotBlank String path);

    /**
     * Copy a file resource to a new location within the project.
     *
     * @param project         the rules project
     * @param sourcePath      project-relative path to the source file (e.g. "folder/rules.xlsx")
     * @param destinationPath relative path within the project for the new file (e.g. "folder/copy.xlsx")
     */
    void copyResource(@NotNull RulesProject project, @NotBlank String sourcePath, @NotBlank String destinationPath);

    /**
     * Upload a new file to the project.
     *
     * @param project       the rules project
     * @param path          relative path within the project for the new file (e.g. "folder/newFile.xlsx")
     * @param content       file content
     * @param createFolders if {@code true}, intermediate folders are created automatically;
     *                      if {@code false}, the parent folder must already exist
     */
    void createResource(@NotNull RulesProject project,
                        @NotBlank String path,
                        @NotNull InputStream content,
                        boolean createFolders);
}
