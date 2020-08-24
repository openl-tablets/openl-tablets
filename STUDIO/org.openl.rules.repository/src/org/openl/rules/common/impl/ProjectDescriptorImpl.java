package org.openl.rules.common.impl;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;

public class ProjectDescriptorImpl implements ProjectDescriptor<CommonVersion> {
    private final String repositoryId;
    private final String projectName;
    private final CommonVersion projectVersion;

    public ProjectDescriptorImpl(String repositoryId, String projectName, CommonVersion projectVersion) {
        this.repositoryId = repositoryId;
        this.projectName = projectName;
        this.projectVersion = projectVersion;
    }

    @Override
    public String getRepositoryId() {
        return repositoryId;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public CommonVersion getProjectVersion() {
        return projectVersion;
    }

}
