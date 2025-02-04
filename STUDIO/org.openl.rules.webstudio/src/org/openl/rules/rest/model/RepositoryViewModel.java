package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.rules.rest.acl.model.AclRepositoryId;

public class RepositoryViewModel {

    @Parameter(description = "Repository unique identifier for ACL", required = true)
    private final AclRepositoryId aclId;

    @Parameter(description = "Repository unique identifier. Used as identifier in all requests", required = true)
    private final String id;

    @Parameter(description = "Repository display name", required = true)
    private final String name;

    private RepositoryViewModel(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.aclId = builder.aclId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AclRepositoryId getAclId() {
        return aclId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private AclRepositoryId aclId;

        protected Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder aclId(AclRepositoryId aclId) {
            this.aclId = aclId;
            return this;
        }

        public RepositoryViewModel build() {
            return new RepositoryViewModel(this);
        }
    }

}
