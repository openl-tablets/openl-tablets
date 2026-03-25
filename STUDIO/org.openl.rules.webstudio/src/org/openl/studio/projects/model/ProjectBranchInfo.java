package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ProjectBranchInfo(
        @Schema(description = "Branch name")
        String name,

        @Schema(description = "Whether the branch is protected")
        @JsonProperty("protected")
        boolean protectedFlag
) {
}
