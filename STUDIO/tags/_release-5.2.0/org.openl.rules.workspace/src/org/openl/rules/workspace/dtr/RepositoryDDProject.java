package org.openl.rules.workspace.dtr;

import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.ProjectException;

public interface RepositoryDDProject extends DeploymentDescriptorProject, RepositoryProject {
    void update(DeploymentDescriptorProject srcArtefact) throws ProjectException;
}
