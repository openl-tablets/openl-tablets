package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;

/**
 * Naming a repository's type reads the factory it declares, and nothing else of the configuration.
 */
class RepositoryConfigurationTypeTest {

    private static final String DESIGN_FACTORY = "repository.design.factory";

    private final List<String> reads = new ArrayList<>();

    private PropertyResolver properties(Map<String, Object> values) {
        var sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource("test", new HashMap<>(values)) {
            @Override
            public Object getProperty(String name) {
                reads.add(name);
                return super.getProperty(name);
            }
        });
        return new PropertySourcesPropertyResolver(sources);
    }

    @Test
    void namesTheDeclaredType() {
        assertEquals("repo-jdbc",
                RepositoryConfiguration.getType("design", properties(Map.of(DESIGN_FACTORY, "repo-jdbc"))));
    }

    @Test
    void namesTheTypeAFactoryClassNameStandsFor() {
        var legacy = Map.<String, Object> of(DESIGN_FACTORY, "org.openl.rules.repository.db.JdbcDBRepositoryFactory");
        assertEquals("repo-jdbc", RepositoryConfiguration.getType("design", properties(legacy)));
    }

    @Test
    void reportsAGitRepositoryWhenNoSupportedFactoryIsDeclared() {
        assertEquals("repo-git", RepositoryConfiguration.getType("design", properties(Map.of())));
        assertEquals("repo-git",
                RepositoryConfiguration.getType("design", properties(Map.of(DESIGN_FACTORY, "repo-of-its-own"))));
    }

    @Test
    void readsOnlyTheFactoryOfTheNamedConfiguration() {
        RepositoryConfiguration.getType("Design", properties(Map.of(DESIGN_FACTORY, "repo-jdbc")));

        assertIterableEquals(List.of(DESIGN_FACTORY), reads);
    }
}
