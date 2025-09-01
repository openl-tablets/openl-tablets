package org.openl.rules.webstudio.web.repository;

import java.util.Collection;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectDescriptor;

public record DeploymentRequest(
        String productionRepositoryId,
        String name,
        Collection<ProjectDescriptor> projectDescriptors,
        CommonUser currentUser,
        String comment
) {

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String productionRepositoryId;
            private String name;
            private Collection<ProjectDescriptor> projectDescriptors;
            private CommonUser currentUser;
            private String comment;

            private Builder() {}

            public Builder productionRepositoryId(String productionRepositoryId) {
                this.productionRepositoryId = productionRepositoryId;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder projectDescriptors(Collection<ProjectDescriptor> projectDescriptors) {
                this.projectDescriptors = projectDescriptors;
                return this;
            }

            public Builder currentUser(CommonUser currentUser) {
                this.currentUser = currentUser;
                return this;
            }

            public Builder comment(String comment) {
                this.comment = comment;
                return this;
            }

            public DeploymentRequest build() {
                return new DeploymentRequest(productionRepositoryId, name, projectDescriptors, currentUser, comment);
            }
        }

}
