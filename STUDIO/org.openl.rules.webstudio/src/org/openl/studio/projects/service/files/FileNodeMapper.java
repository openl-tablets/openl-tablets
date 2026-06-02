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
            .comparing((FsNode r) -> r instanceof FileNode)
            .thenComparing(FsNode::getName, String.CASE_INSENSITIVE_ORDER);

    FsNode map(AProjectArtefact artefact);
}
