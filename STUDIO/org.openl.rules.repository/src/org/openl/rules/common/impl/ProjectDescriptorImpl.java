package org.openl.rules.common.impl;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;

public class ProjectDescriptorImpl implements ProjectDescriptor<CommonVersion> {
    private String projectName;
    private CommonVersion projectVersion;

    public ProjectDescriptorImpl(String projectName, CommonVersion projectVersion) {
        this.projectName = projectName;
        this.projectVersion = projectVersion;
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
