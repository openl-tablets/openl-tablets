package org.openl.studio.projects.model.merge;

import java.util.Objects;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.git.MergeConflictDetails;

/**
 * Contains information about merge conflicts occurred during merge operation.
 *
 * @param details         details about merge conflicts
 * @param project         project in which merge operation was performed
 * @param mergeBranchFrom source branch name of the merge operation
 * @param mergeBranchTo   target branch name of the merge operation
 * @param currentBranch   current branch name of the project
 */
@Builder
public record MergeConflictInfo(
        @NotNull
        MergeConflictDetails details,

        @NotNull
        @JsonIgnore
        RulesProject project,
        String mergeBranchFrom,
        String mergeBranchTo,
        @JsonIgnore
        String currentBranch
) {

    public MergeConflictInfo {
        Objects.requireNonNull(details);
        Objects.requireNonNull(project);
        if (mergeBranchFrom != null || mergeBranchTo != null || currentBranch != null) {
            Objects.requireNonNull(mergeBranchFrom, "mergeBranchFrom must be initialized");
            Objects.requireNonNull(mergeBranchTo, "mergeBranchTo must be initialized");
            Objects.requireNonNull(currentBranch, "currentBranch must be initialized");
        }
    }

    public boolean isMerging() {
        return mergeBranchFrom != null && mergeBranchTo != null;
    }

    public boolean isExportOperation() {
        return mergeBranchFrom != null && mergeBranchFrom.equals(currentBranch);
    }

    public String getRepositoryId() {
        return project.getRepository().getId();
    }
}
