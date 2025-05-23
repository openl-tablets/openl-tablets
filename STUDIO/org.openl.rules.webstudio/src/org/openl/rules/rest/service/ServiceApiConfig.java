package org.openl.rules.rest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.PropertyResolver;
import org.springframework.web.context.WebApplicationContext;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.testmethod.TestSuiteExecutor;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

/**
 * REST services configuration
 */
@Configuration
public class ServiceApiConfig {

    @Autowired
    private PropertyResolver propertyResolver;

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
    @Scope(WebApplicationContext.SCOPE_SESSION)
    public RulesUserSession rulesUserSession(CurrentUserInfo currentUserInfo,
                                             MultiUserWorkspaceManager workspaceManager,
                                             UserManagementService userManagementService,
                                             TestSuiteExecutor testSuiteExecutor,
                                             UserSettingManagementService userSettingManagementService,
                                             RepositoryAclService designRepositoryAclService,
                                             @Qualifier("productionRepositoryAclService") SimpleRepositoryAclService productionRepositoryAclService,
                                             ProjectDescriptorArtefactResolver projectDescriptorArtefactResolver,
                                             PathFilter zipFilter,
                                             ZipCharsetDetector zipCharsetDetector,
                                             PropertyResolver propertyResolver,
                                             DeploymentManager deploymentManager) {
        var rulesUserSession = new RulesUserSession();
        rulesUserSession.setUserName(currentUserInfo.getUserName());
        rulesUserSession.setWorkspaceManager(workspaceManager);
        rulesUserSession.setUserManagementService(userManagementService);

        WebStudio webStudio = new WebStudio(rulesUserSession,
                testSuiteExecutor,
                userSettingManagementService,
                designRepositoryAclService,
                productionRepositoryAclService,
                projectDescriptorArtefactResolver,
                zipFilter,
                zipCharsetDetector,
                propertyResolver,
                deploymentManager);
        rulesUserSession.setWebStudio(webStudio);
        return rulesUserSession;
    }

    @Bean
    @Scope(WebApplicationContext.SCOPE_SESSION)
    public UserWorkspace userWorkspace(RulesUserSession rulesUserSession) {
        return rulesUserSession.getUserWorkspace();
    }

    @Bean
    @Scope(WebApplicationContext.SCOPE_SESSION)
    public WebStudio webstudio(RulesUserSession rulesUserSession) {
        return rulesUserSession.getWebStudio();
    }
}
