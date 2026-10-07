package org.openl.studio.projects.model.merge;

import lombok.Builder;

@Builder
public record MergeResult(
        MergeResultStatus status,
        MergeConflictInfo conflictInfo
) {

    public MergeResult {
        status = conflictInfo == null ? MergeResultStatus.SUCCESS : MergeResultStatus.CONFLICTS;
    }
}
