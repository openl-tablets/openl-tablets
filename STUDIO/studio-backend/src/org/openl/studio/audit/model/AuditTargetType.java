package org.openl.studio.audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The kind of object an audited action was done to.")
public enum AuditTargetType {

    @Schema(description = "A user, named by the login name.")
    @JsonProperty("user")
    USER,

    @Schema(description = "A group, named by its name.")
    @JsonProperty("group")
    GROUP,

    @Schema(description = "A personal access token, named by its public id.")
    @JsonProperty("token")
    TOKEN,

    @Schema(description = "All the design or all the production repositories, named `design` or `production`.")
    @JsonProperty("root")
    ROOT,

    @Schema(description = "A repository, named by its id.")
    @JsonProperty("repository")
    REPOSITORY,

    @Schema(description = "A project, named by the repository id and the path of the project in it.")
    @JsonProperty("project")
    PROJECT
}
