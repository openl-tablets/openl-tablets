package org.openl.rules.rest.settings.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.context.annotation.RequestScope;

import org.openl.config.InMemoryProperties;
import org.openl.rules.rest.settings.service.DeployConfigRepositorySettingsService;
import org.openl.rules.rest.settings.service.DesignRepositorySettingsService;
import org.openl.rules.rest.settings.service.ProductionRepositorySettingsService;
import org.openl.rules.rest.settings.service.impl.DeployConfigRepositorySettingsServiceImpl;
import org.openl.rules.rest.settings.service.impl.DesignRepositorySettingsServiceImpl;
import org.openl.rules.rest.settings.service.impl.ProductionRepositorySettingsServiceImpl;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.webstudio.web.admin.RepositoryEditor;
import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.rules.webstudio.web.admin.SettingsServiceImpl;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;

@Configuration
public class SettingsConfiguration {

    @Bean
    @RequestScope
    public InMemoryProperties inMemoryProperties(Environment environment) {
        return new InMemoryProperties(environment);
    }

    @Bean("designRepositoryEditor")
    @RequestScope
    public RepositoryEditor designRepositoryEditor(@Qualifier("designRepositoryFactoryProxy") RepositoryFactoryProxy designRepositoryFactoryProxy,
                                                   InMemoryProperties inMemoryProperties) {
        return new RepositoryEditor(designRepositoryFactoryProxy, inMemoryProperties);
    }

    @Bean("productionRepositoryEditor")
    @RequestScope
    public RepositoryEditor productionRepositoryEditor(@Qualifier("productionRepositoryFactoryProxy") RepositoryFactoryProxy designRepositoryFactoryProxy,
                                                       InMemoryProperties inMemoryProperties) {
        return new RepositoryEditor(designRepositoryFactoryProxy, inMemoryProperties);
    }

    @Bean
    public SettingsService settingsService(InMemoryProperties inMemoryProperties) {
        return new SettingsServiceImpl(inMemoryProperties);
    }

    @Bean
    public DesignRepositorySettingsService designRepositorySettingsService(@Qualifier("designRepositoryEditor") RepositoryEditor repositoryEditor,
                                                                           ObjectMapper objectMapper,
                                                                           BeanValidationProvider beanValidationProvider) {
        return new DesignRepositorySettingsServiceImpl(repositoryEditor, objectMapper, beanValidationProvider);
    }

    @Bean
    public ProductionRepositorySettingsService productionRepositorySettingsService(@Qualifier("productionRepositoryEditor") RepositoryEditor repositoryEditor,
                                                                                   ObjectMapper objectMapper,
                                                                                   BeanValidationProvider beanValidationProvider) {
        return new ProductionRepositorySettingsServiceImpl(repositoryEditor, objectMapper, beanValidationProvider);
    }

    @Bean
    public DeployConfigRepositorySettingsService deployConfigRepositorySettingsService(InMemoryProperties inMemoryProperties,
                                                                                       ObjectMapper objectMapper,
                                                                                       BeanValidationProvider beanValidationProvider,
                                                                                       @Qualifier("designRepositoryEditor") RepositoryEditor designRepositoryEditor) {
        return new DeployConfigRepositorySettingsServiceImpl(inMemoryProperties, objectMapper, beanValidationProvider, designRepositoryEditor);
    }

}
