package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpSession;

import org.openl.rules.testmethod.TestSuiteExecutor;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tree.view.Profile;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.ProjectDescriptorArtefactResolver;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.openl.studio.projects.service.ProjectAccessService;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.studio.security.CurrentUserInfo;

class ServiceApiConfigTest {

    @TempDir
    private Path workspaceRoot;

    private Environment previousEnvironment;

    @BeforeEach
    void setUp() {
        previousEnvironment = Props.getEnvironment();
        Props.setEnvironment(new MockEnvironment());
    }

    @AfterEach
    void tearDown() {
        Props.setEnvironment(previousEnvironment);
    }

    @Test
    void rulesUserSession_registersLegacySessionAttributes() {
        var session = new MockHttpSession();
        var workspace = workspace();
        var workspaceManager = mock(MultiUserWorkspaceManager.class);
        when(workspaceManager.getUserWorkspace(any(WorkspaceUser.class))).thenReturn(workspace);
        var userSettings = userSettings();
        var currentUserInfo = mock(CurrentUserInfo.class);
        when(currentUserInfo.getUserName()).thenReturn("admin");

        var rulesUserSession = new ServiceApiConfig().rulesUserSession(currentUserInfo,
                workspaceManager,
                mock(UserManagementService.class),
                mock(TestSuiteExecutor.class),
                userSettings,
                mock(RepositoryAclService.class),
                mock(SimpleRepositoryAclService.class),
                mock(ProjectDescriptorArtefactResolver.class),
                mock(PropertyResolver.class),
                mock(DeploymentManager.class),
                mock(ApplicationEventPublisher.class),
                mock(ProjectsMergeConflictsSessionHolder.class),
                mock(ProtectedBranchBypassService.class),
                mock(ProjectIdentifierMapper.class),
                mock(ProjectStateValidator.class),
                mock(ProjectAccessService.class),
                session);

        assertSame(rulesUserSession, session.getAttribute(Constants.RULES_USER_SESSION));
        assertSame(rulesUserSession.getWebStudio(), session.getAttribute("studio"));
    }

    private UserWorkspace workspace() {
        var workspace = mock(UserWorkspace.class);
        var localWorkspace = mock(LocalWorkspace.class);
        var designTimeRepository = mock(DesignTimeRepository.class);

        when(workspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(localWorkspace.getLocation()).thenReturn(workspaceRoot.toFile());
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        return workspace;
    }

    private UserSettingManagementService userSettings() {
        var userSettings = mock(UserSettingManagementService.class);
        when(userSettings.getStringProperty("admin", WebStudio.RULES_TREE_VIEW_DEFAULT))
                .thenReturn(Profile.TREE_VIEWS[0].getName());
        return userSettings;
    }
}
