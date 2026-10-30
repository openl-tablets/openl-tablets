package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.openl.config.InMemoryProperties;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.spring.env.DisablePropertySource;
import org.openl.studio.audit.service.SettingsAuditor;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.settings.model.repositories.CURepositoryConfigurationModel;
import org.openl.studio.settings.service.repositories.DesignRepositorySettingsServiceImpl;

/**
 * A repository whose settings an environment variable sets: a change of them is refused, the settings sent back as they
 * were answered are saved without them.
 */
class ReadOnlyRepositorySettingsTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** What the environment sets, as {@link DisablePropertySource} answers it. */
    private final Map<String, Object> readOnly = new HashMap<>(Map.of(
            "_sys_disable_repository.design.base.path", "true",
            "_sys_disable_repository.design.uri", "true"));
    private final Map<String, Object> saved = new HashMap<>();
    private final StandardEnvironment environment = new StandardEnvironment();
    private final InMemoryProperties properties;
    private final DesignRepositorySettingsServiceImpl service;
    private final SettingsServiceImpl settings;

    ReadOnlyRepositorySettingsTest() {
        var sources = environment.getPropertySources();
        sources.addFirst(new MapPropertySource(DisablePropertySource.PROPS_NAME, readOnly));
        sources.addLast(new MapPropertySource("settings", Map.of(
                AdministrationSettings.DESIGN_REPOSITORY_CONFIGS, "design,design2",
                "repository.design.factory", "repo-jdbc",
                "repository.design.name", "Design",
                "repository.design.uri", "jdbc:h2:mem:design",
                "repository.design.base.path", "DESIGN/rules",
                "repository.design2.factory", "repo-jdbc",
                "repository.design2.name", "Design 2",
                "repository.design2.uri", "jdbc:h2:mem:design2")));
        Props.setEnvironment(environment);
        properties = new InMemoryProperties(environment);
        var proxy = mock(RepositoryFactoryProxy.class);
        when(proxy.getRepoListConfig()).thenReturn(AdministrationSettings.DESIGN_REPOSITORY_CONFIGS);
        service = new DesignRepositorySettingsServiceImpl(new RepositoryEditor(proxy, properties), JSON,
                mock(BeanValidationProvider.class));
        settings = new SettingsServiceImpl(properties, environment, mock(SettingsAuditor.class), saved::putAll);
    }

    @AfterEach
    void forgetEnvironment() {
        Props.setEnvironment(null);
    }

    private static CURepositoryConfigurationModel request(String id, String basePath) {
        return CURepositoryConfigurationModel.builder()
                .id(id)
                .name("Design")
                .type(RepositoryType.DB)
                .settings(JSON.createObjectNode().put("uri", "jdbc:h2:mem:design").put("basePath", basePath))
                .build();
    }

    @Test
    void savesTheRepositoryAnsweredWithoutTheSettingsTheEnvironmentSets() throws Exception {
        var configuration = service.transform(request("design", "DESIGN/rules"));
        // The path is read as the environment gives it and written ending with a slash.
        assertEquals("DESIGN/rules/", configuration.getSettings().getBasePath());
        service.store(configuration);
        settings.commit();

        assertFalse(saved.containsKey("repository.design.base.path"));
        assertFalse(saved.containsKey("repository.design.uri"));
        assertFalse(service.isListReadOnly());
    }

    @Test
    void refusesToChangeASettingTheEnvironmentSets() {
        var changed = request("design", "OTHER/");
        var error = assertThrows(BadRequestException.class, () -> service.transform(changed));

        assertEquals("openl.error.400.settings.read-only.message", error.getErrorCode());
        assertArrayEquals(new Object[]{"settings.basePath"}, error.getArgs());
    }

    @Test
    void refusesToAddOrRemoveARepositoryWhileTheEnvironmentSetsTheList() {
        readOnly.put("_sys_disable_" + AdministrationSettings.DESIGN_REPOSITORY_CONFIGS, "true");

        assertTrue(service.isListReadOnly());

        var newRepository = request("design3", "");
        var added = assertThrows(BadRequestException.class, () -> service.transform(newRepository));
        var removed = assertThrows(BadRequestException.class, () -> service.delete("design2"));

        // A repository that does not exist is not found, whatever the list.
        assertThrows(NotFoundException.class, () -> service.delete("design9"));
        for (var error : new BadRequestException[]{added, removed}) {
            assertEquals("openl.error.400.settings.repositories.read-only.message", error.getErrorCode());
            assertArrayEquals(new Object[]{AdministrationSettings.DESIGN_REPOSITORY_CONFIGS}, error.getArgs());
        }
    }
}
