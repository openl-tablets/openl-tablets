package org.openl.security.acl.workspace;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;

/**
 * The workspace copy of a project must leave the workspace as soon as the user loses the access:
 * a revoked permission hides the project, and the copied data must not stay on the server either.
 *
 * @author Yury Molchan
 */
class SecureUserWorkspaceImplTest {

    private RepositoryAclService designRepositoryAclService;
    private UserWorkspace delegate;
    private SecureUserWorkspaceImpl secureWorkspace;

    @BeforeEach
    void init() {
        designRepositoryAclService = mock(RepositoryAclService.class);
        delegate = mock(UserWorkspace.class);
        secureWorkspace = new SecureUserWorkspaceImpl(delegate, designRepositoryAclService, false);
    }

    @Test
    void revokedOpenedCopyIsClosedAndHidden() throws ProjectException {
        var project = project(true, false, false);

        assertTrue(secureWorkspace.getProjects().isEmpty());

        verify(project).close();
    }

    @Test
    void grantedOpenedCopyStaysOpened() throws ProjectException {
        var project = project(true, false, true);

        assertTrue(secureWorkspace.getProjects().contains(project));

        verify(project, never()).close();
    }

    @Test
    void localProjectIsNotClosed() throws ProjectException {
        var project = project(true, true, false);

        assertTrue(secureWorkspace.getProjects().isEmpty());

        verify(project, never()).close();
    }

    @Test
    void closedProjectIsNotTouched() throws ProjectException {
        var project = project(false, false, false);

        assertTrue(secureWorkspace.getProjects().isEmpty());

        verify(project, never()).close();
    }

    @Test
    void closeFailureDoesNotBreakTheListing() throws ProjectException {
        var project = project(true, false, false);
        doThrow(new ProjectException("Locked")).when(project).close();

        assertTrue(secureWorkspace.getProjects().isEmpty());
    }

    @Test
    void repositoryListingEvictsTheRevokedCopy() throws ProjectException {
        var project = project(true, false, false);
        when(delegate.getProjects("design")).thenReturn(List.of(project));

        assertTrue(secureWorkspace.getProjects("design").isEmpty());

        verify(project).close();
    }

    private RulesProject project(boolean opened, boolean local, boolean granted) {
        RulesProject project = mock(RulesProject.class);
        lenient().when(project.isOpened()).thenReturn(opened);
        lenient().when(project.isLocalOnly()).thenReturn(local);
        lenient().when(project.getName()).thenReturn("P1");
        lenient().when(designRepositoryAclService.isGranted(any(RulesProject.class), anyList()))
                .thenReturn(granted);
        lenient().when(delegate.getProjects()).thenReturn(List.of(project));
        return project;
    }
}
