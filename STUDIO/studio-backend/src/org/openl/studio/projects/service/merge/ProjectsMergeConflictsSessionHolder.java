package org.openl.studio.projects.service.merge;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.session.ClientSessionScope;

@Component
@ClientSessionScope
public class ProjectsMergeConflictsSessionHolder {

    private record Entry(ProjectIdModel projectId,
                         MergeConflictInfo mergeConflictInfo) {
    }

    private final AtomicReference<Entry> ref = new AtomicReference<>();

    public void store(ProjectIdModel projectId, MergeConflictInfo mergeConflictInfo) {
        ref.set(new Entry(projectId, mergeConflictInfo));
    }

    public boolean hasConflictInfo(ProjectIdModel projectId) {
        var e = ref.get();
        return e != null && e.projectId().equals(projectId);
    }

    /**
     * Refuses to go on while a merge has left conflicts of the project to resolve.
     *
     * @param projectId the project
     * @throws ConflictException when the project has unresolved merge conflicts
     */
    public void requireResolved(ProjectIdModel projectId) {
        if (hasConflictInfo(projectId)) {
            throw new ConflictException("project.unresolved.merge.conflicts.message");
        }
    }

    public MergeConflictInfo getConflictInfo(ProjectIdModel projectId) {
        var e = ref.get();
        if (e != null && e.projectId().equals(projectId)) {
            return e.mergeConflictInfo();
        }
        return null;
    }

    public void remove(ProjectIdModel projectId) {
        var e = ref.get();
        if (e != null && e.projectId().equals(projectId)) {
            ref.set(null);
        }
    }
}
