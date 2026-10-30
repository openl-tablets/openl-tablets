package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.openl.config.InMemoryProperties;

/**
 * A saved repository changes its type in place, keeping the settings it has.
 */
class RepositoryConfigurationSetTypeTest {

    @Test
    void switchesASavedRepositoryToGit() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("settings", Map.of(
                "design-repository-configs", "design",
                "repository.design.factory", "repo-jdbc",
                "repository.design.uri", "jdbc:h2:mem:rules")));
        var properties = new InMemoryProperties(environment);
        var configuration = new RepositoryConfiguration("design", properties);

        configuration.setType(RepositoryType.GIT.factoryId);

        assertEquals(RepositoryType.GIT, configuration.getRepositoryType());
        var settings = assertInstanceOf(GitRepositorySettings.class, configuration.getSettings());
        assertEquals("jdbc:h2:mem:rules", settings.getUri());
        assertEquals(Map.of("repository.design.$ref", "repo-git"), properties.getConfig());
        assertNull(configuration.getErrorMessage());
    }
}
