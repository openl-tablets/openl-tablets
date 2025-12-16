package org.openl.studio.projects.service.merge;

import java.util.List;

import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.MergeConflictInfo;

public interface ProjectsMergeConflictsService {

    List<ConflictGroup> getMergeConflicts(MergeConflictInfo mergeConflictInfo);

}
