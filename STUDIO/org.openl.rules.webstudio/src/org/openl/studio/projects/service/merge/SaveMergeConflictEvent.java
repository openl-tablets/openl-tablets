package org.openl.studio.projects.service.merge;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.merge.MergeConflictInfo;

/**
 * Spring event published when a merge conflict occurs during a project save.
 * Allows decoupled storage of conflict info in the session holder for the React merge UI.
 */
public record SaveMergeConflictEvent(RulesProject project, MergeConflictInfo conflictInfo) {
}
