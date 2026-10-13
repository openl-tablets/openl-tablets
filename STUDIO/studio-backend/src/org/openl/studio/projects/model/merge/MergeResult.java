package org.openl.studio.projects.model.merge;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * What a merge ended with.
 *
 * <p>A merge with conflict information ended with conflicts, one without it succeeded, unless the status is given:
 * a merge stopped on request is {@link MergeResultStatus#CANCELLED}. A merge that succeeded tells what it changed.
 */
@Builder
public record MergeResult(
        MergeResultStatus status,
        MergeConflictInfo conflictInfo,
        @Nullable MergedChanges changes
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
