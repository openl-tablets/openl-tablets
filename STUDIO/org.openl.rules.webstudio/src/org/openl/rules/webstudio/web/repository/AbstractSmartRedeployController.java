package org.openl.rules.webstudio.web.repository;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.CREATE_DEPLOYMENT;
import static org.openl.rules.security.Privileges.EDIT_DEPLOYMENT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.cache.ProjectVersionCacheManager;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.PropertyResolver;

import com.thoughtworks.xstream.XStreamException;

public abstract class AbstractSmartRedeployController {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * A controller which contains pre-built UI object tree.
     */
    @Autowired
    RepositoryTreeState repositoryTreeState;

    @Autowired
    private ProductionRepositoriesTreeController productionRepositoriesTreeController;

    @Autowired
    private DeploymentManager deploymentManager;

    @Autowired
    private ProjectVersionCacheManager projectVersionCacheManager;

    @Autowired
    private volatile ProjectDescriptorArtefactResolver projectDescriptorResolver;

    @Autowired
    @Qualifier("deployConfigRepositoryComments")
    private Comments deployConfigRepoComments;

    @Autowired
    private PropertyResolver propertyResolver;

    volatile UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());

    List<DeploymentProjectItem> items;

    private String repositoryConfigName;

    AProject currentProject;

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }

    public synchronized List<DeploymentProjectItem> getItems() {
        AProject project = getSelectedProject();
        if (project == null || project != currentProject || isSupportsBranches() && project.getVersion() == null) {
            reset();
            return null;
        }

        if (items == null) {
            items = getItems4Project(project, getRepositoryConfigName());
        }
        return items;
    }

    public synchronized boolean isProjectHasSelectedItems() {
        List<DeploymentProjectItem> items = getItems();
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

    private String getLastDeployedVersion(AProject wsProject, String deployConfigName) throws IOException {
        Repository deployRepo = null;
        try {
            deployRepo = deploymentManager.getDeployRepository(repositoryConfigName);
        } catch (RRepositoryException e) {
            throw new IOException(e);
        }
        boolean folderStructure;

        if (deployRepo.supports().folders()) {
            folderStructure = !((FolderRepository) deployRepo)
                .listFolders(deploymentManager.repositoryFactoryProxy.getDeploymentsPath(repositoryConfigName) + "/")
                .isEmpty();
        } else {
            folderStructure = false;
        }
        Deployment deployment = new Deployment(deployRepo,
            deploymentManager.repositoryFactoryProxy.getDeploymentsPath(repositoryConfigName) + deployConfigName,
            wsProject.getName(),
            null,
            folderStructure);
        AProject deployedProject = deployment.getProject(wsProject.getName());
        return deployedProject != null ? projectVersionCacheManager.getDeployedProjectVersion(deployedProject) : null;
    }

    private List<DeploymentProjectItem> getItems4Project(AProject project, String repositoryConfigName) {
        String projectName = project.getName();

        List<DeploymentProjectItem> result = new LinkedList<>();
        if (userWorkspace == null) {
            return result; // must never happen
        }
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
                latestDeploymentVersion = userWorkspace.getLatestDeploymentConfiguration(deploymentProject.getName());
            }

            ProjectDescriptor<?> projectDescriptor = null;

            // check all descriptors
            // we are interested in all Deployment projects that has the project
            @SuppressWarnings("rawtypes")
            Collection<ProjectDescriptor> descriptors = latestDeploymentVersion.getProjectDescriptors();
            for (ProjectDescriptor<?> descr : descriptors) {
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

            DependencyChecker checker = new DependencyChecker(projectDescriptorResolver);
            // check against latest version of the deployment project
            checker.addProjects(latestDeploymentVersion);

            String lastDeployedVersion = "";
            try {
                String name = getSelectedProject().getName();
                lastDeployedVersion = getLastDeployedVersion(project, name);
            } catch (IOException e) {
                item.setMessages("Internal error while reading project cache.");
            }

            if (lastDeployedVersion != null && lastDeployedVersion.equals(project.getVersion().getVersionName())) {
                if (StringUtils.isEmpty(repositoryConfigName)) {
                    item.setDisabled(true);
                    item.setMessages("Repository is not selected");
                } else if (deploymentProject.isModified()) {
                    // prevent loosing of user's changes
                    item.setDisabled(true);
                    item.setMessages("Opened for Editing");
                    item.setStyleForMessages(UiConst.STYLE_WARNING);
                    item.setStyleForName(UiConst.STYLE_WARNING);
                } else {
                    if (checker.check()) {
                        item.setMessages("This project revision is already deployed");
                    } else {
                        item.setMessages("Dependent projects should be added to deploy configuration.");
                        item.setStyleForMessages(UiConst.STYLE_ERROR);
                        item.setStyleForName(UiConst.STYLE_ERROR);
                        item.setDisabled(true);
                    }
                }
            } else {
                if (!isGranted(EDIT_DEPLOYMENT)) {
                    // Don't have permission to edit deploy configuration -
                    // skip it
                    continue;
                }
                if (deploymentProject.isModified()) {
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
                    // overwrite settings
                    checker.addProject(project);
                    if (checker.check()) {
                        String to = RepositoryTreeController.getDescriptiveVersion(project.getVersion());
                        String from = "undefined";
                        if (lastDeployedVersion != null) {
                            from = RepositoryTreeController
                                .getDescriptiveVersion(userWorkspace.getDesignTimeRepository()
                                    .getProject(projectDescriptor.getProjectName(),
                                        new CommonVersionImpl(lastDeployedVersion))
                                    .getVersion());
                        }
                        item.setMessages("Can be updated to '" + to + "' from '" + from + "' and then deployed");
                    } else {
                        item.setMessages(
                            "Project version will be updated. Dependent projects should be added to deploy configuration.");
                        item.setStyleForMessages(UiConst.STYLE_ERROR);
                        item.setCanDeploy(false);
                    }
                }
            }

            result.add(item);
        }

        if (!userWorkspace.hasDDProject(projectName) && isGranted(CREATE_DEPLOYMENT)) {
            // there is no deployment project with the same name...
            DeploymentProjectItem item = new DeploymentProjectItem();
            item.setName(projectName);
            try {
                List<ProjectDependencyDescriptor> dependencies = projectDescriptorResolver.getDependencies(project);
                if (dependencies == null || dependencies.isEmpty()) {
                    item.setMessages("Create deploy configuration and deploy");
                } else {
                    item.setMessages(
                        "Create deploy configuration. You should add dependent projects to created deploy configuration after that.");
                    item.setStyleForMessages(UiConst.STYLE_ERROR);
                    item.setCanDeploy(false);
                }
            } catch (ProjectException e) {
                log.error(e.getMessage(), e);
                item.setDisabled(true);
                item.setMessages("Internal error while reading the project from repository.");
                item.setStyleForMessages(UiConst.STYLE_ERROR);
            } catch (XStreamException e) {
                log.error(e.getMessage(), e);
                item.setDisabled(true);
                item.setMessages("Project descriptor is invalid.");
                item.setStyleForMessages(UiConst.STYLE_ERROR);
            }
            item.setStyleForName(UiConst.STYLE_WARNING);

            // place it first
            result.add(0, item);
        }

        return result;
    }

    public abstract AProject getSelectedProject();

    public abstract void reset();

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    public String redeploy() {
        AProject project = getSelectedProject();
        if (project == null) {
            return UiConst.OUTCOME_FAILURE;
        }

        List<ADeploymentProject> toDeploy = new LinkedList<>();
        // update selected deployment projects
        List<DeploymentProjectItem> items = getItems();
        for (DeploymentProjectItem item : items) {
            if (!item.isSelected()) {
                continue;
            }

            ADeploymentProject deploymentProject = update(item.getName(), project);
            if (deploymentProject != null && item.isCanDeploy()) {
                // OK, it was updated
                toDeploy.add(deploymentProject);
            }
        }

        // redeploy takes more time
        String repositoryName = getRepositoryName(repositoryConfigName);

        for (ADeploymentProject deploymentProject : toDeploy) {
            try {
                DeployID id = deploymentManager.deploy(deploymentProject, repositoryConfigName);
                String message = String.format("Project '%s' is successfully deployed with id '%s' to repository '%s'",
                    project.getName(),
                    id.getName(),
                    repositoryName);
                WebStudioUtils.addInfoMessage(message);
            } catch (Exception e) {
                String msg = String
                    .format("Failed to deploy '%s' to repository '%s'", project.getName(), repositoryName);
                log.error(msg, e);
                WebStudioUtils.addErrorMessage(msg, e.getMessage());
            }
        }

        reset();
        productionRepositoriesTreeController.refreshTree();

        return UiConst.OUTCOME_SUCCESS;
    }

    protected String getRepositoryName(String repositoryConfigName) {
        RepositoryConfiguration repo = new RepositoryConfiguration(repositoryConfigName, propertyResolver);
        return repo.getName();
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void setProjectDescriptorResolver(ProjectDescriptorArtefactResolver projectDescriptorResolver) {
        this.projectDescriptorResolver = projectDescriptorResolver;
    }

    public void setDeployConfigRepoComments(Comments deployConfigRepoComments) {
        this.deployConfigRepoComments = deployConfigRepoComments;
    }

    public void setProjectVersionCacheManager(ProjectVersionCacheManager projectVersionCacheManager) {
        this.projectVersionCacheManager = projectVersionCacheManager;
    }

    private ADeploymentProject update(String deploymentName, AProject project) {
        try {

            // get latest version
            // FIXME ADeploymentProject should be renamed to
            // ADeployConfiguration, because of the renaming 'Deployment
            // Project' to the 'Deploy configuration'
            ADeploymentProject deployConfiguration = null;
            if (userWorkspace == null) {
                return null; // must never happen
            }

            if (deploymentName.equals(project.getName())) {
                // the same name
                if (!userWorkspace.hasDDProject(deploymentName)) {
                    // create if absent
                    deployConfiguration = userWorkspace.createDDProject(deploymentName);
                }
            }

            boolean create;

            if (deployConfiguration == null) {
                deployConfiguration = userWorkspace.getDDProject(deploymentName);
                create = false;
            } else {
                create = true;
            }

            boolean sameVersion = deployConfiguration.hasProjectDescriptor(project.getName()) && project.getVersion()
                .compareTo(deployConfiguration.getProjectDescriptor(project.getName()).getProjectVersion()) == 0;

            if (sameVersion) {
                return deployConfiguration;
            } else if (deployConfiguration.isLocked()) {
                // someone else is locked it while we were thinking
                WebStudioUtils.addWarnMessage("Deploy configuration '" + deploymentName + "' is locked by other user");
                return null;
            } else {
                deployConfiguration.open();
                // rewrite project->version
                deployConfiguration.addProjectDescriptor(project.getName(), project.getVersion());

                String comment;
                if (create) {
                    comment = deployConfigRepoComments.createProject(deploymentName);
                } else {
                    comment = deployConfigRepoComments.saveProject(deploymentName);
                }
                deployConfiguration.getFileData().setComment(comment);

                deployConfiguration.save();

                String action = create ? "created" : "updated";
                WebStudioUtils
                    .addInfoMessage("Deploy configuration '" + deploymentName + "' is successfully " + action);
                return deployConfiguration;
            }
        } catch (ProjectException e) {
            String msg = "Failed to update deploy configuration '" + deploymentName + "'";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg);
        }

        return null;
    }

    public String getRepositoryConfigName() {
        if (repositoryConfigName == null) {
            Iterator<RepositoryConfiguration> repos = getRepositories().iterator();
            if (repos.hasNext()) {
                repositoryConfigName = repos.next().getConfigName();
            }
        }

        return repositoryConfigName;
    }

    public void setRepositoryConfigName(String repositoryConfigName) {
        if (repositoryConfigName == null || !repositoryConfigName.equals(this.repositoryConfigName)) {
            this.items = null;
        }
        this.repositoryConfigName = repositoryConfigName;
    }

    public Collection<RepositoryConfiguration> getRepositories() {
        List<RepositoryConfiguration> repos = new ArrayList<>();
        Collection<String> repositoryConfigNames = deploymentManager.getRepositoryConfigNames();
        for (String configName : repositoryConfigNames) {
            RepositoryConfiguration config = new RepositoryConfiguration(configName, propertyResolver);
            repos.add(config);
        }

        repos.sort(RepositoryConfiguration.COMPARATOR);

        return repos;
    }

    public boolean isSelectAll4SmartRedeploy() {
        List<DeploymentProjectItem> items = getItems();

        boolean hasSelectedItem = false;

        for (DeploymentProjectItem item : items) {
            if (!item.isDisabled() && !item.isSelected()) {
                return false;
            }
            if (item.isSelected()) {
                hasSelectedItem = true;
            }
        }

        return hasSelectedItem;
    }

    public void setSelectAll4SmartRedeploy(boolean newState) {
        List<DeploymentProjectItem> items = getItems();

        for (DeploymentProjectItem item : items) {
            if (!item.isDisabled()) {
                item.setSelected(newState);
            }
        }
    }

    public ProductionRepositoriesTreeController getProductionRepositoriesTreeController() {
        return productionRepositoriesTreeController;
    }

    public void setProductionRepositoriesTreeController(
            ProductionRepositoriesTreeController productionRepositoriesTreeController) {
        this.productionRepositoriesTreeController = productionRepositoriesTreeController;
    }

    /**
     * Checks if design repository supports branches
     */
    public boolean isSupportsBranches() {
        try {
            return userWorkspace.getDesignTimeRepository().getRepository().supports().branches();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

}
