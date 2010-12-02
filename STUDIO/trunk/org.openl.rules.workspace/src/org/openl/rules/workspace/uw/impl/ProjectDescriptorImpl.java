package org.openl.rules.workspace.uw.impl;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;

public class ProjectDescriptorImpl implements ProjectDescriptor {
    private String projectName;
    private CommonVersion projectVersion;

    public ProjectDescriptorImpl(String projectName, CommonVersion projectVersion) {
        this.projectName = projectName;
        this.projectVersion = projectVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public CommonVersion getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(CommonVersion version) throws ProjectException {
        projectVersion = new CommonVersionImpl(version);
    }

}
