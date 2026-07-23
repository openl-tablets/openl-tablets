package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.Module;
import org.openl.studio.projects.model.ModuleViewModel;

class ProjectModulesTest {

    private static Module module(String name, String path) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(path);
        return module;
    }

    private static Module matched(String name, String path, String pattern) {
        var module = module(name, path);
        module.setWildcardRulesRootPath(pattern);
        return module;
    }

    private static List<String> paths(List<ModuleViewModel> modules) {
        return modules.stream().map(ModuleViewModel::path).toList();
    }

    @Test
    void showsOneModulePerDeclaration() {
        var declared = List.of(module("Main", "rules/Main.xlsx"), module("Rules", "rules/**/*.xlsx"));
        var resolved = List.of(module("Main", "rules/Main.xlsx"),
                matched("Auto", "rules/Auto.xlsx", "rules/**/*.xlsx"),
                matched("Home", "rules/Home.xlsx", "rules/**/*.xlsx"));

        var modules = ProjectModules.map(declared, resolved);

        assertEquals(List.of("rules/Main.xlsx", "rules/**/*.xlsx"), paths(modules));
        // A module declared by its own path stands for itself and holds nothing.
        assertNull(modules.get(0).modules());
        var pattern = modules.get(1);
        assertEquals("Rules", pattern.name());
        assertEquals(List.of("rules/Auto.xlsx", "rules/Home.xlsx"), paths(pattern.modules()));
    }

    @Test
    void keepsAPatternThatMatchedNothing() {
        var modules = ProjectModules.map(List.of(module("Tests", "tests/**/*.xlsx")), List.of());

        assertEquals(1, modules.size());
        assertEquals(List.of(), modules.get(0).modules());
    }

    @Test
    void namesAModuleAfterItsFileWhenTheDeclarationLeavesTheNameOut() {
        var modules = ProjectModules.map(List.of(module(null, "rules/Pricing.xlsx")), List.of());

        assertEquals("Pricing", modules.get(0).name());
    }

    @Test
    void showsTheModulesOfAProjectThatDeclaresNone() {
        // A project without rules.xml is made of the Excel files found in it.
        var modules = ProjectModules.map(List.of(), List.of(module("Main", "Main.xlsx")));

        assertEquals(List.of("Main.xlsx"), paths(modules));
        assertNull(modules.get(0).modules());
    }
}
