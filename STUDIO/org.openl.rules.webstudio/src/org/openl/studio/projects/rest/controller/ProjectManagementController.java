package org.openl.studio.projects.rest.controller;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.xml.bind.JAXBException;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.service.ProjectDependencyResolver;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.studio.repositories.rest.resolver.DesignRepository;

@RestController
@RequestMapping(value = "/user-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User Workspace")
@Deprecated(forRemoval = true)
public class ProjectManagementController {

    private final ProjectDependencyResolver projectDependencyResolver;
    private final ProjectStateValidator projectStateValidator;
    private final RepositoryAclServiceProvider aclServiceProvider;
    private final WorkspaceProjectService projectService;
    private final AclProjectsHelper aclProjectsHelper;

    @Autowired
    public ProjectManagementController(ProjectDependencyResolver projectDependencyResolver,
                                       ProjectStateValidator projectStateValidator,
                                       RepositoryAclServiceProvider aclServiceProvider,
                                       WorkspaceProjectService projectService,
                                       AclProjectsHelper aclProjectsHelper) {
        this.projectDependencyResolver = projectDependencyResolver;
        this.projectStateValidator = projectStateValidator;
        this.aclServiceProvider = aclServiceProvider;
        this.projectService = projectService;
        this.aclProjectsHelper = aclProjectsHelper;
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
                    .map(ProjectInfo::new)
                    .collect(Collectors.toList());
            info.dependencies = projectDependencyResolver.getProjectDependencies(project)
                    .stream()
                    .map(ProjectInfo::new)
                    .collect(Collectors.toList());
            return info;
        } catch (ProjectException | JAXBException e) {
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
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

    /**
     * WARNING: Currently it's used only for testing purpose. Should be finalized before using in real life
     *
     * @see org.openl.rules.webstudio.web.repository.RepositoryTreeController#deleteNode()
     */
    @Hidden
    @DeleteMapping(value = "/{repo-name}/projects/{proj-name}/delete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED) // change status after full implementation
    public void delete(@DesignRepository("repo-name") Repository repo,
                       @PathVariable("proj-name") String name,
                       @RequestParam(value = "comment", required = false) final String comment) {
        // FIXME request body is not allowed for DELETE method.
        // see: https://www.rfc-editor.org/rfc/rfc7231#section-4.3.5
        // A payload within a DELETE request message has no defined semantics; sending a payload body on a DELETE
        // request might cause some existing implementations to reject the request.
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            if (!aclProjectsHelper.hasPermission(project, BasePermission.DELETE)) {
                throw new ForbiddenException();
            }
            if (!projectStateValidator.canDelete(project)) {
                if (project.getDesignRepository().supports().branches() && project.getVersion() == null && !project
                        .isLocalOnly()) {
                    throw new ConflictException("project.delete.branch.message");
                }
                if (project.isLocked() || project.isLockedByMe()) {
                    throw new ConflictException("project.delete.locked.message");
                }
                throw new ConflictException("project.delete.message");
            }
            // TODO: Project should be closed for all users
            project.delete(comment);
            getWebStudio().reset();
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", name);
        }
    }

    /**
     * WARNING: Currently it's used only for testing purpose. Should be finalized before using in real life
     *
     * @see org.openl.rules.webstudio.web.repository.RepositoryTreeController#deleteNode()
     */
    @Hidden
    @DeleteMapping(value = "/{repo-name}/projects/{proj-name}/erase", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED) // change status after full implementation
    public void erase(@DesignRepository("repo-name") Repository repo,
                      @PathVariable("proj-name") String name,
                      @RequestParam(value = "comment", required = false) final String comment) {
        try {
            RulesProject project = getUserWorkspace().getProject(repo.getId(), name);
            if (!aclProjectsHelper.hasPermission(project, BasePermission.DELETE)) {
                throw new ForbiddenException();
            }
            if (!projectStateValidator.canErase(project)) {
                throw new ConflictException("project.erase.message");
            }
            project.erase(getUserWorkspace().getUser(), comment);
            aclServiceProvider.getDesignRepoAclService().deleteAcl(project);
            getWebStudio().reset();
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
