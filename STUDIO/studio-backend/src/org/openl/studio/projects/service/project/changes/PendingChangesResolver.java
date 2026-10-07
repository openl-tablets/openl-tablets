package org.openl.studio.projects.service.project.changes;

import jakarta.annotation.Nullable;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.project.status.PendingChanges;

/**
 * Enumerates uncommitted file changes for a workspace project by diffing the local
 * working copy against the design revision the project is currently opened on.
 *
 * @author Vladyslav Pikus
 */
public interface PendingChangesResolver {

    /**
     * @param project workspace project to inspect
     * @return summary of added, modified and deleted files, or {@code null} when the project
     *         has no local modifications
     */
    @Nullable
    PendingChanges resolve(RulesProject project);
}
