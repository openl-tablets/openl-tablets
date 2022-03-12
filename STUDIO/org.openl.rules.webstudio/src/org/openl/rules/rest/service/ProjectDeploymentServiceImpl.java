package org.openl.rules.rest.service;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.CREATE_DEPLOYMENT;
import static org.openl.rules.security.Privileges.EDIT_DEPLOYMENT;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.web.repository.DependencyChecker;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.DeploymentProjectItem;
import org.openl.rules.webstudio.web.repository.UiConst;
import org.openl.rules.webstudio.web.repository.cache.ProjectVersionCacheManager;
import org.openl.rules.webstudio.web.util.Utils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;

import com.thoughtworks.xstream.XStreamException;

@Service
public class ProjectDeploymentServiceImpl implements ProjectDeploymentService {

    private final Logger log = LoggerFactory.getLogger(ProjectDeploymentServiceImpl.class);

    private final ProjectDescriptorArtefactResolver projectDescriptorResolver;
    private final DeploymentManager deploymentManager;
    private final ProjectVersionCacheManager projectVersionCacheManager;
    private final PropertyResolver propertyResolver;

    @Autowired
    public ProjectDeploymentServiceImpl(ProjectDescriptorArtefactResolver projectDescriptorResolver,
            DeploymentManager deploymentManager,
            ProjectVersionCacheManager projectVersionCacheManager,
            PropertyResolver propertyResolver) {
        this.projectDescriptorResolver = projectDescriptorResolver;
        this.deploymentManager = deploymentManager;
        this.projectVersionCacheManager = projectVersionCacheManager;
        this.propertyResolver = propertyResolver;
    }

    @Override
    public List<DeploymentProjectItem> getDeploymentProjectItems(AProject project,
            String deployRepoName) throws ProjectException {
        UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        String projectName = project.getBusinessName();
        String repoId = project.getRepository().getId();
        String path = project.getRealPath();

        List<DeploymentProjectItem> result = new LinkedList<>();

        // get all deployment projects
        for (ADeploymentProject deploymentProject : userWorkspace.getDDProjects()) {
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
                deployedProject = getDeployedProject(project, deploymentProject.getName(), deployRepoName);
                lastDeployedVersion = deployedProject != null ? projectVersionCacheManager
                    .getDeployedProjectVersion(deployedProject) : null;
            } catch (IOException e) {
                log.debug("Error occurred: ", e);
                item.setMessages("Internal error while reading project cache.");
            }

            if (lastDeployedVersion != null && lastDeployedVersion.equals(project.getVersion().getVersionName())) {
                if (StringUtils.isEmpty(deployRepoName)) {
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
                if (!isGranted(EDIT_DEPLOYMENT) || isMainBranchProtected(
                    userWorkspace.getDesignTimeRepository().getDeployConfigRepository())) {
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
                        String dateTimeFormat = WebStudioFormats.getInstance().dateTime();
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
                                    log.error(e.getMessage(), e);
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

        if (!userWorkspace.hasDDProject(projectName) && isGranted(CREATE_DEPLOYMENT) && !isMainBranchProtected(
            userWorkspace.getDesignTimeRepository().getDeployConfigRepository())) {
            // there is no deployment project with the same name...
            DeploymentProjectItem item = new DeploymentProjectItem();
            item.setName(projectName);
            try {
                List<ProjectDependencyDescriptor> dependencies = projectDescriptorResolver.getDependencies(project);
                if (dependencies.isEmpty()) {
                    item.setMessages("Create deploy configuration to deploy.");
                } else {
                    item.setMessages(
                        "Create deploy configuration and add all dependent projects to just created deploy configuration.");
                    item.setStyleForMessages(UiConst.STYLE_ERROR);
                    item.setCanDeploy(false);
                }
            } catch (ProjectException e) {
                log.error(e.getMessage(), e);
                item.setDisabled(true);
                item.setMessages("Internal error while reading the project from the repository.");
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

    @Override
    public AProject getDeployedProject(AProject wsProject,
            String deployConfigName,
            String repositoryConfigName) throws IOException {
        Repository deployRepo = deploymentManager.getDeployRepository(repositoryConfigName);
        boolean folderStructure;

        if (deployRepo.supports().folders()) {
            folderStructure = !((FolderRepository) deployRepo)
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

    @Override
    public ADeploymentProject update(String deploymentName, AProject project, String repoName) {

        UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
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

            boolean sameVersion = deployConfiguration
                .hasProjectDescriptor(project.getBusinessName()) && project.getVersion()
                    .compareTo(
                        deployConfiguration.getProjectDescriptor(project.getBusinessName()).getProjectVersion()) == 0;

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

                Comments deployConfigRepoComments = new Comments(propertyResolver, repoName);
                String comment = create ? deployConfigRepoComments.createProject(deploymentName)
                                        : deployConfigRepoComments.saveProject(deploymentName);
                deployConfiguration.getFileData().setComment(comment);

                deployConfiguration.save();

                return deployConfiguration;
            }
        } catch (ProjectException e) {
            String msg = "Failed to update deploy configuration '" + deploymentName + "'.";
            log.error(msg, e);
        }

        return null;
    }

    private boolean isMainBranchProtected(Repository repo) {
        if (repo.supports().branches()) {
            BranchRepository branchRepo = (BranchRepository) repo;
            return branchRepo.isBranchProtected(branchRepo.getBranch());
        }
        return false;
    }
}
