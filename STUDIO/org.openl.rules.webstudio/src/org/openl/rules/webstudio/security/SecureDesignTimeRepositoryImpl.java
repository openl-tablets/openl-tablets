package org.openl.rules.webstudio.security;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.BranchedProject;
import org.openl.rules.workspace.dtr.BranchedProjectIndexService;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SecuredRepositoryFactory;

@RequiredArgsConstructor
public class SecureDesignTimeRepositoryImpl implements SecureDesignTimeRepository {

    private final DesignTimeRepository designTimeRepository;
    private final RepositoryAclService designRepositoryAclService;

    @Override
    public List<Repository> getRepositories() {
        return designTimeRepository.getRepositories()
                .stream()
                .filter(e -> designRepositoryAclService
                        .isGranted(e.getId(), null, List.of(BasePermission.READ, BasePermission.CREATE))
                        || isGrantedToAnyProject(e.getId(), List.of(BasePermission.READ))
                )
                .map(e -> SecuredRepositoryFactory.wrapToSecureRepo(e, designRepositoryAclService))
                .collect(Collectors.toList());
    }

    @Override
    public List<Repository> getManageableRepositories() {
        return designTimeRepository.getRepositories()
                .stream()
                .filter(e -> designRepositoryAclService
                        .isGranted(e.getId(), null, List.of(BasePermission.ADMINISTRATION))
                )
                .map(e -> SecuredRepositoryFactory.wrapToSecureRepo(e, designRepositoryAclService))
                .collect(Collectors.toList());
    }

    private boolean isGrantedToAnyProject(String repoId, List<Permission> permissions) {
        return designTimeRepository.getProjects(repoId).stream()
                .anyMatch(project -> secureVisibleProject(project, permissions).isPresent());
    }

    @Override
    public List<AProject> getManageableProjects() {
        return designTimeRepository.getProjects()
                .stream()
                .map(project -> secureVisibleProject(project, List.of(BasePermission.ADMINISTRATION)))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public Repository getRepository(String id) {
        return SecuredRepositoryFactory.wrapToSecureRepo(designTimeRepository.getRepository(id),
                designRepositoryAclService);
    }

    @Override
    public AProject getProject(String repositoryId, String name) throws ProjectException {
        var branchedProject = getBranchedProject(repositoryId, name);
        if (branchedProject.isPresent()) {
            return branchedProject.get().homeEntry().project();
        }
        var project = designTimeRepository.getProject(repositoryId, name);
        if (designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            return secureProject(project);
        }
        throw new ProjectException("Access denied");
    }

    @Override
    public AProject getProject(String repositoryId, String name, CommonVersion version) {
        var project = designTimeRepository.getProject(repositoryId, name, version);
        if (designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            return secureProject(project);
        }
        return null;
    }

    @Override
    public AProject getProjectByPath(String repositoryId,
                                     String branch,
                                     String path,
                                     String version) throws IOException {
        var project = designTimeRepository.getProjectByPath(repositoryId, branch, path, version);
        if (designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            return secureProject(project);
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public Collection<AProject> getProjects() {
        return designTimeRepository.getProjects()
                .stream()
                .map(this::secureVisibleProject)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<? extends AProject> getProjects(String repositoryId) {
        return designTimeRepository.getProjects(repositoryId)
                .stream()
                .map(this::secureVisibleProject)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasProject(String repositoryId, String name) {
        if (!designTimeRepository.hasProject(repositoryId, name)) {
            return false;
        }
        try {
            getProject(repositoryId, name);
            return true;
        } catch (ProjectException e) {
            return false;
        }
    }

    @Override
    public boolean hasProjectInAnyBranch(String repositoryId, String name) {
        return designTimeRepository.hasProjectInAnyBranch(repositoryId, name);
    }

    @Override
    public Optional<BranchedProject> getBranchedProject(String repositoryId, String name) {
        return getBranchedProject(repositoryId, name, List.of(BasePermission.READ));
    }

    /**
     * Answers from the unfiltered index, as do the projects it holds alone. Deleting a branch removes content
     * whether or not the caller may read the other branches holding it, so filtering those out would report the
     * last branch where there is none and let the guard through.
     *
     * <p>The trade-off is deliberate: a caller learns whether some branch they cannot read still holds the
     * project, which is one bit about content they are already allowed to remove. Reporting a wrong answer
     * would cost a project.
     */
    @Override
    public boolean isLastProjectBranch(String repositoryId, String name, String branch) {
        return designTimeRepository.isLastProjectBranch(repositoryId, name, branch);
    }

    @Override
    public List<AProject> getProjectsHeldOnlyBy(String repositoryId, String branch) {
        return designTimeRepository.getProjectsHeldOnlyBy(repositoryId, branch);
    }

    /**
     * Answers from the index, without building a readable view of the project first.
     *
     * <p>Callers ask about a project they already hold, so the answer adds nothing they may not see, while
     * filtering it would evaluate the permission of every branch the project lives in to return one bit.
     */
    @Override
    public boolean containsProject(String repositoryId, String name, String branch) {
        return designTimeRepository.containsProject(repositoryId, name, branch);
    }

    private Optional<BranchedProject> getBranchedProject(String repositoryId,
                                                         String name,
                                                         List<Permission> permissions) {
        return designTimeRepository.getBranchedProject(repositoryId, name)
                .flatMap(project -> project
                        .filter(entry -> designRepositoryAclService
                                .isGranted(entry.project(), permissions))
                        .map(filtered -> filtered.mapProjects(this::secureProject)));
    }

    @Override
    public Optional<BranchedProjectIndexService.IndexHealth> getProjectIndexHealth(String repositoryId) {
        return designRepositoryAclService.isGranted(repositoryId, null, List.of(BasePermission.READ))
                ? designTimeRepository.getProjectIndexHealth(repositoryId)
                : Optional.empty();
    }

    @Override
    public void refresh() {
        designTimeRepository.refresh();
    }

    @Override
    public CompletionStage<Void> refreshBranch(String repositoryId, String branch) {
        return designTimeRepository.refreshBranch(repositoryId, branch);
    }

    @Override
    public CompletionStage<Void> refreshRepository(String repositoryId) {
        return designTimeRepository.refreshRepository(repositoryId);
    }

    @Override
    public void addListener(DesignTimeRepositoryListener listener) {
        designTimeRepository.addListener(listener);
    }

    @Override
    public void removeListener(DesignTimeRepositoryListener listener) {
        designTimeRepository.removeListener(listener);
    }

    @Override
    public String getRulesLocation() {
        return designTimeRepository.getRulesLocation();
    }

    @Override
    public List<String> getExceptions() {
        return designTimeRepository.getExceptions();
    }

    private Optional<AProject> secureVisibleProject(AProject project) {
        return secureVisibleProject(project, List.of(BasePermission.READ));
    }

    private Optional<AProject> secureVisibleProject(AProject project, List<Permission> permissions) {
        var branched = getBranchedProject(project.getRepository().getId(), project.getName(), permissions);
        if (branched.isPresent()) {
            return Optional.of(branched.get().homeEntry().project());
        }
        return designRepositoryAclService.isGranted(project, permissions)
                ? Optional.of(secureProject(project))
                : Optional.empty();
    }

    private AProject secureProject(AProject project) {
        var repository = SecuredRepositoryFactory.wrapToSecureRepo(project.getRepository(),
                designRepositoryAclService);
        return new AProject(repository, project.getFileData());
    }

}
