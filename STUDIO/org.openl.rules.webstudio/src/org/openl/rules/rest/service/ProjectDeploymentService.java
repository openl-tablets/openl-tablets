package org.openl.rules.rest.service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.webstudio.web.repository.DeploymentProjectItem;

import java.io.IOException;
import java.util.List;

public interface ProjectDeploymentService {
    List<DeploymentProjectItem> getDeploymentProjectItems(AProject project,
                                                          String deployRepoName) throws ProjectException;

    AProject getDeployedProject(AProject wsProject,
                                String deployConfigName,
                                String repositoryConfigName) throws IOException;

    ADeploymentProject update(String deploymentName, AProject project, String repoName);
}
