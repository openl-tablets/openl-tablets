package org.openl.studio.projects.model.merge;

import java.util.List;

/**
 * Response after resolving conflicts.
 *
 * @param status the status of the conflict resolution
 * @param resolvedFiles list of files that were successfully resolved
 */
public record ResolveConflictsResponse(

        ConflictResolutionStatus status,
        List<String> resolvedFiles

) {
}
