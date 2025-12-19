package org.openl.studio.projects.model.merge;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response containing the result of a merge operation.
 */
@Schema(description = "Response containing the result of a merge operation")
public record MergeResultResponse(

        @Schema(description = "Status of the merge operation: 'success' if merged successfully, 'conflicts' if conflicts were detected")
        MergeResultStatus status,

        @Schema(description = "List of conflict groups if conflicts were detected, empty list if merge was successful")
        List<ConflictGroup> conflictGroups

) {
}
