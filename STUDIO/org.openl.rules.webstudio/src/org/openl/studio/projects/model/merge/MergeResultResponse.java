package org.openl.studio.projects.model.merge;

import java.util.List;

public record MergeResultResponse(

        MergeResultStatus status,
        List<ConflictGroup> conflictGroups

) {
}
