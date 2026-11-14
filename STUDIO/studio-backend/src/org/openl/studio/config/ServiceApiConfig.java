package org.openl.studio.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import org.openl.rules.repository.api.Repository;
import org.openl.rules.testmethod.TestSuiteExecutor;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.repository.ProjectDescriptorArtefactResolver;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.projects.service.ProjectAccessService;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.repositories.service.HistoryRepositoryMapper;
import org.openl.studio.security.CurrentUserInfo;
import org.openl.studio.session.ClientSessionScope;

/**
 * REST services configuration
 */
@Configuration
public class ServiceApiConfig {

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public HistoryRepositoryMapper historyRepositoryMapper(Repository repository) {
        return new HistoryRepositoryMapper(repository);
    }

    @Bean
    @ClientSessionScope(proxyMode = ScopedProxyMode.NO)
    public RulesUserSession rulesUserSession(CurrentUserInfo currentUserInfo,
                                             MultiUserWorkspaceManager workspaceManager,
                                             UserManagementService userManagementService,
                                             TestSuiteExecutor testSuiteExecutor,
                                             RepositoryAclService designRepositoryAclService,
                                             ProjectDescriptorArtefactResolver projectDescriptorArtefactResolver,
                                             ApplicationEventPublisher eventPublisher,
                                             ProtectedBranchBypassService bypassService,
                                             ProjectAccessService projectAccessService) {
        var rulesUserSession = new RulesUserSession();
        rulesUserSession.setUserName(currentUserInfo.getUserName());
        rulesUserSession.setWorkspaceManager(workspaceManager);
        rulesUserSession.setUserManagementService(userManagementService);

        var webStudio = new WebStudio(rulesUserSession,
                testSuiteExecutor,
                designRepositoryAclService,
                projectDescriptorArtefactResolver,
                eventPublisher,
                bypassService,
                projectAccessService);
        rulesUserSession.setWebStudio(webStudio);
        return rulesUserSession;
    }

    @Bean
    @ClientSessionScope(proxyMode = ScopedProxyMode.NO)
    public UserWorkspace userWorkspace(RulesUserSession rulesUserSession) {
        return rulesUserSession.getUserWorkspace();
    }

    @Bean
    @ClientSessionScope(proxyMode = ScopedProxyMode.NO)
    public WebStudio webstudio(RulesUserSession rulesUserSession) {
        return rulesUserSession.getWebStudio();
    }
}
