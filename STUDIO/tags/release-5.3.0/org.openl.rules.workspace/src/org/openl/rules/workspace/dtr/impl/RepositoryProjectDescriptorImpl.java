package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.repository.RProjectDescriptor;
import org.openl.rules.repository.RVersion;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;

public class RepositoryProjectDescriptorImpl implements ProjectDescriptor {
    private RepositoryDeploymentDescriptorProjectImpl deploymentProject;

    private String projectName;
    private CommonVersion projectVersion;

    protected RepositoryProjectDescriptorImpl(RepositoryDeploymentDescriptorProjectImpl deploymentProject,
            RProjectDescriptor rulesProjectDescr) {
        this.deploymentProject = deploymentProject;

        projectName = rulesProjectDescr.getProjectName();
        RVersion rv = rulesProjectDescr.getProjectVersion();
        projectVersion = new CommonVersionImpl(rv.getMajor(), rv.getMinor(), rv.getRevision());
    }

    protected RepositoryProjectDescriptorImpl(RepositoryDeploymentDescriptorProjectImpl deploymentProject,
            String projectName, CommonVersion projectVersion) {
        this.deploymentProject = deploymentProject;
        this.projectName = projectName;
        this.projectVersion = projectVersion;
    }

    public void delete() {
        deploymentProject.delete(projectName);
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
