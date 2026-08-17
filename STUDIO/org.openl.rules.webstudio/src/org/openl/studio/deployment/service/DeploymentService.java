package org.openl.studio.deployment.service;

import java.util.List;
import java.util.Optional;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.webstudio.web.repository.cache.CachedProjectVersion;
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

    /**
     * Finds the design repository revision a deployed project was built from.
     *
     * <p>The revision is recognized by content, and the design repository is indexed in the background, so the
     * answer is empty while a freshly created revision is still being indexed. It stays empty for a project
     * deployed from a design repository this OpenL Studio does not have.
     *
     * @param deployedProject a project inside a deployment
     * @return the design revision, or empty when none matches
     */
    Optional<CachedProjectVersion> findDesignRevision(AProject deployedProject);

}
