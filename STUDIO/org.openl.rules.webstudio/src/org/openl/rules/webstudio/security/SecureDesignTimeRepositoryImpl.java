package org.openl.rules.webstudio.security;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.acls.model.Permission;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SecuredRepositoryFactory;
import org.openl.util.CollectionUtils;

public class SecureDesignTimeRepositoryImpl implements SecureDesignTimeRepository {

    private final DesignTimeRepository designTimeRepository;
    private final RepositoryAclService designRepositoryAclService;

    public SecureDesignTimeRepositoryImpl(DesignTimeRepository designTimeRepository,
                                          RepositoryAclService designRepositoryAclService) {
        this.designTimeRepository = designTimeRepository;
        this.designRepositoryAclService = designRepositoryAclService;
    }

    @Override
    public List<Repository> getRepositories() {
        return designTimeRepository.getRepositories()
                .stream()
                .filter(e -> designRepositoryAclService
                        .isGranted(e.getId(), null, List.of(AclPermission.READ, AclPermission.CREATE))
                        || isGrantedToAnyProject(e.getId(), List.of(AclPermission.READ))
                )
                .map(e -> SecuredRepositoryFactory.wrapToSecureRepo(e, designRepositoryAclService))
                .collect(Collectors.toList());
    }

    @Override
    public List<Repository> getManageableRepositories() {
        return designTimeRepository.getRepositories()
                .stream()
                .filter(e -> designRepositoryAclService
                        .isGranted(e.getId(), null, List.of(AclPermission.ADMINISTRATION))
                )
                .map(e -> SecuredRepositoryFactory.wrapToSecureRepo(e, designRepositoryAclService))
                .collect(Collectors.toList());
    }

    private boolean isGrantedToAnyProject(String repoId, List<Permission> permissions) {
        return designTimeRepository.getProjects(repoId).stream()
                .anyMatch(project -> designRepositoryAclService.isGranted(project, permissions));
    }

    @Override
    public List<AProject> getManageableProjects() {
        return designTimeRepository.getProjects()
                .stream()
                .filter(e -> designRepositoryAclService.isGranted(e, List.of(AclPermission.ADMINISTRATION)))
                .collect(Collectors.toList());
    }

    @Override
    public Repository getRepository(String id) {
        return SecuredRepositoryFactory.wrapToSecureRepo(designTimeRepository.getRepository(id),
                designRepositoryAclService);
    }

    @Override
    public AProject getProject(String repositoryId, String name) throws ProjectException {
        AProject project = designTimeRepository.getProject(repositoryId, name);
        if (designRepositoryAclService.isGranted(project, List.of(AclPermission.READ))) {
            return project;
        }
        throw new ProjectException("Access denied");
    }

    @Override
    public AProject getProject(String repositoryId, String name, CommonVersion version) {
        AProject project = designTimeRepository.getProject(repositoryId, name, version);
        if (designRepositoryAclService.isGranted(project, List.of(AclPermission.READ))) {
            return project;
        }
        return null;
    }

    @Override
    public AProject getProjectByPath(String repositoryId,
                                     String branch,
                                     String path,
                                     String version) throws IOException {
        AProject project = designTimeRepository.getProjectByPath(repositoryId, branch, path, version);
        if (designRepositoryAclService.isGranted(project, List.of(AclPermission.READ))) {
            return project;
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public Collection<AProject> getProjects() {
        return designTimeRepository.getProjects()
                .stream()
                .filter(e -> designRepositoryAclService.isGranted(e, List.of(AclPermission.READ)))
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends AProject> getProjects(String repositoryId) {
        return designTimeRepository.getProjects(repositoryId)
                .stream()
                .filter(e -> designRepositoryAclService.isGranted(e, List.of(AclPermission.READ)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasProject(String repositoryId, String name) {
        return designTimeRepository.hasProject(repositoryId, name);
    }

    @Override
    public ADeploymentProject.Builder createDeploymentConfigurationBuilder(String name) {
        return designTimeRepository.createDeploymentConfigurationBuilder(name);
    }

    @Override
    public List<ADeploymentProject> getDDProjects() throws RepositoryException {
        return designTimeRepository.getDDProjects()
                .stream()
                .filter(this::canRead)
                .collect(Collectors.toList());
    }

    @Override
    public ADeploymentProject getDDProject(String name) throws RepositoryException {
        var ddProject = designTimeRepository.getDDProject(name);
        if (ddProject == null) {
            return null;
        }
        if (canRead(ddProject)) {
            return ddProject;
        } else {
            throw new RepositoryException("Access denied");
        }
    }

    private boolean canRead(ADeploymentProject deployConfig) {
        return CollectionUtils.isEmpty(deployConfig.getProjectDescriptors()) || deployConfig.getProjectDescriptors().stream()
                .anyMatch(pd -> {
                    try {
                        getProjectByPath(pd.repositoryId(),
                                pd.branch(),
                                pd.path(),
                                pd.projectVersion().getVersionName());
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                });
    }

    @Override
    public void refresh() {
        designTimeRepository.refresh();
    }

    @Override
    public boolean hasDDProject(String name) {
        return designTimeRepository.hasDDProject(name);
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

    @Override
    public boolean hasDeployConfigRepo() {
        return designTimeRepository.hasDeployConfigRepo();
    }

    @Override
    public Repository getDeployConfigRepository() {
        return designTimeRepository.getDeployConfigRepository();
    }

    @Override
    public String getDeployConfigLocation() {
        return designTimeRepository.getDeployConfigLocation();
    }
}
