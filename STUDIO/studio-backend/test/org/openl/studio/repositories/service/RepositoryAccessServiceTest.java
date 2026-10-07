package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

class RepositoryAccessServiceTest {

    private static final String REPO = "design-repo";
    private static final String BRANCH = "master";

    private SimpleRepositoryAclService aclService;
    private AclProjectsHelper aclProjectsHelper;
    private RepositoryAccessService service;

    @BeforeEach
    void setUp() {
        aclService = mock(SimpleRepositoryAclService.class);
        var aclServiceProvider = mock(RepositoryAclServiceProvider.class);
        aclProjectsHelper = mock(AclProjectsHelper.class);
        when(aclServiceProvider.getAclService(AclRepositoryType.DESIGN.getType())).thenReturn(aclService);
        when(aclServiceProvider.getAclService(AclRepositoryType.PROD.getType())).thenReturn(aclService);
        service = new RepositoryAccessService(aclServiceProvider, aclProjectsHelper);
    }

    @Test
    void design_repo_reports_create_project() {
        when(aclProjectsHelper.hasCreateProjectPermission(REPO)).thenReturn(true);

        var caps = service.computeCapabilities(REPO, AclRepositoryType.DESIGN);

        assertEquals(Boolean.TRUE, caps.canCreateProject());
        assertNull(caps.canManage());
    }

    @Test
    void administration_grant_reports_manage() {
        when(aclService.isGranted(REPO, null, List.of(BasePermission.ADMINISTRATION))).thenReturn(true);

        var caps = service.computeCapabilities(REPO, AclRepositoryType.DESIGN);

        assertEquals(Boolean.TRUE, caps.canManage());
    }

    @Test
    void design_branch_repo_reports_create_project() {
        var repository = branchRepository();
        when(aclProjectsHelper.hasCreateProjectPermission(REPO)).thenReturn(true);

        var caps = service.computeCapabilities(repository, AclRepositoryType.DESIGN);

        assertEquals(Boolean.TRUE, caps.canCreateProject());
    }

    @Test
    void create_capability_is_independent_of_the_configured_branch_protection() {
        var repository = branchRepository();
        // A protected configured branch must not gate project creation.
        when(repository.isBranchProtected(BRANCH)).thenReturn(true);
        when(aclProjectsHelper.hasCreateProjectPermission(REPO)).thenReturn(true);

        var caps = service.computeCapabilities(repository, AclRepositoryType.DESIGN);

        assertEquals(Boolean.TRUE, caps.canCreateProject());
    }

    private static BranchRepository branchRepository() {
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn(REPO);
        when(repository.getBranch()).thenReturn(BRANCH);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        return repository;
    }
}
