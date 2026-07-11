package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertyResolver;

import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.repositories.model.RepositoryCapabilities;

class DesignTimeRepositoryServiceImplTest {

    private final DesignTimeRepository designTimeRepository = mock(DesignTimeRepository.class);
    private final RepositoryAccessService repositoryAccessService = mock(RepositoryAccessService.class);
    private final DesignTimeRepositoryServiceImpl service = new DesignTimeRepositoryServiceImpl(
            designTimeRepository, mock(RepositoryAclService.class), repositoryAccessService, mock(PropertyResolver.class));

    private Repository repositoryThatCanCreate(boolean canCreate) {
        var repository = mock(Repository.class);
        when(repositoryAccessService.computeCapabilities(repository, AclRepositoryType.DESIGN))
                .thenReturn(new RepositoryCapabilities(canCreate ? Boolean.TRUE : null, null));
        return repository;
    }

    @Test
    void canCreateInAnyRepository_true_when_at_least_one_repository_accepts_a_new_project() {
        var noCreate = repositoryThatCanCreate(false);
        var canCreate = repositoryThatCanCreate(true);
        when(designTimeRepository.getRepositories()).thenReturn(List.of(noCreate, canCreate));

        assertTrue(service.canCreateInAnyRepository());
    }

    @Test
    void canCreateInAnyRepository_false_when_no_repository_accepts_a_new_project() {
        var first = repositoryThatCanCreate(false);
        var second = repositoryThatCanCreate(false);
        when(designTimeRepository.getRepositories()).thenReturn(List.of(first, second));

        assertFalse(service.canCreateInAnyRepository());
    }
}
