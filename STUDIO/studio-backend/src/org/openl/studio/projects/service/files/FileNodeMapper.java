package org.openl.studio.projects.service.files;

import java.util.Comparator;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.studio.projects.model.files.FileNode;
import org.openl.studio.projects.model.files.FsNode;

/**
 * Maps project artefacts to FsNode DTOs.
 */
public interface FileNodeMapper {

    Comparator<FsNode> NODE_COMPARATOR = Comparator
            .<FsNode, Boolean>comparing(FileNode.class::isInstance)
            .thenComparing(FsNode::getName, String.CASE_INSENSITIVE_ORDER);

    /**
     * Maps an artefact of a mount.
     *
     * <p>A file or a folder of a project is marked hidden when the project settings list it as a hidden or a
     * protected file. Nothing is hidden in a mount that is not a project.
     *
     * @param artefact the artefact, its internal path relative to the root of the mount
     * @param root     the mount the artefact belongs to
     */
    FsNode map(AProjectArtefact artefact, FileRoot root);
}
