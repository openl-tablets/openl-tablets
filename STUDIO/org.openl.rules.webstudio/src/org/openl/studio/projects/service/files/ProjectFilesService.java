package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.studio.projects.model.files.FsNode;

/**
 * Service for managing files and folders of a mount (a project's working copy or a repository subtree).
 *
 */
public interface ProjectFilesService {

    /**
     * Get resources from a mount.
     *
     * @param root      the file root to read from
     * @param query     filtering criteria
     * @param recursive whether to include nested resources recursively
     * @param viewMode  FLAT returns a flat list, NESTED returns tree structure
     * @param version   optional historical revision to read; {@code null} or blank reads the latest
     * @return list of resources matching the criteria
     */
    List<FsNode> getResources(@NotNull FileRoot root,
                                @NotNull FileCriteriaQuery query,
                                boolean recursive,
                                @NotNull FileViewMode viewMode,
                                String version);

    /**
     * Get a file resource by its path.
     *
     * @param root    the file root
     * @param path    mount-relative path to the resource (e.g. "folder/rules.xlsx")
     * @param version optional historical revision to read; {@code null} or blank reads the latest
     * @return the resource
     */
    AProjectResource getResource(@NotNull FileRoot root, @NotBlank String path, String version);

    /**
     * Get metadata for a single file by its path.
     *
     * @param root    the file root
     * @param path    mount-relative path to the file (e.g. "folder/rules.xlsx")
     * @param version optional historical revision to read; {@code null} or blank reads the latest
     * @return file metadata
     */
    FsNode getNode(@NotNull FileRoot root, @NotBlank String path, String version);

    /**
     * Update a file resource with new content.
     *
     * @param root    the file root
     * @param path    mount-relative path to the resource (e.g. "folder/rules.xlsx")
     * @param content new file content
     */
    void updateResource(@NotNull FileRoot root, @NotBlank String path, @NotNull InputStream content);

    /**
     * Delete a resource (file or folder) by its path.
     *
     * @param root the file root
     * @param path mount-relative path to the resource (e.g. "folder/rules.xlsx")
     */
    void deleteResource(@NotNull FileRoot root, @NotBlank String path);

    /**
     * Copy a file resource to a new location within the mount.
     *
     * @param root            the file root
     * @param sourcePath      mount-relative path to the source file (e.g. "folder/rules.xlsx")
     * @param destinationPath relative path within the mount for the new file (e.g. "folder/copy.xlsx")
     */
    void copyResource(@NotNull FileRoot root, @NotBlank String sourcePath, @NotBlank String destinationPath);

    /**
     * Move (or rename) a file resource to a new location within the mount.
     *
     * @param root            the file root
     * @param sourcePath      mount-relative path to the source file (e.g. "folder/rules.xlsx")
     * @param destinationPath relative path within the mount for the moved file (e.g. "folder/renamed.xlsx")
     */
    void moveResource(@NotNull FileRoot root, @NotBlank String sourcePath, @NotBlank String destinationPath);

    /**
     * Upload a new file to the mount.
     *
     * @param root          the file root
     * @param path          relative path within the mount for the new file (e.g. "folder/newFile.xlsx")
     * @param content       file content
     * @param createFolders if {@code true}, intermediate folders are created automatically;
     *                      if {@code false}, the parent folder must already exist
     */
    void createResource(@NotNull FileRoot root,
                        @NotBlank String path,
                        @NotNull InputStream content,
                        boolean createFolders);

    /**
     * Create a folder at the given path. The operation is idempotent — an existing folder
     * is left unchanged.
     *
     * @param root          the file root
     * @param path          mount-relative folder path (e.g. "folder/subfolder")
     * @param createParents if {@code true}, missing intermediate folders are created automatically;
     *                      if {@code false}, every parent folder must already exist
     */
    void createFolder(@NotNull FileRoot root, @NotBlank String path, boolean createParents);

    /**
     * Write a folder and all of its descendants to the stream as a ZIP archive. Entry paths
     * are relative to the requested folder. Files the user cannot read are skipped.
     *
     * @param root    the file root
     * @param path    mount-relative folder path (e.g. "folder")
     * @param out     stream the archive is written to
     * @param version optional historical revision to read; {@code null} or blank reads the latest
     */
    void writeFolderAsZip(@NotNull FileRoot root, @NotBlank String path, @NotNull OutputStream out, String version)
            throws IOException;

    /**
     * Expand a ZIP archive into a folder. Entry paths are resolved relative to the folder and
     * missing intermediate folders are created. Each entry is validated for a safe path and for
     * content consistency. The upload is bounded to guard against malicious archives.
     *
     * @param root           the file root
     * @param path           mount-relative target folder (e.g. "folder"); empty for the mount root
     * @param archive        the ZIP archive stream
     * @param createParents  if {@code true}, the target folder and missing parents are created
     * @param conflictPolicy how to handle an entry whose target file already exists
     */
    void uploadArchive(@NotNull FileRoot root,
                       @NotNull String path,
                       @NotNull InputStream archive,
                       boolean createParents,
                       @NotNull ConflictPolicy conflictPolicy) throws IOException;

    /**
     * Upload several files into a folder as one operation. Each file's name is resolved relative to
     * the folder, missing intermediate folders are created, and the name and content are validated.
     * On a repository mount the files are committed as a single changeset.
     *
     * @param root           the file root
     * @param path           mount-relative target folder (e.g. "folder"); empty for the mount root
     * @param files          the files to upload, each carrying a name and content
     * @param conflictPolicy how to handle a file whose target already exists
     */
    void uploadFiles(@NotNull FileRoot root,
                     @NotNull String path,
                     @NotNull List<UploadedFile> files,
                     @NotNull ConflictPolicy conflictPolicy);

    /**
     * A file to upload: its folder-relative name and content bytes.
     */
    record UploadedFile(String name, byte[] content) {
    }

    /**
     * Search files and folders. {@code SUBTREE} scope matches entries within the mount by ant-glob
     * path pattern, file extensions, type and a case-insensitive content substring. {@code ANCESTORS}
     * scope walks up from a path to the repository root and returns matches of the pattern at each
     * level, nearest first.
     *
     * @param root  the file root
     * @param query the search criteria
     * @return matching files and folders, sorted (folders first); empty if nothing matches
     */
    List<FsNode> search(@NotNull FileRoot root, @NotNull FileSearchQuery query);
}
