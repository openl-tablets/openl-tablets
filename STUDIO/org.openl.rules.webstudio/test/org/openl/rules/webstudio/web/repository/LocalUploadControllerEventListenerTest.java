package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.faces.context.FacesContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.PropertyResolver;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.webstudio.web.admin.ProjectTagsBean;
import org.openl.rules.webstudio.web.jsf.ViewScope;
import org.openl.rules.webstudio.web.repository.event.ProjectDeletedEvent;
import org.openl.security.acl.repository.RepositoryAclService;

@SpringJUnitConfig(classes = LocalUploadControllerEventListenerTest.TestConfig.class)
class LocalUploadControllerEventListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AtomicBoolean localUploadControllerInstantiated;

    @Test
    void projectDeletedEventOutsideJsfViewDoesNotResolveViewScopedListener() {
        assertNull(FacesContext.getCurrentInstance());

        var event = new ProjectDeletedEvent(mock(AProjectArtefact.class));
        assertDoesNotThrow(() -> eventPublisher.publishEvent(event));

        assertFalse(localUploadControllerInstantiated.get());
    }

    @Configuration
    static class TestConfig {

        @Bean
        static CustomScopeConfigurer customScopeConfigurer() {
            var configurer = new CustomScopeConfigurer();
            configurer.setScopes(Map.of("view", new ViewScope()));
            return configurer;
        }

        @Bean
        AtomicBoolean localUploadControllerInstantiated() {
            return new AtomicBoolean();
        }

        @Bean
        @Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
        LocalUploadController localUploadController(AtomicBoolean localUploadControllerInstantiated,
                                                    PropertyResolver propertyResolver,
                                                    ProjectTagsBean projectTagsBean,
                                                    RepositoryAclService designRepositoryAclService) {
            localUploadControllerInstantiated.set(true);
            return new LocalUploadController(propertyResolver, projectTagsBean, designRepositoryAclService);
        }

        @Bean
        PropertyResolver propertyResolver() {
            return mock(PropertyResolver.class);
        }

        @Bean
        ProjectTagsBean projectTagsBean() {
            return mock(ProjectTagsBean.class);
        }

        @Bean
        RepositoryAclService designRepositoryAclService() {
            return mock(RepositoryAclService.class);
        }
    }
}
