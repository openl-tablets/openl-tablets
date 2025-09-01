package org.openl.rules.common.impl;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.project.abstraction.RulesProject;

public record ProjectDescriptorImpl(
        String repositoryId,
        String projectName,
        String path,
        String branch,
        CommonVersion projectVersion
) implements ProjectDescriptor {

    public static Builder builder() {
        return new Builder();
    }

    public static ProjectDescriptor from(RulesProject project) {
        return ProjectDescriptorImpl.builder()
                .repositoryId(project.getRepository().getId())
                .projectName(project.getBusinessName())
                .path(project.getRealPath())
                .projectVersion(project.getVersion())
                .branch(project.getBranch())
                .build();
    }

    public static class Builder {
        private String repositoryId;
        private String projectName;
        private String path;
        private String branch;
        private CommonVersion projectVersion;

        private Builder() {
        }

        public Builder repositoryId(String repositoryId) {
            this.repositoryId = repositoryId;
            return this;
        }

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder branch(String branch) {
            this.branch = branch;
            return this;
        }

        public Builder projectVersion(CommonVersion projectVersion) {
            this.projectVersion = projectVersion;
            return this;
        }

        public ProjectDescriptorImpl build() {
            return new ProjectDescriptorImpl(repositoryId, projectName, path, branch, projectVersion);
        }
    }

}
