package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record ProjectBranchInfo(
        @Parameter(description = "Branch name")
        @NonNull String name,

        @Parameter(description = "Whether the branch is protected")
        @JsonProperty("protected")
        boolean protectedFlag,

        @Parameter(description = "Whether this is the repository base branch, which can never be deleted")
        boolean base,

        @Parameter(description = "Whether the branch's current Git tree contains this project")
        boolean containsProject
) {
}
