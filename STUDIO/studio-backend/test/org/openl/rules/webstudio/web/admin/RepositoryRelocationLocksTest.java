package org.openl.rules.webstudio.web.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.openl.config.InMemoryProperties;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.settings.model.repositories.CURepositoryConfigurationModel;
import org.openl.studio.settings.service.repositories.DesignRepositorySettingsServiceImpl;

/**
 * The locks of the projects of a design repository are released when its settings move it to another place, or when
 * it is removed. A change that keeps the place keeps the locks.
 *
 * @author Yury Molchan
 */
class RepositoryRelocationLocksTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final StandardEnvironment environment = new StandardEnvironment();
    private final LockEngine locks = mock(LockEngine.class);
    private final DesignRepositorySettingsServiceImpl service;

    RepositoryRelocationLocksTest() {
        environment.getPropertySources().addLast(new MapPropertySource("settings", Map.of(
                AdministrationSettings.DESIGN_REPOSITORY_CONFIGS, "design,design2",
                "repository.design.factory", "repo-jdbc",
                "repository.design.name", "Design",
                "repository.design.uri", "jdbc:h2:mem:design",
                "repository.design.base.path", "DESIGN/rules/",
                "repository.design2.factory", "repo-jdbc",
                "repository.design2.name", "Design 2",
                "repository.design2.uri", "jdbc:h2:mem:design2",
                "repo-default.design.name", "Design")));
        Props.setEnvironment(environment);
        var proxy = mock(RepositoryFactoryProxy.class);
        when(proxy.getRepoListConfig()).thenReturn(AdministrationSettings.DESIGN_REPOSITORY_CONFIGS);
        var workspaces = mock(LocalWorkspaceManager.class);
        when(workspaces.getLockEngine(LocalWorkspaceManager.PROJECT_LOCKS)).thenReturn(locks);
        service = new DesignRepositorySettingsServiceImpl(
                new RepositoryEditor(proxy, new InMemoryProperties(environment)), JSON,
                mock(BeanValidationProvider.class), workspaces);
    }

    @AfterEach
    void forgetEnvironment() {
        Props.setEnvironment(null);
    }

    private static CURepositoryConfigurationModel request(String id, String name, String uri, String basePath) {
        return CURepositoryConfigurationModel.builder()
                .id(id)
                .name(name)
                .type(RepositoryType.DB)
                .settings(JSON.createObjectNode().put("uri", uri).put("basePath", basePath))
                .build();
    }

    @Test
    void anotherAddressReleasesTheLocks() throws Exception {
        service.store(service.transform(request("design", "Design", "jdbc:h2:mem:moved", "DESIGN/rules/")));

        verify(locks).forceUnlockRepository("design");
    }

    @Test
    void anotherFolderOfTheProjectsReleasesTheLocks() throws Exception {
        service.store(service.transform(request("design", "Design", "jdbc:h2:mem:design", "OTHER/")));

        verify(locks).forceUnlockRepository("design");
    }

    @Test
    void anotherNameKeepsTheLocks() throws Exception {
        service.store(service.transform(request("design", "Renamed", "jdbc:h2:mem:design", "DESIGN/rules/")));

        verify(locks, never()).forceUnlockRepository("design");
    }

    @Test
    void aNewRepositoryReleasesTheLocksLeftUnderItsId() throws Exception {
        service.store(service.transform(request("design3", "Design 3", "jdbc:h2:mem:design3", "")));

        verify(locks).forceUnlockRepository("design3");
    }

    @Test
    void removingARepositoryReleasesItsLocks() {
        service.delete("design2");

        verify(locks).forceUnlockRepository("design2");
    }
}
