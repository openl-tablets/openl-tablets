package org.openl.studio.projects.model.merge;

import java.util.Objects;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing merge status between source and target branches")
public record CheckMergeResult(
        @Schema(description = "Source branch name")
        @NotNull
        String sourceBranch,
        @Schema(description = "Target branch name")
        @NotNull
        String targetBranch,
        @Schema(description = "Merge status between source and target branches")
        @NotNull
        CheckMergeStatus status,
        @Schema(description = "Whether the current user may perform this merge")
        boolean canMerge,
        @Schema(description = "What prevents the merge; absent when the user may perform it")
        MergeBlockedBy blockedBy
) {
    public CheckMergeResult {
        Objects.requireNonNull(sourceBranch);
        Objects.requireNonNull(targetBranch);
        Objects.requireNonNull(status);
    }
}
