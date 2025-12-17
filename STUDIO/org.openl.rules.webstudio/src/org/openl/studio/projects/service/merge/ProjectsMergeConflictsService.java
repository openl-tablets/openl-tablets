package org.openl.studio.projects.service.merge;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.InputStreamSource;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.FileItem;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.FileConflictResolution;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.ResolveConflictsResponse;

public interface ProjectsMergeConflictsService {

    List<ConflictGroup> getMergeConflicts(MergeConflictInfo mergeConflictInfo);

    FileItem getConflictFileItem(MergeConflictInfo mergeConflictInfo, String path, ConflictBase side) throws IOException;

    ResolveConflictsResponse resolveConflicts(MergeConflictInfo mergeConflictInfo,
                                              List<FileConflictResolution> resolutions,
                                              Map<String, InputStreamSource> customFiles,
                                              String mergeMessage) throws IOException, ProjectException;

}
