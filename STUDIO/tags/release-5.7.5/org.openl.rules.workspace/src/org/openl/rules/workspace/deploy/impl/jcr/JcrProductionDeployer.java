package org.openl.rules.workspace.deploy.impl.jcr;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.RulesRepositoryArtefact;
import org.openl.rules.common.ValueType;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;

import java.util.Collection;

/**
 * Implementation of <code>ProductionDeployer</code> that uses <i>JCR</i> as
 * production repository.
 */
public class JcrProductionDeployer implements ProductionDeployer {
    /**
     * The user.
     */
    private final WorkspaceUser user;

    public JcrProductionDeployer(WorkspaceUser user) {
        this.user = user;
    }

    private void copyProperties(ArtefactAPI newArtefact, RulesRepositoryArtefact artefact) throws RRepositoryException {
        try {
            newArtefact.addProperty(ArtefactProperties.PROP_EFFECTIVE_DATE, ValueType.DATE, artefact.getEffectiveDate());
            newArtefact.addProperty(ArtefactProperties.PROP_EXPIRATION_DATE, ValueType.DATE, artefact.getExpirationDate());
            newArtefact.addProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS, ValueType.STRING, artefact.getLineOfBusiness());

            newArtefact.setProps(artefact.getProps());
        } catch (PropertyException e) {
            throw new RRepositoryException("", e);
        }
    }

    /**
     * Deploys a collection of <code>Project</code>s to the production
     * repository with given ID. Overwrites deployment with given <i>id</i> if
     * it already exists.
     *
     * @param projects projects to deploy
     * @return <code>id</code> parameter
     * @throws DeploymentException if any deployment error occures
     */

    public synchronized DeployID deploy(ADeploymentProject deploymentProject, DeployID id, Collection<AProject> projects) throws DeploymentException {

        boolean alreadyDeployed = false;
        try {
            RProductionRepository rRepository = ProductionRepositoryFactoryProxy.getRepositoryInstance();

            if (rRepository.hasDeploymentProject(id.getName())) {
                alreadyDeployed = true;
            } else {

                FolderAPI deployment = rRepository.createDeploymentProject(id.getName());

                for (AProject p : projects) {
                    deployProject(deployment, p);
                }
                
                copyProperties(deployment, deploymentProject);

                deployment.commit(user, 0, 0, 1);
            }
        } catch (Exception e) {
            throw new DeploymentException("Failed to deploy: " + e.getMessage(), e);
        }

        if (alreadyDeployed) {
            throw new DeploymentException("Project is already deployed to production repository, id: " + id.getName(),
                    null);
        }

        return id;
    }

    public synchronized DeployID deploy(ADeploymentProject deploymentProject, Collection<AProject> projects) throws DeploymentException {
        String idKey = generatedDeployID(projects);
        return deploy(deploymentProject, new DeployID(idKey), projects);
    }
    
    private void deployProject(FolderAPI deployment, AProject project) throws RRepositoryException,
            ProjectException {
        FolderAPI rProject = deployment.addFolder(project.getName());
        AProject copiedProject = new AProject(rProject);
        copiedProject.update(project, user, project.getVersion().getMajor(), project.getVersion().getMinor());
    }

    private String generatedDeployID(Collection<AProject> projects) {
        StringBuilder name = new StringBuilder();
        for (AProject p : projects) {
            name.append(p.getName());
            if (p.getVersion() != null) {
                name.append('-').append(p.getVersion().getVersionName());
            }
            name.append('_');
        }
        name.append(System.currentTimeMillis());
        return name.toString();
    }
}
