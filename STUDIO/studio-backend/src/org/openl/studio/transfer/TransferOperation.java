package org.openl.studio.transfer;

import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.service.files.ConflictPolicy;

/**
 * What a transfer link does when it is used: the whole operation, so that a request to the link can change none of
 * it.
 *
 * @author Yury Molchan
 */
public sealed interface TransferOperation {

    /** Whether the first request to the link spends it: an upload writes once, a download may be repeated. */
    default boolean once() {
        return false;
    }

    /**
     * Downloads a file of a project, or a folder of it as a ZIP archive.
     *
     * @param projectId the id of the project
     * @param path    the project-relative path of the file, or of the folder
     * @param branch  the branch the project must be on, or null for the one it is on
     * @param version the revision to read, or null for the working copy
     * @param archive whether the path is a folder downloaded as a ZIP archive
     */
    record Download(String projectId, String path, @Nullable String branch, @Nullable String version, boolean archive)
            implements TransferOperation {
    }

    /**
     * Downloads one version of a file of the merge conflict the client has pending in a project.
     *
     * @param projectId the id of the project
     * @param file the project-relative path of the conflicted file
     * @param side the version to download
     */
    record ConflictVersion(String projectId, String file, ConflictBase side) implements TransferOperation {
    }

    /**
     * Uploads one file of a project into its working copy.
     *
     * @param projectId      the id of the project
     * @param path           the project-relative path the file is written to
     * @param branch         the branch the project must be on, or null for the one it is on
     * @param conflictPolicy what to do with a file already at the path
     * @param createFolders  whether missing parent folders are created
     * @param size           the size the uploaded file must have, or null to take any size
     * @param sha256         the hex SHA-256 the uploaded file must have, or null to take any content
     */
    record Upload(String projectId, String path, @Nullable String branch, ConflictPolicy conflictPolicy,
                  boolean createFolders, @Nullable Long size, @Nullable String sha256) implements TransferOperation {

        @Override
        public boolean once() {
            return true;
        }
    }
}
