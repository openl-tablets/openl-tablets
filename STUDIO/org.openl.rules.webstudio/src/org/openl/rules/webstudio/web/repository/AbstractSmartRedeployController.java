package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.cache.ProjectVersionCacheManager;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.util.Utils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.util.StringUtils;

public abstract class AbstractSmartRedeployController {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSmartRedeployController.class);

    /**
     * A controller which contains pre-built UI object tree.
     */
    @Autowired
    private RepositoryTreeState repositoryTreeState;

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

    @Autowired
    private RepositoryAclServiceProvider aclServiceProvider;

    @Autowired
    private SecureDeploymentRepositoryService secureDeploymentRepositoryService;

    @Autowired
    private AclProjectsHelper aclProjectsHelper;

    volatile UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());

    List<DeploymentProjectItem> items;

    private String repositoryConfigName;

    AProject currentProject;

    private String deployComment;

    public synchronized List<DeploymentProjectItem> getItems() {
        if (currentProject == null || (isSupportsBranches() && currentProject.getLastHistoryVersion() == null)) {
            reset();
            return null;
        }

        if (items == null) {
            items = getItems4Project(currentProject, getRepositoryConfigName());
        }
        return items;
    }

    public synchronized boolean isProjectHasSelectedItems() {
        List<DeploymentProjectItem> itemList = getItems();
        if (itemList == null) {
            return false;
        }

        for (DeploymentProjectItem item : itemList) {
            if (item.isSelected()) {
                return true;
            }
        }

        return false;
    }

    public AProject getSelectedProject() {
        return null;
    }

    private AProject getDeployedProject(AProject wsProject, String deployConfigName) throws IOException {
        Repository deployRepo = deploymentManager.getDeployRepository(repositoryConfigName);
        boolean folderStructure;

        if (deployRepo.supports().folders()) {
            folderStructure = !deployRepo
                    .listFolders(deploymentManager.repositoryFactoryProxy.getBasePath(repositoryConfigName))
                    .isEmpty();
        } else {
            folderStructure = false;
        }
        Deployment deployment = new Deployment(deployRepo,
                deploymentManager.repositoryFactoryProxy.getBasePath(repositoryConfigName) + deployConfigName,
                deployConfigName,
                null,
                folderStructure);
        return (AProject) deployment.getProject(wsProject.getName());
    }

    private List<DeploymentProjectItem> getItems4Project(AProject project, String repositoryConfigName) {
        if (!userWorkspace.getDesignTimeRepository().hasDeployConfigRepo()) {
            return Collections.emptyList();
        }

        String projectName = project.getBusinessName();
        String repoId = project.getRepository().getId();
        String path = project.getRealPath();

        List<DeploymentProjectItem> result = new LinkedList<>();
        if (userWorkspace == null) {
            return result; // must never happen
        }
        String dateTimeFormat = WebStudioFormats.getInstance().dateTime();
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
                if (projectName
                        .equals(descr.getProjectName()) && (descr.getRepositoryId() == null || descr.getRepositoryId()
                        .equals(repoId)) && (descr.getPath() == null || descr.getPath().equals(path))) {
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
            AProject deployedProject = null;
            try {
                deployedProject = getDeployedProject(project, deploymentProject.getName());
                lastDeployedVersion = deployedProject != null ? projectVersionCacheManager
                        .getDeployedProjectVersion(deployedProject) : null;
            } catch (IOException e) {
                LOG.debug("Error occurred: ", e);
                item.setMessages("Internal error while reading project cache.");
            }

            if (lastDeployedVersion != null && lastDeployedVersion.equals(project.getVersion().getVersionName())) {
                if (StringUtils.isEmpty(repositoryConfigName)) {
                    item.setDisabled(true);
                    item.setMessages("Repository is not selected.");
                } else if (deploymentProject.isModified()) {
                    // prevent loosing of user's changes
                    item.setDisabled(true);
                    item.setMessages("Opened for Editing");
                    item.setStyleForMessages(UiConst.STYLE_WARNING);
                    item.setStyleForName(UiConst.STYLE_WARNING);
                } else {
                    if (checker.check()) {
                        item.setMessages("This project revision is already deployed.");
                    } else {
                        item.setMessages("Dependent projects should be added to deploy configuration.");
                        item.setStyleForMessages(UiConst.STYLE_ERROR);
                        item.setStyleForName(UiConst.STYLE_ERROR);
                        item.setDisabled(true);
                    }
                }
            } else {
                if (!canUpdateDeployConfig(deploymentProject) || DeploymentRepositoriesUtil
                        .isMainBranchProtected(userWorkspace.getDesignTimeRepository().getDeployConfigRepository())) {
                    // Don't have permission to edit deploy configuration -
                    // skip it
                    continue;
                }
                if (deploymentProject.isModified()) {
                    // prevent loosing of user's changes
                    item.setDisabled(true);
                    item.setMessages("Opened for Editing.");
                    item.setStyleForMessages(UiConst.STYLE_WARNING);
                    item.setStyleForName(UiConst.STYLE_WARNING);
                } else if (deploymentProject.isLocked()) {
                    // won't be able to modify anyway
                    item.setDisabled(true);
                    item.setMessages("Locked by another user.");
                    item.setStyleForMessages(UiConst.STYLE_WARNING);
                    item.setStyleForName(UiConst.STYLE_WARNING);
                } else {
                    // overwrite settings
                    checker.addProject(project);
                    if (checker.check()) {
                        String to = Utils.getDescriptiveVersion(project.getVersion(), dateTimeFormat);
                        if (deployedProject == null) {
                            item.setMessages("Can be deployed.");
                        } else if (lastDeployedVersion == null) {
                            if (projectVersionCacheManager.isCacheCalculated()) {
                                item.setMessages(
                                        "Can be updated to '" + to + "' and then deployed. Deployed version is unknown.");
                            } else {
                                item.setMessages(
                                        "Can be updated to " + to + " and then deployed. Deployed version is being defined.");
                            }
                        } else {
                            String repositoryId = projectDescriptor.getRepositoryId();
                            if (repositoryId == null) {
                                repositoryId = userWorkspace.getDesignTimeRepository().getRepositories().get(0).getId();
                            }
                            ProjectVersion version;
                            if (projectDescriptor.getPath() != null) {
                                try {
                                    version = userWorkspace.getDesignTimeRepository()
                                            .getProjectByPath(repositoryId,
                                                    projectDescriptor.getBranch(),
                                                    projectDescriptor.getPath(),
                                                    lastDeployedVersion)
                                            .getVersion();
                                } catch (IOException e) {
                                    LOG.error(e.getMessage(), e);
                                    version = null;
                                    item.setMessages("Cannot find a project due to error.");
                                }
                            } else {
                                version = userWorkspace.getDesignTimeRepository()
                                        .getProject(repositoryId,
                                                projectDescriptor.getProjectName(),
                                                new CommonVersionImpl(lastDeployedVersion))
                                        .getVersion();
                            }

                            if (version != null) {
                                if (version.getVersionInfo() == null) {
                                    item.setMessages(
                                            "Can be updated to '" + to + "' and then deployed. Deployed version is unknown.");
                                } else {
                                    String from = Utils.getDescriptiveVersion(version, dateTimeFormat);
                                    item.setMessages(
                                            "Can be updated to '" + to + "' from '" + from + "' and then deployed.");
                                }
                            }
                        }
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
        var dtRepository = userWorkspace.getDesignTimeRepository();
        if (!userWorkspace.hasDDProject(projectName) && dtRepository.hasDeployConfigRepo()
                && secureDeploymentRepositoryService.hasPermission(BasePermission.WRITE)
                && !DeploymentRepositoriesUtil.isMainBranchProtected(dtRepository.getDeployConfigRepository())) {
            // there is no deployment project with the same name...
            DeploymentProjectItem item = new DeploymentProjectItem();
            item.setName(projectName);
            try {
                List<ProjectDependencyDescriptor> dependencies = projectDescriptorResolver.getDependencies(project);
                if (dependencies == null || dependencies.isEmpty()) {
                    item.setMessages("Create deploy configuration to deploy.");
                } else {
                    item.setMessages(
                            "Create deploy configuration and add all dependent projects to just created deploy configuration.");
                    item.setStyleForMessages(UiConst.STYLE_ERROR);
                    item.setCanDeploy(false);
                }
            } catch (ProjectException e) {
                LOG.error(e.getMessage(), e);
                item.setDisabled(true);
                item.setMessages("Internal error while reading the project from the repository.");
                item.setStyleForMessages(UiConst.STYLE_ERROR);
            } catch (JAXBException e) {
                LOG.error(e.getMessage(), e);
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

    private boolean canUpdateDeployConfig(ADeploymentProject deployConfigProject) {
        if (!secureDeploymentRepositoryService.hasPermission(BasePermission.WRITE)) {
            return false;
        }
        return deployConfigProject.getProjectDescriptors().stream()
                .anyMatch(pd -> userWorkspace.getProjectByPath(pd.getRepositoryId(), pd.getPath()).isPresent());
    }

    public abstract void reset();

    public String redeploy() {
        if (currentProject == null) {
            return UiConst.OUTCOME_FAILURE;
        }

        List<ADeploymentProject> toDeploy = new LinkedList<>();
        // update selected deployment projects
        List<DeploymentProjectItem> projectItems = getItems();
        for (DeploymentProjectItem item : projectItems) {
            if (!item.isSelected()) {
                continue;
            }

            ADeploymentProject deploymentProject = update(item.getName(), currentProject);
            if (deploymentProject != null && item.isCanDeploy()) {
                // OK, it was updated
                toDeploy.add(deploymentProject);
            }
        }

        // redeploy takes more time
        String repositoryName = getRepositoryName(repositoryConfigName);

        for (ADeploymentProject deploymentProject : toDeploy) {
            try {
                DeployID id = deploymentManager.deploy(deploymentProject, repositoryConfigName, deployComment);
                String message = String.format("Project '%s' is successfully deployed with id '%s' to repository '%s'.",
                        currentProject.getBusinessName(),
                        id.getName(),
                        repositoryName);
                WebStudioUtils.addInfoMessage(message);
            } catch (Exception e) {
                String msg = String.format("Failed to deploy '%s' to repository '%s'.",
                        currentProject.getBusinessName(),
                        repositoryName);
                LOG.error(msg, e);
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

            if (deploymentName.equals(project.getBusinessName()) && !userWorkspace.hasDDProject(deploymentName)) {
                if (!aclProjectsHelper.hasCreateDeployConfigProjectPermission()) {
                    WebStudioUtils
                            .addErrorMessage("There is no permission for creating a new deployment configuration.");
                    return null;
                }
                // the same name, than create if absent
                deployConfiguration = userWorkspace.createDDProject(deploymentName);
            }

            boolean create;

            if (deployConfiguration == null) {
                deployConfiguration = userWorkspace.getDDProject(deploymentName);
                create = false;
            } else {
                create = true;
            }

            boolean sameVersion = isSameVersion(deployConfiguration, project);

            if (sameVersion) {
                return deployConfiguration;
            } else if (deployConfiguration.isLocked()) {
                // someone else is locked it while we were thinking
                WebStudioUtils
                        .addWarnMessage("Deploy configuration '" + deploymentName + "' is locked by another user.");
                return null;
            } else {
                deployConfiguration.open();
                // rewrite project->version
                String branch = project instanceof RulesProject ? ((RulesProject) project).getBranch() : null;
                deployConfiguration.addProjectDescriptor(project.getRepository()
                        .getId(), project.getBusinessName(), project.getRealPath(), branch, project.getVersion());

                String comment;
                if (create) {
                    comment = deployConfigRepoComments.createProject(deploymentName);
                } else {
                    comment = deployConfigRepoComments.saveProject(deploymentName);
                }
                deployConfiguration.getFileData().setComment(comment);

                deployConfiguration.save();

                WebStudioUtils.addInfoMessage(String.format("Deploy configuration '%s' is successfully %s.",
                        deploymentName,
                        create ? "created" : "updated"));
                return deployConfiguration;
            }
        } catch (ProjectException e) {
            String msg = "Failed to update deploy configuration '" + deploymentName + "'.";
            LOG.error(msg, e);
            WebStudioUtils.addErrorMessage(msg);
        }

        return null;
    }

    private boolean isSameVersion(ADeploymentProject deployConfiguration, AProject project) throws ProjectException {
        if (!deployConfiguration.hasProjectDescriptor(project.getBusinessName())) {
            return false;
        }
        var projectDescriptor = deployConfiguration.getProjectDescriptor(project.getBusinessName());
        if (project.getVersion().compareTo(projectDescriptor.getProjectVersion()) != 0) {
            return false;
        }
        String branch = project instanceof RulesProject ? ((RulesProject) project).getBranch() : null;
        return Objects.equals(branch, projectDescriptor.getBranch());
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
            this.repositoryConfigName = repositoryConfigName;
            if (currentProject != null && items != null) {
                List<DeploymentProjectItem> newItems = getItems4Project(currentProject, getRepositoryConfigName());
                if (newItems.size() == items.size()) {
                    for (int i = 0; i < items.size(); i++) {
                        DeploymentProjectItem item = items.get(i);
                        if (item.isSelected()) {
                            newItems.get(i).setSelected(true);
                        }
                    }
                }
                items = newItems;
            } else {
                items = null;
            }
        }
    }

    public String getValidBranch() {
        List<DeploymentProjectItem> projectItems = getItems();
        if (projectItems == null) {
            return null;
        }
        List<ADeploymentProject> selectedProjectsToDeploy = new ArrayList<>();
        for (DeploymentProjectItem item : projectItems) {
            if (item.isSelected() && item.isCanDeploy()) {
                // create a temporary deploy config that is used only in validation. No need to trigger ACL checks
                var deployConfiguration = userWorkspace.getDesignTimeRepository()
                        .createDeploymentConfigurationBuilder("random")
                        .user(userWorkspace.getUser())
                        .lockEngine(new DummyLockEngine())
                        .build();
                var project = currentProject;
                String branch = project instanceof RulesProject ? ((RulesProject) project).getBranch() : null;
                deployConfiguration.addProjectDescriptor(project.getRepository()
                        .getId(), project.getBusinessName(), project.getRealPath(), branch, project.getVersion());
                selectedProjectsToDeploy.add(deployConfiguration);
            }
        }

        return deploymentManager.validateOnMainBranch(selectedProjectsToDeploy, repositoryConfigName);
    }

    public Collection<RepositoryConfiguration> getRepositories() {
        return secureDeploymentRepositoryService.getRepositories().stream()
                .filter(e -> aclServiceProvider.getProdRepoAclService().isGranted(e.getId(), null, List.of(AclPermission.WRITE)))
                .filter(e -> !DeploymentRepositoriesUtil.isMainBranchProtected(
                        deploymentManager.repositoryFactoryProxy.getRepositoryInstance(e.getConfigName())))
                .collect(Collectors.toList());
    }

    public String getRepositoryTypes() throws JsonProcessingException {
        Map<String, String> types = deploymentManager.getRepositoryConfigNames()
                .stream()
                .map(repositoryConfigName -> new RepositoryConfiguration(repositoryConfigName, propertyResolver))
                .filter(e -> aclServiceProvider.getProdRepoAclService().isGranted(e.getId(), null, List.of(AclPermission.WRITE)))
                .collect(Collectors.toMap(RepositoryConfiguration::getConfigName, RepositoryConfiguration::getType));
        return new ObjectMapper().writeValueAsString(types);
    }

    public String getDeployConfigRepositoryType() {
        return Optional.ofNullable(userWorkspace.getDesignTimeRepository())
                .map(DesignTimeRepository::getDeployConfigRepository)
                .map(Repository::getId)
                .map(deployConfigRepositoryId -> new RepositoryConfiguration(deployConfigRepositoryId, propertyResolver))
                .filter(e -> aclServiceProvider.getProdRepoAclService().isGranted(e.getId(), null, List.of(AclPermission.WRITE)))
                .map(RepositoryConfiguration::getType)
                .orElse(null);
    }

    public boolean isSelectAll4SmartRedeploy() {
        List<DeploymentProjectItem> projectItems = getItems();

        boolean hasSelectedItem = false;

        for (DeploymentProjectItem item : projectItems) {
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
        List<DeploymentProjectItem> projectItems = getItems();

        for (DeploymentProjectItem item : projectItems) {
            if (!item.isDisabled()) {
                item.setSelected(newState);
            }
        }
    }

    /**
     * Checks if design repository supports branches
     */
    public boolean isSupportsBranches() {
        try {
            if (currentProject == null) {
                return false;
            }
            return userWorkspace.getDesignTimeRepository()
                    .getRepository(currentProject.getRepository().getId())
                    .supports()
                    .branches();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return false;
    }

    public String getDeployComment() {
        return deployComment;
    }

    public void setDeployComment(String deployComment) {
        this.deployComment = deployComment;
    }
}
