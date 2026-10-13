package org.openl.studio.projects.model.merge;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Response containing the result of a merge operation.
 */
@Schema(description = "Response containing the result of a merge operation")
public record MergeResultResponse(

        @Parameter(description = "Status of the merge operation: 'success' if merged successfully, 'conflicts' if conflicts were detected, 'cancelled' if the merge was stopped on request before it committed anything")
        MergeResultStatus status,

        @Parameter(description = "List of conflict groups if conflicts were detected, empty list otherwise")
        List<ConflictGroup> conflictGroups,

        @Parameter(description = "What a successful merge changed in the target branch; absent otherwise, and when it cannot be told")
        @Nullable MergeSummaryView summary

) {
}
