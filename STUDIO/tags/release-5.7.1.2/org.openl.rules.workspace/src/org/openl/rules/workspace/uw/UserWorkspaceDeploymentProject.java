package org.openl.rules.workspace.uw;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;

public interface UserWorkspaceDeploymentProject extends UserWorkspaceProject, DeploymentDescriptorProject {

    ProjectDescriptor addProjectDescriptor(String name, CommonVersion version) throws ProjectException;

    ProjectDescriptor getProjectDescriptor(String name) throws ProjectException;
}
