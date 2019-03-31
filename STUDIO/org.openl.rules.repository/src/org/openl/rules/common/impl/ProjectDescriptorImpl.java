package org.openl.rules.common.impl;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.RVersion;

public class ProjectDescriptorImpl implements ProjectDescriptor {
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

    public void setProjectVersion(RVersion version) {
        projectVersion = new CommonVersionImpl(version);
    }

    @Override
    public void setProjectVersion(CommonVersion version) throws ProjectException {
        projectVersion = new CommonVersionImpl(version);
    }

}
