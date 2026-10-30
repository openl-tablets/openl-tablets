package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.openl.config.InMemoryProperties;
import org.openl.spring.env.RefPropertySource;

/**
 * Saving a repository keeps the {@code $ref} it takes its type from, unless its type changes.
 */
class RepositoryConfigurationStoreTest {

    private final InMemoryProperties properties = properties();

    private static InMemoryProperties properties() {
        var environment = new StandardEnvironment();
        var sources = environment.getPropertySources();
        sources.addLast(new MapPropertySource("settings", Map.of(
                "design-repository-configs", "design-flat",
                "repository.design-flat.$ref", "design-git",
                "repository.design-flat.uri", "/repositories/flat",
                "design-git.$ref", "repo-git",
                "repository.broken.$ref", "repo-foo",
                "repo-foo.factory", "repo-foo",
                "repo-git.factory", "repo-git",
                "repo-jdbc.factory", "repo-jdbc")));
        sources.addLast(new RefPropertySource(environment, sources));
        return new InMemoryProperties(environment);
    }

    @Test
    void keepsTheReferenceToSettingsSharedByRepositoriesOfTheType() {
        var configuration = new RepositoryConfiguration("design-flat", properties);

        configuration.commit();

        assertEquals(RepositoryType.GIT, configuration.getRepositoryType());
        assertFalse(properties.getConfig().containsKey("repository.design-flat.$ref"));
    }

    @Test
    void refersToTheSettingsOfANewType() {
        var configuration = new RepositoryConfiguration("design-flat", properties);

        configuration.setType(RepositoryType.DB.factoryId);
        configuration.commit();

        assertEquals("repo-jdbc", properties.getConfig().get("repository.design-flat.$ref"));
    }

    @Test
    void replacesAReferenceToATypeThatIsNotSupported() {
        var configuration = new RepositoryConfiguration("broken", properties);

        configuration.commit();

        assertEquals(RepositoryType.GIT, configuration.getRepositoryType());
        assertEquals("repo-git", properties.getConfig().get("repository.broken.$ref"));
    }
}
