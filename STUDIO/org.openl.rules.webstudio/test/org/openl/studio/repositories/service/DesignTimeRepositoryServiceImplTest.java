package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.repositories.model.RepositoryCapabilities;

class DesignTimeRepositoryServiceImplTest {

    private final DesignTimeRepository designTimeRepository = mock(DesignTimeRepository.class);
    private final RepositoryAclService designRepositoryAclService = mock(RepositoryAclService.class);
    private final RepositoryAccessService repositoryAccessService = mock(RepositoryAccessService.class);
    private final DesignTimeRepositoryServiceImpl service = new DesignTimeRepositoryServiceImpl(
            designTimeRepository,
            designRepositoryAclService,
            repositoryAccessService,
            mock(PropertyResolver.class));

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

    @Test
    void getBranchesUsesRepositoryBranchRefs() throws IOException {
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        when(repository.listBranches()).thenReturn(new ArrayList<>(List.of("release", "Main")));
        when(designRepositoryAclService.isGranted("design", null, List.of(BasePermission.READ))).thenReturn(true);

        assertEquals(List.of("Main", "release"), service.getBranches(repository));
        verify(repository).listBranches();
    }
}
