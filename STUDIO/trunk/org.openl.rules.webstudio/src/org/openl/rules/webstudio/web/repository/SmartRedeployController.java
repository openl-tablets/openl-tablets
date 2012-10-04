package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 *
 * @author Aleh Bykhavets
 *
 */
@ManagedBean
@RequestScoped
public class SmartRedeployController {

    private final Log log = LogFactory.getLog(SmartRedeployController.class);

    /** A controller which contains pre-built UI object tree. */
    @ManagedProperty(value="#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    @ManagedProperty(value="#{deploymentManager}")
    private DeploymentManager deploymentManager;

    @ManagedProperty(value="#{productionRepositoryConfigManagerFactory}")
    private ConfigurationManagerFactory productionConfigManagerFactory;

    private List<DeploymentProjectItem> items;

    private String repositoryConfigName;

    public synchronized List<DeploymentProjectItem> getItems() {
        AProject project = getSelectedProject();
        if (project == null) {
            items = null;
            return null;
        }

        if (items == null) {
            items = getItems4Project(project);
        }
        return items;
    }
    
    public synchronized boolean isProjectHasSelectedItems() {
        if (items == null) {
            return false;
        }
        
        for (DeploymentProjectItem item : items) {
            if (item.isSelected()) {
                return true;
            }
        }
        
        return false;
    }

    private List<DeploymentProjectItem> getItems4Project(AProject project) {
        String projectName = project.getName();
        UserWorkspace workspace = RepositoryUtils.getWorkspace();

        List<DeploymentProjectItem> result = new LinkedList<DeploymentProjectItem>();

        // FIXME take latest deployment projects from DTR not from user scope
        // get all deployment projects
        List<TreeNode> nodes = repositoryTreeState.getDeploymentRepository().getChildNodes();
        for (TreeNode node : nodes) {
            AProjectArtefact artefact = node.getData();
            if (!(artefact instanceof ADeploymentProject)) {
                continue; // should never happen
            }

            ADeploymentProject deploymentProject = (ADeploymentProject) artefact;
            if (deploymentProject.isDeleted()) {
                continue; // don't check marked for deletion projects
            }

            ADeploymentProject latestDeploymentVersion = deploymentProject;
            if (deploymentProject.isOpenedOtherVersion()) {
                try {
                    latestDeploymentVersion = workspace.getDesignTimeRepository().getDDProject(
                            deploymentProject.getName());
                } catch (RepositoryException e) {
                    log.error("Failed to get latest version for deployment project '" + deploymentProject.getName()
                            + "'", e);
                }
            }

            ProjectDescriptor projectDescriptor = null;

            // check all descriptors
            // we are interested in all Deployment projects that has the project
            Collection<ProjectDescriptor> descriptors = latestDeploymentVersion.getProjectDescriptors();
            for (ProjectDescriptor descr : descriptors) {
                if (projectName.equals(descr.getProjectName())) {
                    projectDescriptor = descr;
                    break;
                }
            }

            if (projectDescriptor == null) {
                continue;
            }

            // create new item
            DeploymentProjectItem item = new DeploymentProjectItem();
            item.setName(deploymentProject.getName());

            CommonVersionImpl descrVersion = new CommonVersionImpl(projectDescriptor.getProjectVersion());
            int cmp = descrVersion.compareTo(project.getVersion());

            if (cmp == 0) {
                item.setDisabled(true);
                item.setMessages("Up to date");
            } else if (cmp < 0) {
                if (deploymentProject.isOpenedForEditing()) {
                    // prevent loosing of user's changes
                    item.setDisabled(true);
                    item.setMessages("Opened for Editing");
                    item.setStyleForMessages(UiConst.STYLE_WARNING);
                    item.setStyleForName(UiConst.STYLE_WARNING);
                } else if (deploymentProject.isLocked()) {
                    // won't be able to modify anyway
                    item.setDisabled(true);
                    item.setMessages("Locked by other user");
                    item.setStyleForMessages(UiConst.STYLE_WARNING);
                    item.setStyleForName(UiConst.STYLE_WARNING);
                } else {
                    DependencyChecker checker = new DependencyChecker();
                    // check against latest version of the deployment project
                    checker.addProjects(latestDeploymentVersion);
                    // overwrite settings
                    checker.addProject(project);
                    if (checker.check()) {
                        item.setMessages("Can be updated to " + project.getVersion().getVersionName() + " from "
                                + descrVersion.getVersionName());
                    } else {
                        item.setMessages("Has dependency conflict!");
                        item.setStyleForMessages(UiConst.STYLE_ERROR);
                    }
                }
            } else {
                item.setDisabled(true);
                item.setMessages("Deployment uses newer version " + descrVersion.getVersionName());
            }

            result.add(item);
        }

        if (!workspace.hasDDProject(projectName)) {
            // there is no deployment project with the same name...
            DeploymentProjectItem item = new DeploymentProjectItem();
            item.setName(projectName);
            item.setMessages("Create deployment project");
            item.setStyleForName(UiConst.STYLE_WARNING);

            // place it first
            result.add(0, item);
        }

        return result;
    }

    private AProject getSelectedProject() {
        AProjectArtefact artefact = repositoryTreeState.getSelectedNode().getData();
        if (artefact instanceof AProject) {
            return (AProject) artefact;
        }
        return null;
    }

    public String redeploy() {
        AProject project = getSelectedProject();
        if (project == null) {
            return UiConst.OUTCOME_FAILURE;
        }

        List<ADeploymentProject> successfulyUpdated = new LinkedList<ADeploymentProject>();
        // update selected deployment projects
        for (DeploymentProjectItem item : items) {
            if (!item.isSelected()) {
                continue;
            }

            ADeploymentProject deploymentProject = update(item.getName(), project);
            if (deploymentProject != null) {
                // OK, it was updated
                successfulyUpdated.add(deploymentProject);
            }
        }

        // redeploy takes more time
        ConfigurationManager productionConfig = productionConfigManagerFactory.getConfigurationManager(repositoryConfigName);
        RepositoryConfiguration repo = new RepositoryConfiguration(repositoryConfigName, productionConfig);
        for (ADeploymentProject deploymentProject : successfulyUpdated) {
            try {
                DeployID id = deploymentManager.deploy(deploymentProject, repositoryConfigName);
                String message = String.format("Project '%s' successfully deployed with id '%s' to repository '%s'", 
                        project.getName(), id.getName(), repo.getName());
                FacesUtils.addInfoMessage(message);
            } catch (Exception e) {
                String msg = String.format("Failed to deploy '%s' to repository '%s'", project.getName(), repo.getName());
                log.error(msg, e);
                FacesUtils.addErrorMessage(msg, e.getMessage());
            }
        }

        return UiConst.OUTCOME_SUCCESS;
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void setProductionConfigManagerFactory(ConfigurationManagerFactory productionConfigManagerFactory) {
        this.productionConfigManagerFactory = productionConfigManagerFactory;
    }

    private ADeploymentProject update(String deploymentName, AProject project) {
        UserWorkspace workspace = RepositoryUtils.getWorkspace();
        try {
            if (deploymentName.equals(project.getName())) {
                // the same name
                if (!workspace.hasDDProject(deploymentName)) {
                    // create if absent
                    workspace.createDDProject(deploymentName);
                }
            }

            // get latest version
            ADeploymentProject deploymentProject = workspace.getDDProject(deploymentName);

            if (deploymentProject.isLocked()) {
                // someone else is locked it while we were thinking
                FacesUtils.addWarnMessage("Deployment project '" + deploymentName + "' is locked by other user");
                return null;
            } else {
                deploymentProject.edit();

                // rewrite project->version
                deploymentProject.addProjectDescriptor(project.getName(), project.getVersion());

                deploymentProject.save();

                FacesUtils.addInfoMessage("Deployment project '" + deploymentName + "' successfully updated");
                return deploymentProject;
            }
        } catch (ProjectException e) {
            String msg = "Failed to update deployment project '" + deploymentName + "'";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg);
        }

        return null;
    }

    public String getRepositoryConfigName() {
        return repositoryConfigName;
    }

    public void setRepositoryConfigName(String repositoryConfigName) {
        this.repositoryConfigName = repositoryConfigName;
    }
    
    public Collection<RepositoryConfiguration> getRepositories() {
        List<RepositoryConfiguration> repos = new ArrayList<RepositoryConfiguration>();
        Collection<String> repositoryConfigNames = deploymentManager.getRepositoryConfigNames();
        for (String configName : repositoryConfigNames) {
            ConfigurationManager productionConfig = productionConfigManagerFactory.getConfigurationManager(configName);
            RepositoryConfiguration config = new RepositoryConfiguration(configName, productionConfig);
            repos.add(config);
        }

        Collections.sort(repos, RepositoryConfiguration.COMPARATOR);
        return repos;
    }
}
