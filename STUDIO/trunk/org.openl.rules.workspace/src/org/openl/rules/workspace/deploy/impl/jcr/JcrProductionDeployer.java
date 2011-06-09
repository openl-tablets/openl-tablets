package org.openl.rules.workspace.deploy.impl.jcr;

import org.openl.config.ConfigSet;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.RulesRepositoryArtefact;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;

import java.util.Collection;
import java.util.Map;

/**
 * Implementation of <code>ProductionDeployer</code> that uses <i>JCR</i> as
 * production repository.
 */
public class JcrProductionDeployer implements ProductionDeployer {
    /**
     * The user.
     */
    private final WorkspaceUser user;

    private Map<String, Object> config;

    public JcrProductionDeployer(WorkspaceUser user) {
        this.user = user;
    }

    private void copyProperties(AProjectArtefact newArtefact, RulesRepositoryArtefact artefact) throws RRepositoryException {
        try {
            newArtefact.setEffectiveDate(artefact.getEffectiveDate());
            newArtefact.setExpirationDate(artefact.getExpirationDate());
            newArtefact.setLineOfBusiness(artefact.getLineOfBusiness());

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
            if (ProductionRepositoryFactoryProxy.getConfig() == null) {
                ConfigSet configSet = new ConfigSet();
                configSet.addProperties(config);
                ProductionRepositoryFactoryProxy.setConfig(configSet);
            }

            RProductionRepository rRepository = ProductionRepositoryFactoryProxy.getRepositoryInstance();

            if (rRepository.hasDeploymentProject(id.getName())) {
                alreadyDeployed = true;
            } else {

                FolderAPI deployment = rRepository.createDeploymentProject(id.getName());

                AProject deploymentPRJ = new AProject(deployment);
                deploymentPRJ.lock(user);
                for (AProject p : projects) {
                    deployProject(deploymentPRJ, p);
                }
                
                copyProperties(deploymentPRJ, deploymentProject);

                deploymentPRJ.checkIn(user);
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
    
    private void deployProject(AProject deployment, AProject project) throws RRepositoryException,
            ProjectException {
        FolderAPI rProject = deployment.addFolder(project.getName()).getAPI();
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

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

}
