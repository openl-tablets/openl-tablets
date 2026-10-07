package org.openl.studio.projects.model.merge;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response after resolving conflicts.
 *
 * @param status the status of the conflict resolution
 * @param resolvedFiles list of files that were successfully resolved
 */
@Schema(description = "Response containing the result of conflict resolution operation")
public record ResolveConflictsResponse(

        @Schema(description = "Status of the conflict resolution: 'success' if all conflicts resolved, 'partial' if some remain, 'failed' if resolution failed")
        ConflictResolutionStatus status,

        @Schema(description = "List of file paths that were successfully resolved")
        List<String> resolvedFiles

) {
}
