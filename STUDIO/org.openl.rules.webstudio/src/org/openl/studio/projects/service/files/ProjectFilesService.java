package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.files.FsNode;

/**
 * Service for retrieving project resources (files and folders).
 *
 */
public interface ProjectFilesService {

    /**
     * Get resources from a project.
     *
     * @param project   the rules project to get resources from
     * @param query     filtering criteria
     * @param recursive whether to include nested resources recursively
     * @param viewMode  FLAT returns a flat list, NESTED returns tree structure
     * @return list of resources matching the criteria
     */
    List<FsNode> getResources(@NotNull RulesProject project,
                                @NotNull FileCriteriaQuery query,
                                boolean recursive,
                                @NotNull FileViewMode viewMode);

    /**
     * Get a file resource by its path.
     *
     * @param project the rules project
     * @param path    project-relative path to the resource (e.g. "folder/rules.xlsx")
     * @return the project resource
     */
    AProjectResource getResource(@NotNull RulesProject project, @NotBlank String path);

    /**
     * Get metadata for a single file by its path.
     *
     * @param project the rules project
     * @param path    project-relative path to the file (e.g. "folder/rules.xlsx")
     * @return file metadata
     */
    FsNode getNode(@NotNull RulesProject project, @NotBlank String path);

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
     * Move (or rename) a file resource to a new location within the project.
     *
     * @param project         the rules project
     * @param sourcePath      project-relative path to the source file (e.g. "folder/rules.xlsx")
     * @param destinationPath relative path within the project for the moved file (e.g. "folder/renamed.xlsx")
     */
    void moveResource(@NotNull RulesProject project, @NotBlank String sourcePath, @NotBlank String destinationPath);

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

    /**
     * Create a folder at the given path. The operation is idempotent — an existing folder
     * is left unchanged.
     *
     * @param project       the rules project
     * @param path          project-relative folder path (e.g. "folder/subfolder")
     * @param createParents if {@code true}, missing intermediate folders are created automatically;
     *                      if {@code false}, every parent folder must already exist
     */
    void createFolder(@NotNull RulesProject project, @NotBlank String path, boolean createParents);

    /**
     * Write a folder and all of its descendants to the stream as a ZIP archive. Entry paths
     * are relative to the requested folder. Files the user cannot read are skipped.
     *
     * @param project the rules project
     * @param path    project-relative folder path (e.g. "folder")
     * @param out     stream the archive is written to
     */
    void writeFolderAsZip(@NotNull RulesProject project, @NotBlank String path, @NotNull OutputStream out) throws IOException;
}
