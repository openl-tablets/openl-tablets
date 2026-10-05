package org.openl.rules.project.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The URI of a table names the file of its module, then the place of the table in the file. A module owns the
 * tables of its own file only, even when the path of another file starts with its path.
 */
class ModuleTablesTest {

    @TempDir
    private Path projects;

    private Module module(String path) {
        var project = new ProjectDescriptor();
        project.setProjectFolder(projects.resolve("Pricing"));
        var module = new Module();
        module.setRulesRootPath(path);
        module.setProject(project);
        return module;
    }

    @Test
    void ownsTheTablesOfItsFile() {
        var claims = module("rules/Claims.xls");

        assertTrue(claims.containsTable("Pricing/rules/Claims.xls?sheet=Rules&cell=A1"));
        assertTrue(claims.containsTable("Pricing/rules/Claims.xls"), "The file itself");
    }

    @Test
    void ownsNoTableOfAFileItsPathIsTheStartOf() {
        var claims = module("rules/Claims.xls");

        assertFalse(claims.containsTable("Pricing/rules/Claims.xlsx?sheet=Rules&cell=A1"));
        assertFalse(claims.containsTable("Pricing/rules/Claims.xls.bak?sheet=Rules&cell=A1"));
        assertFalse(claims.containsTable("Pricing/rules/Claims.xls/Rates.xlsx?sheet=Rules&cell=A1"));
    }

    @Test
    void ownsNoTableWithoutAPathOrAUri() {
        assertFalse(module(null).containsTable("Pricing/rules/Claims.xls?sheet=Rules&cell=A1"));
        assertFalse(module("rules/Claims.xls").containsTable(null));
    }
}
