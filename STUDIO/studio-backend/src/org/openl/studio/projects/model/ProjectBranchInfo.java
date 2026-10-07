package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record ProjectBranchInfo(
        @Parameter(description = "Branch name")
        @NonNull String name,

        @Parameter(description = "Present only when the branch is protected")
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        @JsonProperty("protected")
        boolean protectedFlag,

        @Parameter(description = "Present only for the repository base branch, which can never be deleted")
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        boolean base
) {
}
