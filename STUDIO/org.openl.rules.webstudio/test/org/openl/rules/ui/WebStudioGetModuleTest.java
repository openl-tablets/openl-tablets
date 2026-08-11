package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * The screens name a module by the name it is shown under, and the bean that edits rules.xml looks it up in the
 * descriptor read from the file as written. A module that declares no name of its own is shown under the base
 * name of its path, so it has to be found by that name too — otherwise editing it silently does nothing while the
 * project stays locked.
 */
class WebStudioGetModuleTest {

    private final WebStudio studio = mock(WebStudio.class, CALLS_REAL_METHODS);

    private static ProjectDescriptor project(Module... modules) {
        var descriptor = new ProjectDescriptor();
        descriptor.setName("Rates");
        descriptor.setProjectFolder(Path.of("."));
        descriptor.setModules(List.of(modules));
        return descriptor;
    }

    private static Module module(String name, String path) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(path);
        return module;
    }

    @Test
    void findsAModuleByItsDeclaredName() {
        var declared = module("Rates", "rules/Main.xlsx");

        assertSame(declared, studio.getModule(project(declared), "Rates"));
    }

    // EPBDS-16430: rules.xml declares the path alone, and the module is shown as 'Main'.
    @Test
    void findsAModuleWithoutADeclaredNameByTheNameItIsShownUnder() {
        var nameless = module(null, "rules/Main.xlsx");

        assertSame(nameless, studio.getModule(project(nameless), "Main"));
    }

    @Test
    void findsNothingForANameNoModuleCarries() {
        assertNull(studio.getModule(project(module(null, "rules/Main.xlsx")), "Other"));
    }

    // A pattern stands for every file it matches, so editing it by the name of one of them would rewrite it into
    // that single file and drop the rest.
    @Test
    void findsNoPatternByTheNameOfAFileItMatches() {
        assertNull(studio.getModule(project(module(null, "rules/*/Main.xlsx")), "Main"));
    }

    @Test
    void findsNothingWithoutAProjectOrAName() {
        assertNull(studio.getModule(null, "Main"));
        assertNull(studio.getModule(project(module(null, "rules/Main.xlsx")), null));
    }
}
