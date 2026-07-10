package org.openl.studio.projects.service.files;

import java.util.List;

import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileItem;
import org.openl.studio.projects.model.files.FsNode;

/**
 * A mount the files service operates on — a project's working copy or a repository subtree.
 *
 * <p>Hides where the artefact tree comes from and how access to the mount is authorized, so the
 * same service can serve both the {@code /projects/{id}/files} and {@code /repos/{id}/files} mounts.
 * Per-artefact permissions are checked separately and uniformly by the service.
 *
 * @author Yury Molchan
 */
public interface FileRoot {

    /**
     * Artefact tree to read from. A blank version reads the current state; a non-blank version
     * reads that historical revision. An unknown revision is reported as not found.
     */
    AProjectFolder readFolder(String version);

    /**
     * Writable artefact tree for the current state. Mutations applied to it are committed to the mount.
     */
    AProjectFolder writeFolder();

    /**
     * Verifies the current user may read the mount.
     */
    void requireReadable();

    /**
     * Verifies the mount can be modified now and the current user may write to it.
     */
    void requireModifiable();

    /**
     * Finds files named like the trailing segment of {@code lookupPath} by walking up from the anchor
     * to the repository root, returning the match at each level nearest to the anchor first. The walk
     * goes up only — descendants and sibling branches are not visited — and is not limited to the
     * mount's project scope.
     */
    List<FsNode> searchAncestors(String lookupPath);

    /**
     * Writes the given files as one atomic changeset, using {@code comment} as the commit message.
     * Each item's name is the mount-relative path; existing files at those paths are overwritten.
     *
     * <p>A {@code DIFF} changeset adds and overwrites the listed files, leaving others intact.
     * A {@code FULL} changeset makes the base folder contain exactly the listed files: files under
     * it that are absent from the list are deleted.
     *
     * @param basePath      mount-relative folder the changeset applies to; empty for the mount root
     * @param items         the files to write, named by their mount-relative paths
     * @param changesetType how the changeset treats files absent from the list
     * @param comment       the commit message
     */
    void writeBatch(String basePath, List<FileItem> items, ChangesetType changesetType, String comment);
}
