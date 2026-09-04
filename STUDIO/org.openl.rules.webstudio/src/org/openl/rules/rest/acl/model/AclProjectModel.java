package org.openl.rules.rest.acl.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import org.openl.security.acl.permission.AclRole;
import org.openl.studio.projects.model.ProjectIdModel;

@Schema(description = "ACL Project Model")
@Getter
@Builder
public class AclProjectModel {

    @JsonView(AclView.Project.class)
    @Parameter(description = "Project ID")
    private final ProjectIdModel id;

    @JsonView(AclView.Project.class)
    @Parameter(description = "Project Name")
    private final String name;

    @JsonView(AclView.Sid.class)
    @Parameter(description = "SID")
    private final AclSubject sid;

    @JsonView(AclView.Sid.class)
    @Parameter(description = "ACL rule source. REPOSITORY entries are inherited from repository ACL and are read-only on the project.")
    private final AclProjectSource source;

    @NotNull
    @JsonView({AclView.Project.class, AclView.Sid.class})
    @Parameter(description = "Role")
    private final AclRole role;

    public enum AclProjectSource {
        @JsonProperty("project")
        PROJECT,
        @JsonProperty("repository")
        REPOSITORY
    }

}
