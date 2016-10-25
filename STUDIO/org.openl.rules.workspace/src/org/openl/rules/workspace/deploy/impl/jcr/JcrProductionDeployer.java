package org.openl.rules.workspace.deploy.impl.jcr;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.RulesRepositoryArtefact;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;

/**
 * Implementation of <code>ProductionDeployer</code> that uses <i>JCR</i> as
 * production repository.
 */
public class JcrProductionDeployer implements ProductionDeployer {
    private final ProductionRepositoryFactoryProxy repositoryFactoryProxy;
    private final String repositoryConfigName;
    private boolean deploymentFormatOld = false;

    public JcrProductionDeployer(ProductionRepositoryFactoryProxy repositoryFactoryProxy, String repositoryConfigName) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
        this.repositoryConfigName = repositoryConfigName;
    }

    public JcrProductionDeployer(ProductionRepositoryFactoryProxy repositoryFactoryProxy, String repositoryConfigName, boolean deploymentFormatOld) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
        this.repositoryConfigName = repositoryConfigName;
        this.deploymentFormatOld = deploymentFormatOld;
    }

    private void copyProperties(AProjectArtefact newArtefact, RulesRepositoryArtefact artefact) throws RRepositoryException {
        try {
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

    public synchronized DeployID deploy(ADeploymentProject deploymentProject, Collection<AProject> projects, WorkspaceUser user) throws DeploymentException {
        DeployID id = generateDeployID(deploymentProject);

        boolean alreadyDeployed = false;
        try {
            String deployPath = repositoryFactoryProxy.getDeployPath(repositoryConfigName);
            Repository repository =repositoryFactoryProxy.getRepositoryInstance(repositoryConfigName);

            if (!repository.list(deployPath + "/" + id.getName()).isEmpty() || !repository.list(deployPath + "/" + getOtherDeploymentProjectName(deploymentProject)).isEmpty()) {
                alreadyDeployed = true;
            } else {
                String deploymentPath = deployPath + "/" + id.getName();
                AProject deploymentPRJ = new AProject(repository, deploymentPath);
                deploymentPRJ.lock(user);
                for (AProject p : projects) {
                    deployProject(deploymentPRJ, p, user);
                }

                copyProperties(deploymentPRJ, deploymentProject);

                // TODO: Some analogue of notifyChanges() possibly will be needed
//                deploymentPRJ.save(user);
//                rRepository.notifyChanges();
            }
        } catch (Exception e) {
            throw new DeploymentException("Failed to deploy: " + e.getMessage(), e);
        }

        if (alreadyDeployed) {
            throw new DeploymentException("Configuration is already deployed to production repository, id: " + id.getName(),
                    null);
        }

        return id;
    }

    /**
     * Checks if deploymentConfiguration is already deployed to this production
     * repository.
     * 
     * @param deployConfiguration deploy configuration for project
     *            trying to deploy
     * @return true if deploymentConfiguration with its id already exists in
     *         production repository
     * @throws RRepositoryException if cannot get info from repository for some
     *             reason
     */
    @Override
    public synchronized boolean hasDeploymentProject(ADeploymentProject deployConfiguration) throws RRepositoryException {
        Repository repository = repositoryFactoryProxy.getRepositoryInstance(repositoryConfigName);
        String deployPath = repositoryFactoryProxy.getDeployPath(repositoryConfigName);
        DeployID id = generateDeployID(deployConfiguration);
        String otherPossibleID = this.getOtherDeploymentProjectName(deployConfiguration);

        return !repository.list(deployPath + "/" + id.getName()).isEmpty() || !repository.list(deployPath + "/" + otherPossibleID).isEmpty();
    }

    private void deployProject(AProject deployment, AProject project, WorkspaceUser user) throws ProjectException {
        AProjectFolder projectFolder = deployment.addFolder(project.getName());
        AProject copiedProject = new AProject(deployment.getRepository(), projectFolder.getArtefactPath().getStringValue(), deployment.getHistoryVersion());

        /*Update and set project revision*/
        copiedProject.update(project, user, project.getVersion().getRevision());
    }

    private DeployID generateDeployID(ADeploymentProject ddProject) {
        StringBuilder sb = new StringBuilder(ddProject.getName());
        ProjectVersion projectVersion = ddProject.getVersion();
        if (projectVersion != null) {
            if (deploymentFormatOld) {
                if (isOldFormatVersion(projectVersion)) {
                    sb.append('#').append(projectVersion.getVersionName());
                } else {
                    sb.append('#').append("0.0.").append(projectVersion.getVersionName());
                }
            } else {
                sb.append('#').append(projectVersion.getRevision());
            }
        }
        return new DeployID(sb.toString());
    }

    @Override
    public void destroy() throws RRepositoryException {
        if (repositoryFactoryProxy != null) {
            repositoryFactoryProxy.releaseRepository(repositoryConfigName);
        }
    }

    private boolean isOldFormatVersion (ProjectVersion version) {
        return version.getMajor() != CommonVersion.MAX_MM_INT && version.getMajor() != -1;
    }

    /**
     * Method for generating other possible version of deployment ID (e.g if we have id like projectName#1 then we will have id like projectName#0.0.1)
     * 
     * @param deployConfiguration deploy configuration for project
     *            trying to deploy
     * @return other possible version of deployment ID
     */
    private String getOtherDeploymentProjectName(ADeploymentProject deployConfiguration) {
        StringBuilder sb = new StringBuilder(deployConfiguration.getName());
        ProjectVersion projectVersion = deployConfiguration.getVersion();
        if (projectVersion != null) {
            if (!deploymentFormatOld) {
                if (isOldFormatVersion(projectVersion)) {
                    sb.append('#').append(projectVersion.getVersionName());
                } else {
                    sb.append('#').append("0.0.").append(projectVersion.getVersionName());
                }
            } else {
                sb.append('#').append(projectVersion.getRevision());
            }
        }
        return sb.toString();
    }
}
