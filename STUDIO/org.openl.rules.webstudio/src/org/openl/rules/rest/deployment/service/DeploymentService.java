package org.openl.rules.rest.deployment.service;

import java.util.List;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.model.ProjectIdModel;

public interface DeploymentService {

    List<Deployment> getDeployments(DeploymentCriteriaQuery query);

    void deploy(ProjectIdModel deploymentId, RulesProject project, String comment) throws ProjectException;

}
