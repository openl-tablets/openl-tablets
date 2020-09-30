package org.openl.rules.common.impl;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;

public class ProjectDescriptorImpl implements ProjectDescriptor<CommonVersion> {
    private final String repositoryId;
    private final String projectName;
    private final String path;
    private final String branch;
    private final CommonVersion projectVersion;

    public ProjectDescriptorImpl(String repositoryId,
        String projectName,
        String path,
        String branch,
        CommonVersion projectVersion) {
        this.repositoryId = repositoryId;
        this.projectName = projectName;
        this.path = path;
        this.branch = branch;
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
    public String getPath() {
        return path;
    }

    @Override
    public String getBranch() {
        return branch;
    }

    @Override
    public CommonVersion getProjectVersion() {
        return projectVersion;
    }

}
