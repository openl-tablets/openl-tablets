package org.openl.studio.projects.rest.controller;

import java.util.Comparator;
import java.util.List;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.service.ProjectDependencyResolver;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.repositories.rest.resolver.DesignRepository;

@RestController
@RequestMapping(value = "/user-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User Workspace")
@Deprecated(forRemoval = true)
public class ProjectManagementController {

    private final ProjectDependencyResolver projectDependencyResolver;
    private final RepositoryAclServiceProvider aclServiceProvider;
    private final WorkspaceProjectService projectService;

    @Autowired
    public ProjectManagementController(ProjectDependencyResolver projectDependencyResolver,
                                       RepositoryAclServiceProvider aclServiceProvider,
                                       WorkspaceProjectService projectService) {
        this.projectDependencyResolver = projectDependencyResolver;
        this.aclServiceProvider = aclServiceProvider;
        this.projectService = projectService;
    }

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Lookup
    public WebStudio getWebStudio() {
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
    @Hidden
    public ProjectInfo getInfo(@DesignRepository("repo-name") Repository repo, @PathVariable("proj-name") String name) {
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            if (!aclServiceProvider.getDesignRepoAclService().isGranted(project, List.of(BasePermission.READ))) {
                throw new ForbiddenException();
            }
            ProjectInfo info = new ProjectInfo(project);
            info.dependsOn = projectDependencyResolver.getDependsOnProject(project)
                    .stream()
                    .sorted(Comparator.comparing(RulesProject::getBusinessName, String.CASE_INSENSITIVE_ORDER))
                    .map(ProjectInfo::new)
                    .toList();
            info.dependencies = projectDependencyResolver.getProjectDependencies(project)
                    .stream()
                    .sorted(Comparator.comparing(RulesProject::getBusinessName, String.CASE_INSENSITIVE_ORDER))
                    .map(ProjectInfo::new)
                    .toList();
            return info;
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
    @Hidden
    public void close(@DesignRepository("repo-name") Repository repo, @PathVariable("proj-name") String name) {
        var webstudio = getWebStudio();
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            projectService.close(project);
            webstudio.reset();
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * Opens the selected project
     *
     * @param repo repository where the project is located.
     * @param name project name.
     */
    @PostMapping("/{repo-name}/projects/{proj-name}/open")
    @Hidden
    public void open(@DesignRepository("repo-name") Repository repo,
                     @PathVariable("proj-name") String name,
                     @RequestParam(value = "open-dependencies", required = false, defaultValue = "false") boolean openDependencies) {
        var webstudio = getWebStudio();
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            projectService.open(project, openDependencies);
            // User workspace is changed when the project was opened, so we must refresh it to calc dependencies.
            // reset() should internally refresh workspace.
            webstudio.reset();
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    public static class ProjectInfo {

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
