package org.openl.studio.projects.model.merge;

import java.util.Objects;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

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
        CheckMergeStatus status
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sourceBranch;
        private String targetBranch;
        private CheckMergeStatus status;

        private Builder() {
        }

        public Builder sourceBranch(String sourceBranch) {
            this.sourceBranch = sourceBranch;
            return this;
        }

        public Builder targetBranch(String targetBranch) {
            this.targetBranch = targetBranch;
            return this;
        }

        public Builder status(CheckMergeStatus status) {
            this.status = status;
            return this;
        }

        public CheckMergeResult build() {
            return new CheckMergeResult(Objects.requireNonNull(sourceBranch),
                    Objects.requireNonNull(targetBranch),
                    Objects.requireNonNull((status)));
        }
    }
}
