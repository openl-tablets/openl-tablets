package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

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

    private static Module module(ProjectDescriptor project, String name, String path) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(path);
        module.setProject(project);
        return module;
    }
}
