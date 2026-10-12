package org.openl.studio.projects.model.merge;

import lombok.Builder;

/**
 * What a merge ended with.
 *
 * <p>A merge with conflict information ended with conflicts, one without it succeeded, unless the status is given:
 * a merge stopped on request is {@link MergeResultStatus#CANCELLED}.
 */
@Builder
public record MergeResult(
        MergeResultStatus status,
        MergeConflictInfo conflictInfo
) {

    public MergeResult {
        if (status == null) {
            status = conflictInfo == null ? MergeResultStatus.SUCCESS : MergeResultStatus.CONFLICTS;
        }
    }

    /**
     * The result of a merge stopped on request: the branches are as the merge found them.
     */
    public static MergeResult cancelled() {
        return MergeResult.builder().status(MergeResultStatus.CANCELLED).build();
    }
}
