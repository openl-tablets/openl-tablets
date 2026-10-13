package org.openl.studio.projects.model.merge;

import java.util.List;
import java.util.Objects;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.MergeOutcome;

/**
 * What a committed merge changed, kept until the client reads it.
 *
 * @param sourceBranch   the branch merged in
 * @param targetBranch   the branch the merge changed
 * @param outcome        the files the merge changed and the workbooks combined without a decision of the user
 * @param projectsBefore the projects of the target branch before the merge that hold a changed file
 * @param projectsAfter  the projects of the target branch after the merge that hold a changed file
 */
public record MergedChanges(String sourceBranch,
                            String targetBranch,
                            MergeOutcome outcome,
                            List<AProject> projectsBefore,
                            List<AProject> projectsAfter) {

    public MergedChanges {
        Objects.requireNonNull(sourceBranch);
        Objects.requireNonNull(targetBranch);
        Objects.requireNonNull(outcome);
        projectsBefore = List.copyOf(projectsBefore);
        projectsAfter = List.copyOf(projectsAfter);
    }
}
