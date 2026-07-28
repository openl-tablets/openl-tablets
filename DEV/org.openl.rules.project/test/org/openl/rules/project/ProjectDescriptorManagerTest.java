package org.openl.rules.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

class ProjectDescriptorManagerTest {

    private static final Path DESCRIPTOR_ZIP = Path.of("test-resources/descriptor.zip");

    @Test
    void testIsCoveredByWildcardModule() throws Exception {
        assertIsCoveredByWildcardModule(ProjectDescriptor.read(Path.of("test-resources/descriptor/rules-wildcard.xml")));
    }

    @Test
    void zipArchive_testIsCoveredByWildcardModule() throws Exception {
        try (FileSystem fs = openZipFile(DESCRIPTOR_ZIP)) {
            assertIsCoveredByWildcardModule(ProjectDescriptor.read(fs.getPath("/rules-wildcard.xml")));
        }
    }

    private void assertIsCoveredByWildcardModule(ProjectDescriptor descriptor) {
        var manager = new ProjectDescriptorManager();
        var newModule = new Module();
        newModule.setName("New Module");
        newModule.setRulesRootPath("rules/New Module.xlsx");
        assertTrue(manager.isCoveredByWildcardModule(descriptor, newModule));

        newModule.setRulesRootPath("rules\\New Module.xlsx");
        assertTrue(manager.isCoveredByWildcardModule(descriptor, newModule));

        newModule.setRulesRootPath("New Module.xlsx");
        assertFalse(manager.isCoveredByWildcardModule(descriptor, newModule));
    }

    @Test
    void emptyDescriptorIsCoveredByImplicitDefaults() {
        var manager = new ProjectDescriptorManager();
        var descriptor = new ProjectDescriptor();

        // A project with no declared modules relies on the implicit rules/** and tests/** defaults.
        assertTrue(manager.isCoveredByWildcardModule(descriptor, module("rules/New Module.xlsx")));
        assertTrue(manager.isCoveredByWildcardModule(descriptor, module("tests/New Module.xlsx")));
        assertFalse(manager.isCoveredByWildcardModule(descriptor, module("New Module.xlsx")));
    }

    @Test
    void registerModuleUnderRulesKeepsModulesImplicit() {
        var manager = new ProjectDescriptorManager();
        var descriptor = new ProjectDescriptor();

        manager.registerModule(descriptor, module("rules/New Module.xlsx"));

        // The file is auto-discovered, so nothing is written and the other implicit modules survive.
        assertTrue(descriptor.getModules().isEmpty());
    }

    @Test
    void registerModuleInRootMaterializesImplicitDefaults() {
        var manager = new ProjectDescriptorManager();
        var descriptor = new ProjectDescriptor();

        manager.registerModule(descriptor, module("New Module.xlsx"));

        // A root file is not auto-discovered, so the implicit defaults are materialized before appending it.
        assertEquals(List.of("rules/**/*.xlsx", "tests/**/*.xlsx", "New Module.xlsx"),
                descriptor.getModules().stream().map(Module::getRulesRootPath).toList());
    }

    @Test
    void registerModuleCoveredByDeclaredWildcardAddsNothing() throws Exception {
        var manager = new ProjectDescriptorManager();
        ProjectDescriptor descriptor = ProjectDescriptor.read(Path.of("test-resources/descriptor/rules-wildcard.xml"));

        manager.registerModule(descriptor, module("rules/New Module.xlsx"));

        assertEquals(2, descriptor.getModules().size());
    }

    @Test
    void declareModuleAppendsDespiteCoveringWildcard() throws Exception {
        var manager = new ProjectDescriptorManager();
        ProjectDescriptor descriptor = ProjectDescriptor.read(Path.of("test-resources/descriptor/rules-wildcard.xml"));

        // The wildcard would name the module after its workbook, so a caller wanting another name declares it.
        manager.declareModule(descriptor, module("Renamed", "rules/New Module.xlsx"));

        assertEquals(3, descriptor.getModules().size());
        assertEquals("Renamed", descriptor.getModules().getLast().getName());
    }

    @Test
    void declareModuleUnderRulesMaterializesImplicitDefaults() {
        var manager = new ProjectDescriptorManager();
        var descriptor = new ProjectDescriptor();

        // The implicit defaults already match the file, so they are written out before the declaration hides them.
        manager.declareModule(descriptor, module("Renamed", "rules/New Module.xlsx"));

        assertEquals(List.of("rules/**/*.xlsx", "tests/**/*.xlsx", "rules/New Module.xlsx"),
                descriptor.getModules().stream().map(Module::getRulesRootPath).toList());
    }

    @Test
    void addingFileUnderRulesKeepsAllModules(@TempDir Path projectFolder) throws Exception {
        Files.createDirectories(projectFolder.resolve("rules"));
        Files.createFile(projectFolder.resolve("rules/BugReproducing.xlsx"));
        Files.createFile(projectFolder.resolve("rules/generalProject.xlsx"));

        var descriptor = new ProjectDescriptor();
        descriptor.setProjectFolder(projectFolder);

        // Simulate adding a new Excel file under rules/ to a project that declares no modules.
        Files.createFile(projectFolder.resolve("rules/context_bug.xlsx"));
        new ProjectDescriptorManager().registerModule(descriptor, module("context_bug", "rules/context_bug.xlsx"));

        assertEquals(List.of("BugReproducing", "context_bug", "generalProject"), resolvedModuleNames(descriptor));
    }

    @Test
    void addingFileInRootKeepsImplicitModules(@TempDir Path projectFolder) throws Exception {
        Files.createDirectories(projectFolder.resolve("rules"));
        Files.createFile(projectFolder.resolve("rules/BugReproducing.xlsx"));
        Files.createFile(projectFolder.resolve("rules/generalProject.xlsx"));

        var descriptor = new ProjectDescriptor();
        descriptor.setProjectFolder(projectFolder);

        // Simulate adding a new Excel file in the project root, which the implicit defaults do not match.
        Files.createFile(projectFolder.resolve("context_bug.xlsx"));
        new ProjectDescriptorManager().registerModule(descriptor, module("context_bug", "context_bug.xlsx"));

        assertEquals(List.of("BugReproducing", "context_bug", "generalProject"), resolvedModuleNames(descriptor));
    }

    private static List<String> resolvedModuleNames(ProjectDescriptor descriptor) throws IOException {
        return descriptor.expand().getModules().stream().map(Module::getName).sorted().toList();
    }

    private static Module module(String rulesRootPath) {
        return module("New Module", rulesRootPath);
    }

    private static Module module(String name, String rulesRootPath) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(rulesRootPath);
        return module;
    }

    private static FileSystem openZipFile(Path path) throws IOException {
        return FileSystems.newFileSystem(path, Thread.currentThread().getContextClassLoader());
    }
}
