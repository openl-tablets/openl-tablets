package org.openl.studio.projects.validator;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import org.openl.rules.rest.config.ValidationConfiguration;
import org.openl.rules.webstudio.web.Props;

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
}
