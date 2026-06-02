package org.openl.studio.projects.service.files;

import java.util.List;

import org.openl.rules.project.abstraction.AProjectFolder;
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
     * Walks up from {@code lookupPath} to the repository root, returning matches nearest first.
     * The path includes the trailing file/folder name to match at each level.
     */
    List<FsNode> searchAncestors(String lookupPath);
}
