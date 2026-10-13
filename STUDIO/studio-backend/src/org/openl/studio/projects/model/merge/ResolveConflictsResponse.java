package org.openl.studio.projects.model.merge;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Response after resolving conflicts.
 *
 * @param status        the status of the conflict resolution
 * @param resolvedFiles list of files that were successfully resolved
 * @param summary       what the merge changed in the target branch, or {@code null} for a save conflict and when
 *                      it cannot be told
 */
@Schema(description = "Response containing the result of conflict resolution operation")
public record ResolveConflictsResponse(

        @Schema(description = "Status of the conflict resolution: 'success' if all conflicts resolved, 'partial' if some remain, 'failed' if resolution failed")
        ConflictResolutionStatus status,

        @Schema(description = "List of file paths that were successfully resolved")
        List<String> resolvedFiles,

        @Parameter(description = "What the merge changed in the target branch, the workbooks merged automatically included; absent for a save conflict, and when it cannot be told")
        @Nullable MergeSummaryView summary

) {
}
