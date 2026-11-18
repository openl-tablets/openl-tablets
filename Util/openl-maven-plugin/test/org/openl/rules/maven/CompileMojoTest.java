package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Covers how {@link CompileMojo} leaves the modules of the {@code tests} folder out of compilation when tests are
 * skipped. A module is left out only when its file lies inside that folder, a folder with a similar name is kept,
 * and a module without a path stays for the compilation to report.
 */
class CompileMojoTest {

    @Test
    void leavesOutModulesOfTestsFolder(@TempDir Path project) {
        var descriptor = descriptor(project,
                "rules/Rules.xlsx",
                "tests/Tests.xlsx",
                "tests/sub/More Tests.xlsx",
                "./tests/../tests/Dotted.xlsx",
                "testsuite/Suite.xlsx",
                "rules/tests/Nested.xlsx");

        new CompileMojo().leaveTestsOut(descriptor);

        assertEquals(List.of("rules/Rules.xlsx", "testsuite/Suite.xlsx", "rules/tests/Nested.xlsx"),
                descriptor.getModules().stream().map(Module::getRulesRootPath).toList());
    }

    @Test
    void keepsModuleWithoutPath(@TempDir Path project) {
        var descriptor = descriptor(project, "tests/Tests.xlsx", null, "rules/Rules.xlsx");

        new CompileMojo().leaveTestsOut(descriptor);

        assertEquals(Arrays.asList(null, "rules/Rules.xlsx"),
                descriptor.getModules().stream().map(Module::getRulesRootPath).toList());
    }

    @Test
    void keepsProjectWithoutTestModules(@TempDir Path project) {
        var descriptor = descriptor(project, "rules/Rules.xlsx", "rules/Other.xlsx");
        var modules = descriptor.getModules();

        new CompileMojo().leaveTestsOut(descriptor);

        assertSame(modules, descriptor.getModules());
        assertEquals(2, modules.size());
    }

    private static ProjectDescriptor descriptor(Path project, String... paths) {
        var descriptor = new ProjectDescriptor();
        descriptor.setProjectFolder(project);
        var modules = new ArrayList<Module>();
        for (var path : paths) {
            var module = new Module();
            module.setRulesRootPath(path);
            module.setProject(descriptor);
            modules.add(module);
        }
        descriptor.setModules(modules);
        return descriptor;
    }
}
