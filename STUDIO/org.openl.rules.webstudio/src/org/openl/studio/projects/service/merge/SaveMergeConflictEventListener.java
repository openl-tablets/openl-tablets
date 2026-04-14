package org.openl.studio.projects.service.merge;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.openl.studio.projects.service.WorkspaceProjectService;

/**
 * Listens for {@link SaveMergeConflictEvent} and stores the conflict info
 * in {@link ProjectsMergeConflictsSessionHolder} so the React merge UI can find it.
 */
@Component
@RequiredArgsConstructor
public class SaveMergeConflictEventListener {

    private final WorkspaceProjectService projectService;
    private final ProjectsMergeConflictsSessionHolder conflictsSessionHolder;

    @EventListener
    public void onSaveMergeConflict(SaveMergeConflictEvent event) {
        var projectId = projectService.resolveProjectId(event.project());
        conflictsSessionHolder.store(projectId, event.conflictInfo());
    }
}
