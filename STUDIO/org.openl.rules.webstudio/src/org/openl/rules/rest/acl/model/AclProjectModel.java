package org.openl.rules.rest.acl.model;

import javax.validation.constraints.NotNull;

import org.openl.rules.rest.model.ProjectIdModel;
import org.openl.security.acl.permission.AclRole;

public class AclProjectModel {

    private final ProjectIdModel id;
    private final String name;
    private final AclSidModel sid;

    @NotNull
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

    public AclSidModel getSid() {
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
        private AclSidModel sid;
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

        public Builder sid(AclSidModel sid) {
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
