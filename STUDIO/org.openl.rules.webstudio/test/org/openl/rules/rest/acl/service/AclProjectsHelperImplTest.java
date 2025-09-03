package org.openl.rules.rest.acl.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;

@ExtendWith(MockitoExtension.class)
class AclProjectsHelperImplTest {

    @Mock
    private RepositoryAclServiceProvider aclServiceProvider;

    @Mock
    private RepositoryAclService repositoryAclService;

    private AclProjectsHelperImpl aclProjectsHelper;

    @BeforeEach
    void setUp() {
        aclProjectsHelper = new AclProjectsHelperImpl(aclServiceProvider, true);
    }

    @Test
    void allow_any_when_project_is_local() {
        var project = mock(UserWorkspaceProject.class);
        when(project.isLocalOnly()).thenReturn(true);
        assertTrue(aclProjectsHelper.hasPermission(project, BasePermission.DELETE));
        assertTrue(aclProjectsHelper.hasPermission(project, BasePermission.CREATE));
        assertTrue(aclProjectsHelper.hasPermission(project, BasePermission.READ));
        assertTrue(aclProjectsHelper.hasPermission(project, BasePermission.WRITE));
    }

    @Test
    void allow_delete_when_allowProjectCreateDelete_is_true() {
        var project = mock(AProject.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(repositoryAclService);

        when(repositoryAclService.isGranted(eq(project), eq(true), eq(BasePermission.DELETE))).thenReturn(true);
        assertTrue(aclProjectsHelper.hasPermission(project, BasePermission.DELETE));
        verify(repositoryAclService).isGranted(eq(project), eq(true), eq(BasePermission.DELETE));
    }

    @Test
    void allow_delete_when_allowProjectCreateDelete_is_false() {
        var project = mock(AProject.class);
        var aclProjectsHelper = new AclProjectsHelperImpl(aclServiceProvider, false);

        assertFalse(aclProjectsHelper.hasPermission(project, BasePermission.DELETE));
        verify(repositoryAclService, never()).isGranted(any(), anyBoolean(), any());
    }

    @Test
    void allow_create_when_allowProjectCreateDelete_is_true() {
        var project = mock(AProject.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(repositoryAclService);
        when(repositoryAclService.isGranted(eq(project), eq(List.of(BasePermission.CREATE)))).thenReturn(true);
        assertTrue(aclProjectsHelper.hasPermission(project, BasePermission.CREATE));

        verify(repositoryAclService).isGranted(eq(project), eq(List.of(BasePermission.CREATE)));
    }

    @Test
    void allow_delete_for_projectChildArtefact_should_choose_parent_strategy() {
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(repositoryAclService);

        var child = mock(AProjectArtefact.class);

        when(repositoryAclService.isGranted(eq(child), eq(true), eq(BasePermission.DELETE))).thenReturn(true);
        assertTrue(aclProjectsHelper.hasPermission(child, BasePermission.DELETE));
    }

    @Test
    void allow_write_for_projectChildArtefact_should_not_choose_parent_strategy() {
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(repositoryAclService);

        var child = mock(AProjectArtefact.class);

        when(repositoryAclService.isGranted(eq(child), eq(false), eq(BasePermission.WRITE))).thenReturn(true);
        assertTrue(aclProjectsHelper.hasPermission(child, BasePermission.WRITE));
    }

    @Test
    void allow_create_project() {
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(repositoryAclService);
        when(repositoryAclService.isGranted(eq("repo1"), isNull(), eq(List.of(BasePermission.CREATE)))).thenReturn(true);
        assertTrue(aclProjectsHelper.hasCreateProjectPermission("repo1"));
    }

    @Test
    void not_allow_create_project() {
        var aclProjectsHelper = new AclProjectsHelperImpl(aclServiceProvider, false);
        assertFalse(aclProjectsHelper.hasCreateProjectPermission("repo1"));
    }

}
