package org.openl.spring.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

class DisablePropertySourceTest {

    private final Map<String, Object> environment = new HashMap<>();
    private final Map<String, Object> settings = new HashMap<>();
    private final Map<String, Object> defaults = new HashMap<>(Map.of(
            "repo-git.factory", "repo-git",
            "repo-git.connection-timeout", "60",
            "repo-jdbc.factory", "repo-jdbc",
            "repository.design.$ref", "repo-git",
            "repository.design.name", "Design"));
    private final DisablePropertySource disable;

    DisablePropertySourceTest() {
        var sources = new MutablePropertySources();
        sources.addLast(new SysEnvRefPropertySource(environment));
        disable = new DisablePropertySource(sources);
        sources.addLast(disable);
        sources.addLast(new MapPropertySource("settings", settings));
        sources.addLast(new MapPropertySource("defaults", defaults));
        sources.addLast(new RefPropertySource(new PropertySourcesPropertyResolver(sources), sources));
    }

    private Object isDisabled(String name) {
        return disable.getProperty(DisablePropertySource.PROPS_PREFIX + name);
    }

    @Test
    void answersOnlyForThePrefixedNames() {
        environment.put("REPOSITORY_DESIGN_NAME", "Rules");

        assertNull(disable.getProperty("repository.design.name"));
    }

    @Test
    void disablesAPropertyTheEnvironmentSets() {
        environment.put("REPOSITORY_DESIGN_NAME", "Rules");

        assertEquals("true", isDisabled("repository.design.name"));
        assertEquals("false", isDisabled("repository.production.name"));
    }

    @Test
    void keepsEditableAPropertyTheFilesSet() {
        settings.put("repository.design.connection-timeout", "30");

        assertEquals("false", isDisabled("repository.design.name"));
        assertEquals("false", isDisabled("repository.design.connection-timeout"));
    }

    @Test
    void keepsEditableAPropertyAReferenceFromTheFilesGives() {
        assertEquals("false", isDisabled("repository.design.factory"));
        assertEquals("false", isDisabled("repository.design.connection-timeout"));
    }

    @Test
    void disablesThePropertiesAReferenceFromTheEnvironmentGives() {
        environment.put("REPOSITORY_DESIGN__REF_", "repo-jdbc");

        assertEquals("true", isDisabled("repository.design.$ref"));
        assertEquals("true", isDisabled("repository.design.factory"));
    }

    @Test
    void disablesThePropertiesInheritedThroughAReferenceFromTheEnvironment() {
        environment.put("REPOSITORY_DESIGN__REF_", "repo-git");

        assertEquals("true", isDisabled("repository.design.connection-timeout"));
        // A property no reference gives a value stays editable.
        assertEquals("false", isDisabled("repository.design.branch"));
    }

    @Test
    void keepsEditableAPropertyTheFilesSetOverAReferenceFromTheEnvironment() {
        environment.put("REPOSITORY_DESIGN__REF_", "repo-git");
        settings.put("repository.design.connection-timeout", "30");

        assertEquals("false", isDisabled("repository.design.connection-timeout"));
        assertEquals("false", isDisabled("repository.design.name"));
    }

    @Test
    void keepsEditableAPropertyWhoseSharedDefaultTheEnvironmentSets() {
        environment.put("REPO_GIT_CONNECTION_TIMEOUT", "120");

        assertEquals("false", isDisabled("repository.design.connection-timeout"));
        assertEquals("true", isDisabled("repo-git.connection-timeout"));
    }

    @Test
    void disablesAPropertyWhenTheEnvironmentSetsTheSecondReference() {
        defaults.put("repository.design.$ref", "repo-shared");
        environment.put("REPO_SHARED__REF_", "repo-git");

        assertEquals("true", isDisabled("repository.design.connection-timeout"));
    }

    @Test
    void keepsEditableAPropertyAChainOfReferencesFromTheFilesGives() {
        defaults.put("repository.design.$ref", "repo-shared");
        defaults.put("repo-shared.$ref", "repo-git");
        environment.put("REPO_GIT_CONNECTION_TIMEOUT", "120");

        assertEquals("false", isDisabled("repository.design.connection-timeout"));
    }

    @Test
    void ignoresABlankReferenceFromTheEnvironment() {
        environment.put("REPOSITORY_DESIGN_CONNECTION_TIMEOUT__REF_", " ");

        assertEquals("false", isDisabled("repository.design.connection-timeout"));
    }

    @Test
    void answersForAValueWhosePlaceholderCannotBeResolved() {
        environment.put("REPOSITORY_DESIGN__REF_", "repo-git");
        defaults.put("repo-git.password", "${UNDEFINED_GIT_PASSWORD}");

        assertEquals("true", isDisabled("repository.design.password"));
    }

    @Test
    void answersForAReferenceWhosePlaceholderCannotBeResolved() {
        environment.put("REPOSITORY_DESIGN__REF_", "${UNDEFINED_PROFILE}");

        // The reference leads to no value, so the property is not set outside.
        assertEquals("false", isDisabled("repository.design.connection-timeout"));
        assertEquals("true", isDisabled("repository.design.$ref"));
    }

    @Test
    void keepsEditableAPropertyWhoseReferencesLoop() {
        defaults.put("repository.design.$ref", "repo-loop");
        environment.put("REPO_LOOP__REF_", "repository.design");

        assertEquals("false", isDisabled("repository.design.connection-timeout"));
    }
}
