package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.repository.RProjectDescriptor;
import org.openl.rules.repository.RVersion;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;

public class RepositoryProjectDescriptorImpl implements ProjectDescriptor {
    private RepositoryDeploymentDescriptorProjectImpl deploymentProject;

    private String projectName;
    private RepositoryProjectVersionImpl projectVersion;
    
    protected RepositoryProjectDescriptorImpl(RepositoryDeploymentDescriptorProjectImpl deploymentProject, RProjectDescriptor rulesProjectDescr) {
        this.deploymentProject = deploymentProject;
        
        projectName = rulesProjectDescr.getProjectName();
        RVersion rv = rulesProjectDescr.getProjectVersion();
        projectVersion = new RepositoryProjectVersionImpl(rv.getMajor(), rv.getMinor(), rv.getRevision(), null);
    }
    
    protected RepositoryProjectDescriptorImpl(RepositoryDeploymentDescriptorProjectImpl deploymentProject, String projectName, RepositoryProjectVersionImpl projectVersion) {
        this.deploymentProject = deploymentProject;
        this.projectName = projectName;
        this.projectVersion = projectVersion;
    }
    
    public String getProjectName() {
        return projectName;
    }

    public ProjectVersion getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(ProjectVersion version) throws ProjectException {
        projectVersion = new RepositoryProjectVersionImpl(version.getMajor(), version.getMinor(), version.getRevision(), null);
    }

    public void delete() {
        deploymentProject.delete(projectName);
    }
}
