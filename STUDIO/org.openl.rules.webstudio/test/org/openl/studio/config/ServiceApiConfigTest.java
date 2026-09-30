package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.GenericWebApplicationContext;

import org.openl.rules.testmethod.TestSuiteExecutor;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.repository.ProjectDescriptorArtefactResolver;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.openl.studio.projects.service.ProjectAccessService;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.security.CurrentUserInfo;
import org.openl.studio.session.ClientSessionConfig;

class ServiceApiConfigTest {

    @TempDir
    private Path workspaceRoot;

    private final UserWorkspace workspace = mock(UserWorkspace.class);
    private final DesignTimeRepository designTimeRepository = mock(DesignTimeRepository.class);
    private final MockServletContext servletContext = new MockServletContext();
    private final MockHttpSession session = new MockHttpSession(servletContext);
    private final MultiUserWorkspaceManager workspaceManager = mock(MultiUserWorkspaceManager.class);

    private Environment previousEnvironment;

    @BeforeEach
    void setUp() {
        previousEnvironment = Props.getEnvironment();
        Props.setEnvironment(new MockEnvironment());
        var localWorkspace = mock(LocalWorkspace.class);
        when(workspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(localWorkspace.getLocation()).thenReturn(workspaceRoot.toFile());
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        // The request of the session the beans are asked for in.
        var request = new MockHttpServletRequest(servletContext);
        request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        Props.setEnvironment(previousEnvironment);
    }

    @Test
    void theEndOfTheSessionReleasesTheWorkspaceAndTearsTheStudioDown() {
        try (var context = context()) {
            var studio = context.getBean(WebStudio.class);
            verify(workspaceManager, never()).releaseUserWorkspace(any());

            session.invalidate();

            verify(workspaceManager).releaseUserWorkspace(workspace);
            verify(designTimeRepository).removeListener(studio);
        }
    }

    @Test
    void aClientWithItsOwnCredentialsKeepsItsStudioAcrossRequestsWithoutASession() {
        try (var context = context()) {
            SecurityContextHolder.getContext()
                    .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("admin", null, List.of()));
            var first = stateless();
            var studio = context.getBean(WebStudio.class);

            // The next request with the same credentials finds the studio it compiled in, and neither opened a
            // session to keep it in.
            var second = stateless();
            assertSame(studio, context.getBean(WebStudio.class));
            assertNull(first.getSession(false));
            assertNull(second.getSession(false));
            verify(workspaceManager, never()).releaseUserWorkspace(any());
        }

        // The application going down ends what the credentials were kept for, as the end of a session does.
        verify(workspaceManager).releaseUserWorkspace(workspace);
    }

    /** A request that proves who it is with its own credentials, and sends no session cookie. */
    private MockHttpServletRequest stateless() {
        var request = new MockHttpServletRequest(servletContext);
        request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    /** The configuration under test, given the services a user's session state is built from. */
    private GenericWebApplicationContext context() {
        when(workspaceManager.acquireUserWorkspace(any(WorkspaceUser.class))).thenReturn(workspace);
        var currentUserInfo = mock(CurrentUserInfo.class);
        when(currentUserInfo.getUserName()).thenReturn("admin");

        var context = new GenericWebApplicationContext(servletContext);
        AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
        context.registerBean(ClientSessionConfig.class);
        context.registerBean(ServiceApiConfig.class);
        context.registerBean(CurrentUserInfo.class, () -> currentUserInfo);
        context.registerBean(MultiUserWorkspaceManager.class, () -> workspaceManager);
        context.registerBean(UserManagementService.class, () -> mock(UserManagementService.class));
        context.registerBean(TestSuiteExecutor.class, () -> mock(TestSuiteExecutor.class));
        context.registerBean(RepositoryAclService.class, () -> mock(RepositoryAclService.class));
        context.registerBean("productionRepositoryAclService",
                SimpleRepositoryAclService.class,
                () -> mock(SimpleRepositoryAclService.class));
        context.registerBean(ProjectDescriptorArtefactResolver.class,
                () -> mock(ProjectDescriptorArtefactResolver.class));
        context.registerBean(ProtectedBranchBypassService.class, () -> mock(ProtectedBranchBypassService.class));
        context.registerBean(ProjectAccessService.class, () -> mock(ProjectAccessService.class));
        context.refresh();
        return context;
    }
}
