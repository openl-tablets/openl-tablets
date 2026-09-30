package org.openl.studio.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.PropertyResolver;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.testmethod.TestSuiteExecutor;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.repository.ProjectDescriptorArtefactResolver;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.openl.studio.projects.service.ProjectAccessService;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.repositories.service.HistoryRepositoryMapper;
import org.openl.studio.security.CurrentUserInfo;
import org.openl.studio.session.ClientSessionScope;

/**
 * REST services configuration
 */
@Configuration
@RequiredArgsConstructor
public class ServiceApiConfig {

    private final PropertyResolver propertyResolver;

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public HistoryRepositoryMapper historyRepositoryMapper(Repository repository) {
        return new HistoryRepositoryMapper(repository, commentService(repository.getId()));
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Comments commentService(String repoId) {
        return new Comments(propertyResolver, repoId);
    }

    @Bean
    @ClientSessionScope(proxyMode = ScopedProxyMode.NO)
    public RulesUserSession rulesUserSession(CurrentUserInfo currentUserInfo,
                                             MultiUserWorkspaceManager workspaceManager,
                                             UserManagementService userManagementService,
                                             TestSuiteExecutor testSuiteExecutor,
                                             RepositoryAclService designRepositoryAclService,
                                             @Qualifier("productionRepositoryAclService") SimpleRepositoryAclService productionRepositoryAclService,
                                             ProjectDescriptorArtefactResolver projectDescriptorArtefactResolver,
                                             PropertyResolver propertyResolver,
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
                productionRepositoryAclService,
                projectDescriptorArtefactResolver,
                propertyResolver,
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
