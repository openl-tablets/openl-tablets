package org.openl.studio.repositories.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.model.AclRepositoryId;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.repositories.model.RepositoryFeatures;
import org.openl.studio.repositories.model.RepositoryViewModel;

@Service
@RequiredArgsConstructor
public class DesignTimeRepositoryServiceImpl implements DesignTimeRepositoryService {

    private final DesignTimeRepository designTimeRepository;
    private final RepositoryAclService designRepositoryAclService;
    private final RepositoryAccessService repositoryAccessService;
    private final PropertyResolver propertyResolver;

    @Override
    public List<RepositoryViewModel> getRepositoryList() {
        return designTimeRepository.getRepositories()
                .stream()
                .filter(repo -> designRepositoryAclService.isGranted(repo.getId(), null, List.of(BasePermission.READ)))
                .map(repo -> RepositoryViewModel.builder()
                        .aclId(AclRepositoryId.builder()
                                .id(repo.getId())
                                .type(AclRepositoryType.DESIGN)
                                .build())
                        .id(repo.getId())
                        .name(repo.getName())
                        .type(new RepositoryConfiguration(repo.getId(), propertyResolver).getType())
                        .capabilities(repositoryAccessService.computeCapabilities(repo, AclRepositoryType.DESIGN))
                        .features(new RepositoryFeatures(repo.supports()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public boolean canCreateInAnyRepository() {
        // Reuses the same per-repository capability the repository list exposes, so a copy is offered
        // exactly when at least one repository would accept a new project (permission and, for branch
        // repositories, an unprotected branch).
        return designTimeRepository.getRepositories().stream()
                .anyMatch(repo -> Boolean.TRUE.equals(
                        repositoryAccessService.computeCapabilities(repo, AclRepositoryType.DESIGN).canCreateProject()));
    }

    @Override
    public List<String> getBranches(Repository repository) throws IOException {
        if (!designRepositoryAclService.isGranted(repository.getId(), null, List.of(BasePermission.READ))) {
            throw new SecurityException();
        }
        if (!repository.supports().branches()) {
            throw new ConflictException("repository.branch.unsupported.message");
        }
        var branches = ((BranchRepository) repository).listBranches();
        branches.sort(String.CASE_INSENSITIVE_ORDER);
        return branches;
    }
}
