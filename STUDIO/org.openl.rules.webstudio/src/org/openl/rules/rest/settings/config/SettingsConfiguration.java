package org.openl.rules.rest.settings.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.context.annotation.RequestScope;

import org.openl.config.InMemoryProperties;
import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.rules.webstudio.web.admin.SettingsServiceImpl;

@Configuration
public class SettingsConfiguration {

    @Bean
    @RequestScope
    public InMemoryProperties inMemoryProperties(Environment environment) {
        return new InMemoryProperties(environment);
    }

    @Bean
    public SettingsService settingsService(InMemoryProperties inMemoryProperties) {
        return new SettingsServiceImpl(inMemoryProperties);
    }

}
