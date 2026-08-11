package org.openl.rules.project.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * A module in rules.xml may declare only its path. It is still known by a name — the one the engine gives it when
 * it reads the project — and everything that looks a module up by name has to use the same one.
 */
class ModuleNameTest {

    private static Module module(String name, String path) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(path);
        return module;
    }

    @Test
    void aDeclaredNameIsTheName() {
        assertEquals("Rates", module("Rates", "rules/Main.xlsx").getResolvedName());
    }

    @Test
    void aModuleWithoutADeclaredNameIsKnownByTheBaseNameOfItsPath() {
        assertEquals("Main", module(null, "rules/Main.xlsx").getResolvedName());
        assertEquals("Main", module("  ", "rules/Main.xlsx").getResolvedName());
    }

    @Test
    void aModuleWithNeitherHasNoName() {
        assertNull(module(null, null).getResolvedName());
    }

    // A pattern names no single file, so it must not answer to the name of the files it matches.
    @Test
    void aPatternWithoutADeclaredNameHasNoName() {
        assertNull(module(null, "rules/*/Main.xlsx").getResolvedName());
        assertNull(module(null, "rules/**/*.xlsx").getResolvedName());
        assertEquals("All rules", module("All rules", "rules/**/*.xlsx").getResolvedName());
    }

    // The name the engine fills in while reading the project is the same one.
    @Test
    void readingTheProjectFillsInThatName() throws Exception {
        var descriptor = new ProjectDescriptor();
        descriptor.setName("Rates");
        descriptor.setProjectFolder(java.nio.file.Path.of("."));
        descriptor.setModules(new java.util.ArrayList<>(java.util.List.of(module(null, "rules/Main.xlsx"))));

        descriptor.expand();

        assertEquals("Main", descriptor.getModules().getFirst().getName());
    }
}
