package org.openl.studio.projects.service.merge;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import org.openl.rules.workspace.ProjectId;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.session.ClientSessionScope;

@Component
@ClientSessionScope
public class ProjectsMergeConflictsSessionHolder {

    /**
     * A conflict kept for a project.
     *
     * @param textFiles which files of the conflict are text, by their paths, as far as they were asked about
     */
    private record Entry(ProjectId projectId,
                         MergeConflictInfo mergeConflictInfo,
                         Map<String, Boolean> textFiles) {
    }

    private final AtomicReference<Entry> ref = new AtomicReference<>();

    public void store(ProjectId projectId, MergeConflictInfo mergeConflictInfo) {
        ref.set(new Entry(projectId, mergeConflictInfo, new ConcurrentHashMap<>()));
    }

    /**
     * Which files of a conflict are text, by their paths, as far as they were asked about. The versions of the conflict
     * kept here do not change, so what is found about its files is kept with it; any other conflict keeps nothing.
     */
    public Map<String, Boolean> textFilesOf(MergeConflictInfo mergeConflictInfo) {
        var e = ref.get();
        return e != null && e.mergeConflictInfo() == mergeConflictInfo ? e.textFiles() : new HashMap<>();
    }

    public boolean hasConflictInfo(ProjectId projectId) {
        var e = ref.get();
        return e != null && e.projectId().equals(projectId);
    }

    /**
     * Refuses to go on while a merge has left conflicts of the project to resolve.
     *
     * @param projectId the project
     * @throws ConflictException when the project has unresolved merge conflicts
     */
    public void requireResolved(ProjectId projectId) {
        if (hasConflictInfo(projectId)) {
            throw new ConflictException("project.unresolved.merge.conflicts.message");
        }
    }

    public MergeConflictInfo getConflictInfo(ProjectId projectId) {
        var e = ref.get();
        if (e != null && e.projectId().equals(projectId)) {
            return e.mergeConflictInfo();
        }
        return null;
    }

    public void remove(ProjectId projectId) {
        var e = ref.get();
        if (e != null && e.projectId().equals(projectId)) {
            ref.set(null);
        }
    }
}
