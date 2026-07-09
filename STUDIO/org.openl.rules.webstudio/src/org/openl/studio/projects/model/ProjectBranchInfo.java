package org.openl.studio.projects.model;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ProjectBranchInfo(
        @Schema(description = "Branch name")
        String name,

        @Schema(description = "Whether the branch is protected")
        @JsonProperty("protected")
        boolean protectedFlag,

        @Schema(description = "Whether this is the repository base branch, which can never be deleted")
        boolean base,

        @Schema(description = "Whether the current user is eligible to bypass protection (Manager role + global setting enabled)")
        boolean bypassEligible,

        @Schema(description = "The branch's last (tip) commit")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LastCommit lastCommit
) {

    @Builder
    public record LastCommit(
            @Schema(description = "Commit author")
            String author,

            @Schema(description = "Commit time")
            ZonedDateTime modifiedAt,

            @Schema(description = "Commit message")
            String message,

            @Schema(description = "Commit revision (SHA)")
            String revision
    ) {
    }
}
