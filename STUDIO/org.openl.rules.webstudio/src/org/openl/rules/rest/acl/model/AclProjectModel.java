package org.openl.rules.rest.acl.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.rest.model.ProjectIdModel;
import org.openl.security.acl.permission.AclRole;

@Schema(description = "ACL Project Model")
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

    @NotNull
    @JsonView({AclView.Project.class, AclView.Sid.class})
    @Parameter(description = "Role")
    private final AclRole role;

    private AclProjectModel(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.role = builder.role;
        this.sid = builder.sid;
    }

    public ProjectIdModel getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AclSubject getSid() {
        return sid;
    }

    public AclRole getRole() {
        return role;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProjectIdModel id;
        private String name;
        private AclSubject sid;
        private AclRole role;

        private Builder() {
        }

        public Builder id(ProjectIdModel id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder sid(AclSubject sid) {
            this.sid = sid;
            return this;
        }

        public Builder role(AclRole role) {
            this.role = role;
            return this;
        }

        public AclProjectModel build() {
            return new AclProjectModel(this);
        }
    }

}
