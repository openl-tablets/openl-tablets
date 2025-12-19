package org.openl.studio.projects.service.merge;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.merge.MergeConflictInfo;

@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ProjectsMergeConflictsSessionHolder {

    private record Entry(ProjectIdModel projectId,
                         MergeConflictInfo mergeConflictInfo) {
    }

    private final AtomicReference<Entry> ref = new AtomicReference<>();

    public void store(ProjectIdModel projectId, MergeConflictInfo mergeConflictInfo) {
        ref.set(new Entry(projectId, mergeConflictInfo));
    }

    public boolean hasConflictInfo(ProjectIdModel projectId) {
        Entry e = ref.get();
        return e != null && e.projectId().equals(projectId);
    }

    public MergeConflictInfo getConflictInfo(ProjectIdModel projectId) {
        Entry e = ref.get();
        if (e != null && e.projectId().equals(projectId)) {
            return e.mergeConflictInfo();
        }
        return null;
    }

    public void remove(ProjectIdModel projectId) {
        Entry e = ref.get();
        if (e != null && e.projectId().equals(projectId)) {
            ref.set(null);
        }
    }
}
