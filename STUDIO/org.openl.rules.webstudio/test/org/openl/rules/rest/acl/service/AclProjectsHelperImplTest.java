package org.openl.rules.rest.acl.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
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

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;

@ExtendWith(MockitoExtension.class)
class AclProjectsHelperImplTest {

    @Mock
    private RepositoryAclServiceProvider aclServiceProvider;

    @Mock
    private RepositoryAclService repositoryAclService;

    @Mock
    private SecureDeploymentRepositoryService deploymentRepositoryService;

    private AclProjectsHelperImpl aclProjectsHelper;

    @BeforeEach
    void setUp() {
        aclProjectsHelper = new AclProjectsHelperImpl(aclServiceProvider, deploymentRepositoryService, true);
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
        var aclProjectsHelper = new AclProjectsHelperImpl(aclServiceProvider, deploymentRepositoryService, false);

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
    void allow_deployConfigProject() {
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(repositoryAclService);
        when(deploymentRepositoryService.hasPermission(BasePermission.WRITE)).thenReturn(true);

        var deployConfigProject = mock(ADeploymentProject.class);
        when(deployConfigProject.isLocalOnly()).thenReturn(false);

        var pd1RepoId = "repo1";
        var pd1Path = "foo/path1";
        var pd2RepoId = "repo2";
        var pd2Path = "foo/path2";

        doReturn(List.of(createProjectDescriptor(pd1RepoId, pd1Path), createProjectDescriptor(pd2RepoId, pd2Path)))
                .when(deployConfigProject).getProjectDescriptors();

        when(repositoryAclService.isGranted(eq(pd1RepoId), eq(pd1Path), eq(List.of(BasePermission.READ)))).thenReturn(false);
        when(repositoryAclService.isGranted(eq(pd2RepoId), eq(pd2Path), eq(List.of(BasePermission.READ)))).thenReturn(true);

        assertTrue(aclProjectsHelper.hasPermission(deployConfigProject, BasePermission.WRITE));
    }

    @Test
    void allow_delete_for_projectChildArtefact_should_choose_parent_strategy() {
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(repositoryAclService);

        var child = mock(AProjectArtefact.class);
        var project = mock(AProject.class);
        doReturn(project).when(child).getProject();

        when(repositoryAclService.isGranted(eq(child), eq(true), eq(BasePermission.DELETE))).thenReturn(true);
        assertTrue(aclProjectsHelper.hasPermission(child, BasePermission.DELETE));
    }

    @Test
    void allow_write_for_projectChildArtefact_should_not_choose_parent_strategy() {
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(repositoryAclService);

        var child = mock(AProjectArtefact.class);
        var project = mock(AProject.class);
        doReturn(project).when(child).getProject();

        when(repositoryAclService.isGranted(eq(child), eq(false), eq(BasePermission.WRITE))).thenReturn(true);
        assertTrue(aclProjectsHelper.hasPermission(child, BasePermission.WRITE));
    }

    @Test
    void should_fallback_to_parent_project_for_deployConfigurationChild() {
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(repositoryAclService);
        when(deploymentRepositoryService.hasPermission(BasePermission.WRITE)).thenReturn(true);

        var child = mock(AProjectArtefact.class);
        var deployConfigProject = mock(ADeploymentProject.class);
        doReturn(deployConfigProject).when(child).getProject();
        when(deployConfigProject.isLocalOnly()).thenReturn(false);

        var pd1RepoId = "repo1";
        var pd1Path = "foo/path1";

        doReturn(List.of(createProjectDescriptor(pd1RepoId, pd1Path))).when(deployConfigProject).getProjectDescriptors();

        when(repositoryAclService.isGranted(eq(pd1RepoId), eq(pd1Path), eq(List.of(BasePermission.READ)))).thenReturn(true);

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
        var aclProjectsHelper = new AclProjectsHelperImpl(aclServiceProvider, deploymentRepositoryService, false);
        assertFalse(aclProjectsHelper.hasCreateProjectPermission("repo1"));
    }

    @Test
    void allow_deployConfig_project() {
        when(deploymentRepositoryService.hasPermission(BasePermission.CREATE)).thenReturn(true);
        assertTrue(aclProjectsHelper.hasCreateDeployConfigProjectPermission());
    }

    @SuppressWarnings("rawtypes")
    private ProjectDescriptor createProjectDescriptor(String repoId, String path) {
        var projectDescriptor = mock(ProjectDescriptor.class);
        when(projectDescriptor.getRepositoryId()).thenReturn(repoId);
        when(projectDescriptor.getPath()).thenReturn(path);
        return projectDescriptor;
    }

}
