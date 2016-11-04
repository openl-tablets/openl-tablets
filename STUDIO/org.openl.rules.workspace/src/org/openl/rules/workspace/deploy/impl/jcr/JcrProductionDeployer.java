package org.openl.rules.workspace.deploy.impl.jcr;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeployUtils;
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
        DeployID id;

        try {
            Repository repository =repositoryFactoryProxy.getRepositoryInstance(repositoryConfigName);
            id = generateDeployID(deploymentProject, repository);

                String deploymentPath = DeployUtils.DEPLOY_PATH + id.getName();
                AProject deploymentPRJ = new AProject(repository, deploymentPath);
                deploymentPRJ.lock(user);
                for (AProject p : projects) {
                    deployProject(deploymentPRJ, p, user);
                }

                // TODO: Some analogue of notifyChanges() possibly will be needed
//                deploymentPRJ.save(user);
//                rRepository.notifyChanges();
        } catch (Exception e) {
            throw new DeploymentException("Failed to deploy: " + e.getMessage(), e);
        }
        return id;
    }

    private void deployProject(AProject deployment, AProject project, WorkspaceUser user) throws ProjectException {
        AProjectFolder projectFolder = deployment.addFolder(project.getName());
        AProject copiedProject = new AProject(deployment.getRepository(), projectFolder.getArtefactPath().getStringValue(), deployment.getHistoryVersion());

        /*Update and set project revision*/
        copiedProject.update(project, user);
    }

    private DeployID generateDeployID(ADeploymentProject ddProject, Repository repository) throws RRepositoryException {
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
                if (repository != null) {
                    int version = DeployUtils.getNextDeploymentVersion(repository, ddProject);
                    sb.append('#').append(version);
                } else {
                    sb.append('#').append(projectVersion.getRevision());
                }
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
}
