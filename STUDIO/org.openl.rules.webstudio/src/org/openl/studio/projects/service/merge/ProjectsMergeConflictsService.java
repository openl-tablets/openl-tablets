package org.openl.studio.projects.service.merge;

import java.io.IOException;
import java.util.List;

import org.openl.rules.repository.api.FileItem;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.MergeConflictInfo;

public interface ProjectsMergeConflictsService {

    List<ConflictGroup> getMergeConflicts(MergeConflictInfo mergeConflictInfo);

    FileItem getConflictFileItem(MergeConflictInfo mergeConflictInfo, String path, ConflictBase side) throws IOException;

}
