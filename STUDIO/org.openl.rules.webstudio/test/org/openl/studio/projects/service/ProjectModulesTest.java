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

        var modules = ProjectModules.map(declared, resolved, false);

        assertEquals(List.of("rules/Main.xlsx", "rules/**/*.xlsx"), paths(modules));
        // A module declared by its own path stands for itself and holds nothing.
        assertNull(modules.getFirst().modules());
        var pattern = modules.get(1);
        assertEquals("Rules", pattern.name());
        assertEquals(List.of("rules/Auto.xlsx", "rules/Home.xlsx"), paths(pattern.modules()));
    }

    @Test
    void keepsADeclaredPatternThatMatchedNothing() {
        // A pattern the file itself declares is shown even when it matched nothing.
        var modules = ProjectModules.map(List.of(module("Tests", "tests/**/*.xlsx")), List.of(), false);

        assertEquals(1, modules.size());
        assertEquals(List.of(), modules.getFirst().modules());
    }

    @Test
    void hidesAnEngineDefaultPatternThatMatchedNothing() {
        // The .xls/.xlsm and tests defaults a project carries but has no files for are scan noise, not shown.
        var declared = List.of(module(null, "rules/**/*.xlsx"), module(null, "rules/**/*.xls"),
                module(null, "tests/**/*.xlsx"));
        var resolved = List.of(matched("Main", "rules/Main.xlsx", "rules/**/*.xlsx"));

        var modules = ProjectModules.map(declared, resolved, true);

        assertEquals(List.of("rules/**/*.xlsx"), paths(modules));
    }

    @Test
    void namesAModuleAfterItsFileWhenTheDeclarationLeavesTheNameOut() {
        var modules = ProjectModules.map(List.of(module(null, "rules/Pricing.xlsx")), List.of(), false);

        assertEquals("Pricing", modules.getFirst().name());
    }

    @Test
    void showsTheModulesOfAProjectThatDeclaresNone() {
        // A project without rules.xml is made of the Excel files found in it.
        var modules = ProjectModules.map(List.of(), List.of(module("Main", "Main.xlsx")), false);

        assertEquals(List.of("Main.xlsx"), paths(modules));
        assertNull(modules.getFirst().modules());
    }
}
