package org.openl.studio.projects.converter;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;

/**
 * Resolves the project that owns the current session's unresolved merge conflict.
 *
 * <p>A save-time conflict keeps the project addressable by its original id while an unsaved rename differs from the
 * design repository. This allows every request made by the conflict-resolution dialog to reach the same project.
 *
 * <p>Invalid ids and ids unrelated to the active conflict are ignored so the regular project strategies can resolve
 * them.
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class MergeConflictProjectResolveStrategy implements ProjectResolveStrategy {

    private final ProjectsMergeConflictsSessionHolder conflictsSessionHolder;

    @Override
    public List<RulesProject> resolve(UserWorkspace workspace, String identity) {
        ProjectIdModel projectId;
        try {
            projectId = ProjectIdModel.decode(identity);
        } catch (Exception e) {
            return List.of();
        }
        var conflictInfo = conflictsSessionHolder.getConflictInfo(projectId);
        return conflictInfo == null ? List.of() : List.of(conflictInfo.project());
    }
}
