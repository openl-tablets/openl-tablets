package org.openl.studio.projects.service.merge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.merge.MergeConflictInfo;

/**
 * Holds unresolved merge-conflict information per project, so several projects merged in one session (multiple
 * browser tabs) keep their conflicts independently instead of one overwriting another.
 */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ProjectsMergeConflictsSessionHolder {

    private final Map<ProjectIdModel, MergeConflictInfo> conflicts = new ConcurrentHashMap<>();

    public void store(ProjectIdModel projectId, MergeConflictInfo mergeConflictInfo) {
        if (mergeConflictInfo == null) {
            conflicts.remove(projectId);
        } else {
            conflicts.put(projectId, mergeConflictInfo);
        }
    }

    public boolean hasConflictInfo(ProjectIdModel projectId) {
        return conflicts.containsKey(projectId);
    }

    public boolean hasAnyConflictInfo() {
        return !conflicts.isEmpty();
    }

    public MergeConflictInfo getConflictInfo(ProjectIdModel projectId) {
        return conflicts.get(projectId);
    }

    public void remove(ProjectIdModel projectId) {
        conflicts.remove(projectId);
    }
}
