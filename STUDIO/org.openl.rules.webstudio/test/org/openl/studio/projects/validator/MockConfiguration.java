package org.openl.studio.projects.validator;

import static org.mockito.Mockito.mock;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import org.openl.rules.webstudio.web.Props;
import org.openl.studio.config.ValidationConfiguration;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;

@Configuration
@ComponentScan(basePackages = "org.openl.studio.projects.validator")
@Import(ValidationConfiguration.class)
public class MockConfiguration {

    @Autowired
    public Environment environment;

    @PostConstruct
    public void postConstruct() {
        Props.setEnvironment(environment);
    }

    @Bean
    public ProtectedBranchBypassService protectedBranchBypassService() {
        return mock(ProtectedBranchBypassService.class);
    }
}
