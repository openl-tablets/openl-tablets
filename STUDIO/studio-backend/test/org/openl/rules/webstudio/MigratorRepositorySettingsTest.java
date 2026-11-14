package org.openl.rules.webstudio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.spring.env.DynamicPropertySource;

/**
 * Verifies the steps that bring the stored settings of the repositories from older versions.
 *
 * @author Yury Molchan
 */
class MigratorRepositorySettingsTest {

    /** A dash stands for a removed setting, as a map of expectations cannot hold {@code null}. */
    private static final String REMOVED = "-";

    @Test
    void removesTheCommentTemplatesOf630() {
        var props = new HashMap<String, String>();

        Migrator.migrateTo640(settings(Map.of(
                "repository.design.comment-template", "{project-name}",
                "repository.design.comment-template-old", "{project-name}",
                "repository.design.comment-template.comment-validation-pattern", "EPBDS-.*",
                "repo-default.design.comment-template", "{project-name}")), props);

        assertEquals(Map.of("repository.design.comment-template", REMOVED,
                "repository.design.comment-template-old", REMOVED), withDashForNull(props));
    }

    @Test
    void dropsTheFlatFolderStructureAndMovesTheLocalPathToTheUri() {
        var props = new HashMap<String, String>();

        Migrator.migrateTo600(settings(Map.of(
                "repository.design.folder-structure.flat", "true",
                "repository.design.local-repository-path", "/repos/design",
                "repository.deploy.local-repository-path", "/repos/deploy",
                "repository.deploy.uri", "https://example.com/deploy.git",
                "repo-default.design.folder-structure.flat", "true")), props);

        assertEquals(Map.of("repository.design.folder-structure.flat", REMOVED,
                "repository.design.local-repository-path", REMOVED,
                "repository.design.uri", "/repos/design",
                "repository.deploy.local-repository-path", REMOVED), withDashForNull(props));
    }

    @Test
    void replacesTheFactoryOfARepositoryWithItsReference() {
        var props = new HashMap<String, String>();

        Migrator.migrateRepositoryFactories(settings(Map.of(
                "repository.design.factory", "org.openl.rules.repository.LocalRepositoryFactory",
                "repository.deploy.factory", " ",
                "repo-default.design.factory", "org.openl.rules.repository.LocalRepositoryFactory")), props);

        assertEquals(Map.of("repository.design.factory", REMOVED,
                "repository.design.$ref", "repo-file"), withDashForNull(props));
    }

    private static DynamicPropertySource settings(Map<String, String> stored) {
        var settings = mock(DynamicPropertySource.class);
        when(settings.getPropertyNames()).thenReturn(stored.keySet().toArray(String[]::new));
        stored.forEach((name, value) -> when(settings.getProperty(name)).thenReturn(value));
        return settings;
    }

    private static Map<String, String> withDashForNull(Map<String, String> props) {
        var result = new HashMap<String, String>();
        props.forEach((name, value) -> result.put(name, value == null ? REMOVED : value));
        return result;
    }
}
