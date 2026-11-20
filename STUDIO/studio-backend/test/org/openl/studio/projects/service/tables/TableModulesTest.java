package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.dependencies.MavenDependencyResolver;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;

/**
 * The module a table is found in: a module folder holds the tables of its Markdown files, and a workbook in the folder
 * is a module of its own.
 */
class TableModulesTest {

    @TempDir
    private Path projects;

    @Test
    void aWorkbookInAModuleFolderIsAModuleOfItsOwn() throws IOException {
        var folder = Files.createDirectories(projects.resolve("Pricing/rules/Main"));
        Files.write(folder.resolve("Extra.xlsx"), new byte[0]);
        var project = new ProjectDescriptor();
        project.setProjectFolder(projects.resolve("Pricing"));
        var modules = TableModules.of(List.of(module(project, "Main", "rules/Main"),
                module(project, "Extra", "rules/Main/Extra.xlsx")));

        assertEquals("Main", modules.moduleOf("Pricing/rules/Main/rate.md?sheet=Main&range=A1:B2"));
        assertEquals("Extra", modules.moduleOf("Pricing/rules/Main/Extra.xlsx?sheet=Rules&cell=A1"));
    }

    @Test
    void aModuleOfADownloadedProjectIsAddressedByItsMavenId() throws IOException {
        var local = projects.resolve("maven-repository");
        var rules = Files.createDirectories(local.resolve("com/example/domain/1.0.0/domain-1.0.0-1/rules"));
        Files.write(rules.resolve("Main.xlsx"), new byte[0]);
        var project = new ProjectDescriptor();
        project.setName("Domain");
        project.setProjectFolder(rules.getParent());
        var module = module(project, "Main", "rules/Main.xlsx");
        var studio = mock(WebStudio.class);
        when(studio.getProjects()).thenReturn(Map.of());
        when(studio.getMavenDependencyResolver())
                .thenReturn(new MavenDependencyResolver("file:///none", null, null, local.toString()));

        var loader = mock(IDependencyLoader.class);
        when(loader.getModule()).thenReturn(module);
        var manager = mock(WebStudioWorkspaceRelatedDependencyManager.class);
        when(manager.getDependencyLoaders()).thenReturn(List.of(loader));
        var model = mock(ProjectModel.class);
        when(model.getStudio()).thenReturn(studio);
        when(model.getWebStudioWorkspaceDependencyManager()).thenReturn(manager);

        var where = TableModules.ofWorkspaceAddressed(model)
                .locationOf(module.getRelativeUri() + "?sheet=Main&cell=A1");

        assertEquals(new TableModules.TableLocation("Main", "maven:com.example:domain:1.0.0", "Domain"), where);
    }

    private static Module module(ProjectDescriptor project, String name, String path) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(path);
        module.setProject(project);
        return module;
    }
}
