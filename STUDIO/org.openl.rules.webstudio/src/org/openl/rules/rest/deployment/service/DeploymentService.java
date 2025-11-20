package org.openl.rules.rest.deployment.service;

import java.util.List;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.ProjectIdModel;

public interface DeploymentService {

    /**
     * Retrieves a list of deployments based on the provided criteria query.
     *
     * @param query the criteria query to filter deployments
     * @return a list of deployments matching the criteria
     */
    List<Deployment> getDeployments(DeploymentCriteriaQuery query);

    /**
     * Deploys a project to the specified deployment ID
     *
     * @param deploymentId the ID of the deployment target
     * @param project      the project to be deployed
     * @param comment      an optional comment for the deployment
     * @throws ProjectException if there is an error during deployment
     */
    void deploy(ProjectIdModel deploymentId, RulesProject project, String comment) throws ProjectException;

}
