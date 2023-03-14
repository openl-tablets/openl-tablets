package org.openl.rules.rest.service;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.PropertyResolver;

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
    @Scope(BeanDefinition.SCOPE_PROTOTYPE) // TODO probably can be changed to @SessionScope bean
    public UserWorkspace userWorkspace() {
        return WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
    }

}
