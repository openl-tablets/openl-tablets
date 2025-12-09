package org.openl.studio.repositories.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.model.AclRepositoryId;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.repositories.model.RepositoryFeatures;
import org.openl.studio.repositories.model.RepositoryViewModel;

@Service
public class DesignTimeRepositoryServiceImpl implements DesignTimeRepositoryService {

    private final DesignTimeRepository designTimeRepository;
    private final RepositoryAclService designRepositoryAclService;

    public DesignTimeRepositoryServiceImpl(DesignTimeRepository designTimeRepository,
                                           RepositoryAclService designRepositoryAclService) {
        this.designTimeRepository = designTimeRepository;
        this.designRepositoryAclService = designRepositoryAclService;
    }

    @Override
    public List<RepositoryViewModel> getRepositoryList() {
        return designTimeRepository.getRepositories()
                .stream()
                .filter(repo -> designRepositoryAclService.isGranted(repo.getId(), null, List.of(BasePermission.READ)))
                .map(repo -> RepositoryViewModel.builder()
                        .id(repo.getId())
                        .name(repo.getName())
                        .aclId(AclRepositoryId.builder()
                                .id(repo.getId())
                                .type(AclRepositoryType.DESIGN)
                                .build())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getBranches(String id) throws IOException {
        var repository = getRepository(id);
        return getBranches(repository);
    }

    private Repository getRepository(String id) {
        var repository = designTimeRepository.getRepository(id);
        if (repository == null) {
            throw new NotFoundException("design.repo.message", id);
        }
        return repository;
    }

    @Override
    public List<String> getBranches(Repository repository) throws IOException {
        if (!designRepositoryAclService.isGranted(repository.getId(), null, List.of(BasePermission.READ))) {
            throw new SecurityException();
        }
        if (!repository.supports().branches()) {
            throw new ConflictException("repository.branch.unsupported.message");
        }
        var branches = ((BranchRepository) repository).getBranches(null);
        branches.sort(String.CASE_INSENSITIVE_ORDER);
        return branches;
    }

    @Override
    public RepositoryFeatures getFeatures(String id) {
        var repository = getRepository(id);
        return getFeatures(repository);
    }

    @Override
    public RepositoryFeatures getFeatures(Repository repository) {
        var supports = repository.supports();
        return new RepositoryFeatures(supports.branches(), supports.searchable());
    }
}
