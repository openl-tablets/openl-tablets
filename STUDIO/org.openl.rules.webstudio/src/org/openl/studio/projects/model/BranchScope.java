package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Which branches of the repository a project branch listing covers.")
public enum BranchScope {

    @Schema(description = "Only the branches that hold the project — the branches it can be switched to.")
    @JsonProperty("project")
    PROJECT,

    @Schema(description = "Every branch of the repository — the branches the project can be merged with, "
            + "including branches that do not hold it yet.")
    @JsonProperty("repository")
    REPOSITORY
}
