package org.openl.rules.rest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.resolver.DesignRepository;
import org.openl.rules.rest.service.ProjectDependencyResolverImpl;
import org.openl.rules.rest.service.ProjectDeploymentServiceImpl;
import org.openl.rules.security.Privileges;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.DeploymentProjectItem;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/user-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProjectManagementController {

    private final ProjectDependencyResolverImpl projectDependencyResolverImpl;
    private final ProjectDeploymentServiceImpl projectDeploymentServiceImpl;
    private final DeploymentManager deploymentManager;


    @Autowired
    public ProjectManagementController(ProjectDependencyResolverImpl projectDependencyResolverImpl,
            ProjectDeploymentServiceImpl projectDeploymentServiceImpl,
            DeploymentManager deploymentManager) {
        this.projectDependencyResolverImpl = projectDependencyResolverImpl;
        this.projectDeploymentServiceImpl = projectDeploymentServiceImpl;
        this.deploymentManager = deploymentManager;
    }

    @Lookup
    public UserWorkspace getUserWorkspace(){
        return null;
    }

    /**
     * Returns information about the project and its dependencies.
     *
     * @param repo repository where the project is located.
     * @param name project name.
     * @return project info.
     */
    @GetMapping("/{repo-name}/projects/{proj-name}/info")
    public ProjectInfo getInfo(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name)  {
        SecurityChecker.allow(Privileges.VIEW_PROJECTS);
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            ProjectInfo info = new ProjectInfo(project);
            info.dependsOn = projectDependencyResolverImpl.getDependsOnProject(project)
                .stream()
                .map(ProjectInfo::new)
                .collect(Collectors.toList());
            info.dependencies = projectDependencyResolverImpl.getProjectDependencies(project)
                .stream()
                .map(ProjectInfo::new)
                .collect(Collectors.toList());
            return info;
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * Returns deployment items for selected project.
     *
     * @param repo repository where the project is located.
     * @param name project name.
     * @param deployRepoName name of deploy repository.
     * @return project info.
     */
    @GetMapping("/{repo-name}/projects/{proj-name}/deployments/{deploy-repo-name}")
    public List<DeploymentProjectItem> getDeploymentItems(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name,
            @PathVariable("deploy-repo-name") String deployRepoName) {
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            return projectDeploymentServiceImpl.getDeploymentProjectItems(project, deployRepoName);
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * Closes the selected project
     *
     * @param repo repository where the project is located.
     * @param name project name.
     */
    @PostMapping("/{repo-name}/projects/{proj-name}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name,
            HttpSession session) {
        WebStudio webStudio = WebStudioUtils.getWebStudio(session);
        SecurityChecker.allow(Privileges.EDIT_PROJECTS);
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            ProjectHistoryService.deleteHistory(project.getBusinessName());
            // We must release module info because it can hold jars.
            // We cannot rely on studio.getProject() to determine if closing project is compiled inside
            // studio.getModel()
            // because project could be changed or cleared before (See studio.reset() usages). Also that project can be
            // a dependency of other. That's why we must always clear moduleInfo when closing a project.
            webStudio.getModel().clearModuleInfo();
            project.close();
            webStudio.reset();
        } catch (ProjectException | IOException e) {
            throw new NotFoundException(name);
        }
    }

    /**
     * Opens the selected project
     *
     * @param repo repository where the project is located.
     * @param name project name.
     */
    @PostMapping("/{repo-name}/projects/{proj-name}/open")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void open(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name,
            @RequestParam("open-dependencies") boolean openDependencies,
            HttpSession session) {
        UserWorkspace userWorkspace = getUserWorkspace();
        WebStudio webStudio = WebStudioUtils.getWebStudio(session);
        SecurityChecker.allow(Privileges.EDIT_PROJECTS);
        try {
            RulesProject project = userWorkspace.getProject(repo.getId(), name);
            if (userWorkspace.isOpenedOtherProject(project)) {
                throw new ConflictException("open.duplicated.project");
            }
            project.open();
            if (openDependencies) {
                openAllDependencies(project);
            }
            // User workspace is changed when the project was opened, so we must refresh it to calc dependencies.
            // reset() should internally refresh workspace.
            webStudio.reset();
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * Deploy the selected project
     *
     * @param repo repository where the project is located.
     * @param name project name.
     * @param deployRepoName repository name where to deploy the project.
     * @param items items to deploy.
     */
    @PostMapping("/{repo-name}/projects/{proj-name}/deploy")
    public void deploy(@DesignRepository("repo-name") Repository repo,
            @PathVariable("proj-name") String name,
            @RequestParam("deploy-repo-name") String deployRepoName,
            @RequestBody String[] items) {
        SecurityChecker.allow(Privileges.EDIT_PROJECTS);
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            List<DeploymentProjectItem> deploymentProjectItems = projectDeploymentServiceImpl
                .getDeploymentProjectItems(project, repo.getId());
            for (String item : items) {
                Optional<DeploymentProjectItem> deploymentProjectItem = deploymentProjectItems.stream()
                    .filter(p -> p.getName().equals(item))
                    .findFirst();
                if (deploymentProjectItem.isPresent() && deploymentProjectItem.get().isCanDeploy()) {
                    ADeploymentProject deploymentProject = projectDeploymentServiceImpl
                        .update(item, project, repo.getId());
                    deploymentManager.deploy(deploymentProject, deployRepoName);
                }
            }
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    private void openAllDependencies(RulesProject project) throws ProjectException {
        for (RulesProject rulesProject : projectDependencyResolverImpl.getProjectDependencies(project)) {
            rulesProject.open();
        }
    }

    private static class ProjectInfo {

        ProjectInfo(RulesProject project) {
            this.name = project.getBusinessName();
            this.modified = project.isModified();
            this.opened = project.isOpened();
            this.localOnly = project.isLocalOnly();
            this.deleted = project.isDeleted();
            this.openedForEditing = project.isOpenedForEditing();
        }

        public String name;
        public boolean modified;
        public boolean opened;
        public boolean localOnly;
        public boolean openedForEditing;
        public boolean deleted;
        public List<ProjectInfo> dependencies;
        public List<ProjectInfo> dependsOn;
    }

}
