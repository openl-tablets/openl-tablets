package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.studio.common.exception.PreconditionFailedException;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.util.HashingUtils;

/**
 * Service for managing files and folders of a mount (a project's working copy or a repository subtree).
 *
 * <p>How a write is persisted depends on the mount. A project mount stages writes in the working copy;
 * they reach the design repository only on a later save. A repository mount commits each write
 * immediately, and groups a multi-file write into a single changeset.
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
     * The version of a file: a digest of its content. It changes whenever the content of the file changes.
     *
     * @param resource the file
     * @return the version
     */
    default String getVersion(@NotNull AProjectResource resource) {
        try (var content = resource.getContent()) {
            return HashingUtils.sha256Hex(content);
        } catch (IOException | ProjectException e) {
            throw new IllegalStateException("Cannot read the file '%s'.".formatted(resource.getName()), e);
        }
    }

    /**
     * Update a file resource with new content.
     *
     * @param root    the file root
     * @param path    mount-relative path to the resource (e.g. "folder/rules.xlsx")
     * @param content new file content
     */
    default void updateResource(@NotNull FileRoot root, @NotBlank String path, @NotNull InputStream content) {
        updateResource(root, path, content, null);
    }

    /**
     * Update a file resource with new content, based on the version of the file the client read.
     *
     * @param root            the file root
     * @param path            mount-relative path to the resource (e.g. "folder/rules.xlsx")
     * @param content         new file content
     * @param expectedVersion the version of the file the write is based on, or {@code null} to write whatever the
     *                        file holds
     * @throws PreconditionFailedException when the file has another version: it changed since the client read it
     */
    void updateResource(@NotNull FileRoot root,
                        @NotBlank String path,
                        @NotNull InputStream content,
                        @Nullable String expectedVersion);

    /**
     * Delete a resource (file or folder) by its path.
     *
     * @param root the file root
     * @param path mount-relative path to the resource (e.g. "folder/rules.xlsx")
     */
    default void deleteResource(@NotNull FileRoot root, @NotBlank String path) {
        deleteResource(root, path, null);
    }

    /**
     * Delete a resource (file or folder) by its path, based on the version of the file the client read.
     *
     * @param root            the file root
     * @param path            mount-relative path to the resource (e.g. "folder/rules.xlsx")
     * @param expectedVersion the version of the file the deletion is based on, or {@code null} to delete whatever the
     *                        file holds; a folder has no version and is not compared
     * @throws PreconditionFailedException when the file has another version: it changed since the client read it
     */
    void deleteResource(@NotNull FileRoot root, @NotBlank String path, @Nullable String expectedVersion);

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
     * Write a single file, following the conflict policy when a file is already there.
     *
     * <p>A file that is not there is created as {@link #createResource} creates it. A file that is there fails the
     * write for {@link ConflictPolicy#FAIL}, is left as it is for {@link ConflictPolicy#SKIP}, and has its content
     * replaced, as {@link #updateResource} replaces it, for {@link ConflictPolicy#OVERWRITE} and
     * {@link ConflictPolicy#REPLACE}. A folder at the path fails the write whatever the policy.
     *
     * <p>A closed project stays reserved from the look at the path to the write, so a write made in between by
     * another request cannot turn the decision of the policy into a failed create.
     *
     * @param root           the file root
     * @param path           relative path within the mount for the file (e.g. "folder/newFile.xlsx")
     * @param content        file content
     * @param createFolders  if {@code true}, intermediate folders of a new file are created automatically;
     *                       if {@code false}, the parent folder must already exist
     * @param conflictPolicy what to do with a file that is already there
     * @return what the write did with the file
     */
    FileWriteOutcome writeResource(@NotNull FileRoot root,
                                   @NotBlank String path,
                                   @NotNull InputStream content,
                                   boolean createFolders,
                                   @NotNull ConflictPolicy conflictPolicy);

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
     * @param path    mount-relative folder path (e.g. "folder"); blank zips the whole project root
     * @param out     stream the archive is written to
     * @param version optional historical revision to read; {@code null} or blank reads the latest
     */
    void writeFolderAsZip(@NotNull FileRoot root, String path, @NotNull OutputStream out, String version)
            throws IOException;

    /**
     * Expand a ZIP archive into a folder as one atomic changeset. Entry paths are resolved relative
     * to the folder and missing intermediate folders are created. Each entry is validated for a safe
     * path and for content consistency. The upload is bounded to guard against malicious archives.
     *
     * <p>The {@code REPLACE} policy makes the folder contain exactly the archive entries: existing
     * files absent from the archive are deleted, so the archive must not be empty and the user must
     * be allowed to delete the removed files.
     *
     * @param root           the file root
     * @param path           mount-relative target folder (e.g. "folder"); empty for the mount root
     * @param archive        the ZIP archive stream
     * @param createParents  if {@code true}, the target folder and missing parents are created
     * @param conflictPolicy how to handle an entry whose target file already exists
     * @return the mount-relative paths of the files written; an entry the policy skips is not among them
     */
    List<String> uploadArchive(@NotNull FileRoot root,
                       @NotNull String path,
                       @NotNull InputStream archive,
                       boolean createParents,
                       @NotNull ConflictPolicy conflictPolicy) throws IOException;

    /**
     * Upload several files into a folder as one atomic changeset. Each file's name is resolved
     * relative to the folder, missing intermediate folders are created, and the name and content
     * are validated.
     *
     * <p>The {@code REPLACE} policy makes the folder contain exactly the uploaded files: existing
     * files absent from the upload are deleted, so the user must be allowed to delete the removed
     * files.
     *
     * @param root           the file root
     * @param path           mount-relative target folder (e.g. "folder"); empty for the mount root
     * @param files          the files to upload, each carrying a name and content
     * @param conflictPolicy how to handle a file whose target already exists
     * @return the mount-relative paths of the files written; a file the policy skips is not among them
     */
    List<String> uploadFiles(@NotNull FileRoot root,
                     @NotNull String path,
                     @NotNull List<UploadedFile> files,
                     @NotNull ConflictPolicy conflictPolicy);

    /**
     * A file to upload: its folder-relative name and content bytes.
     */
    record UploadedFile(String name, byte[] content) {

        @Override
        public boolean equals(Object obj) {
            return obj instanceof UploadedFile(var otherName, var otherContent)
                    && Objects.equals(name, otherName)
                    && Arrays.equals(content, otherContent);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hashCode(name) + Arrays.hashCode(content);
        }

        @Override
        public String toString() {
            return "UploadedFile[name=%s, content=%d bytes]".formatted(name, content == null ? 0 : content.length);
        }
    }

    /**
     * Writes several files to the mount as one changeset, and deletes others with them.
     *
     * <p>Every file is checked before the first one is written, the way a write of that file alone is checked:
     * its path, the user's permission to write it, and its content. A project descriptor is checked against the
     * one the project stores, with the files of the same write taken as held by the project. A refused file
     * leaves the mount as it was.
     *
     * <p>A deleted folder loses every file under it that the same write does not write again. A path that holds
     * nothing is left as it is. A module reading a deleted file stays declared in {@code rules.xml}, unless the
     * same write carries a descriptor without it.
     *
     * @param root    the file root
     * @param written the content of each file to create or replace, by its mount-relative path
     * @param deleted mount-relative paths of the files and folders to delete
     * @param comment the commit message
     */
    void writeFiles(@NotNull FileRoot root,
                    @NotNull Map<String, byte[]> written,
                    @NotNull Collection<String> deleted,
                    @NotBlank String comment);

    /**
     * Search files and folders. {@code SUBTREE} scope matches entries within the mount by ant-glob
     * path pattern, file extensions, type and a case-insensitive content substring. {@code ANCESTORS}
     * scope walks up from the anchor path to the repository root, returning the same-named file at
     * each level — not limited to the mount's project — nearest first, each with its content.
     *
     * @param root  the file root
     * @param query the search criteria
     * @return matching files and folders, sorted (folders first); empty if nothing matches
     */
    List<FsNode> search(@NotNull FileRoot root, @NotNull FileSearchQuery query);
}
