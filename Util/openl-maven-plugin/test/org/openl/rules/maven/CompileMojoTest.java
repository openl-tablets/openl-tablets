package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Covers how {@link CompileMojo} leaves the modules of the {@code tests} folder out of compilation when tests are
 * skipped. A module is left out only when its file lies inside that folder, named {@code tests} on disk. A folder with
 * a similar name or named in another letter case on disk is kept, and so is a module of a project folder named
 * {@code tests}. A module a pattern found is checked by its own path, and a module without a path stays for the
 * compilation to report.
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
                "rules/tests/Nested.xlsx",
                "Tests/Upper.xlsx");

        new CompileMojo().leaveTestsOut(descriptor);

        assertEquals(List.of("rules/Rules.xlsx", "testsuite/Suite.xlsx", "rules/tests/Nested.xlsx", "Tests/Upper.xlsx"),
                PackageMojoTest.modulePaths(descriptor));
    }

    // Windows allows no '?' in a file name
    @DisabledOnOs(OS.WINDOWS)
    @Test
    void leavesOutFoundModuleWhoseNameLooksLikePattern(@TempDir Path project) {
        var descriptor = descriptor(project, "tests/Why?.xlsx", "rules/Rules.xlsx");
        descriptor.getModules().getFirst().setWildcardRulesRootPath("tests/*.xlsx");

        new CompileMojo().leaveTestsOut(descriptor);

        assertEquals(List.of("rules/Rules.xlsx"), PackageMojoTest.modulePaths(descriptor));
    }

    @Test
    void leavesOutTestsFolderWrittenInAnotherCase(@TempDir Path project) throws IOException {
        Files.createDirectories(project.resolve("tests"));
        assumeTrue(Files.exists(project.resolve("TESTS")), "The file system ignores the letter case");
        var descriptor = descriptor(project, "Tests/Tests.xlsx", "rules/Rules.xlsx");

        new CompileMojo().leaveTestsOut(descriptor);

        assertEquals(List.of("rules/Rules.xlsx"), PackageMojoTest.modulePaths(descriptor));
    }

    @Test
    void keepsFolderNamedInAnotherCaseOnDisk(@TempDir Path project) throws IOException {
        Files.createDirectories(project.resolve("Tests"));
        assumeTrue(Files.exists(project.resolve("tests")), "The file system ignores the letter case");
        var descriptor = descriptor(project, "tests/Tests.xlsx", "rules/Rules.xlsx");
        var modules = descriptor.getModules();

        new CompileMojo().leaveTestsOut(descriptor);

        assertSame(modules, descriptor.getModules());
    }

    @Test
    void keepsModuleOfProjectFolderNamedTests(@TempDir Path tmp) throws IOException {
        var project = Files.createDirectories(tmp.resolve("tests"));
        var descriptor = descriptor(project, ".", "", "rules/Rules.xlsx");
        var modules = descriptor.getModules();

        new CompileMojo().leaveTestsOut(descriptor);

        assertSame(modules, descriptor.getModules());
    }

    @Test
    void keepsModuleWithoutPath(@TempDir Path project) {
        var descriptor = descriptor(project, "tests/Tests.xlsx", null, "rules/Rules.xlsx");

        new CompileMojo().leaveTestsOut(descriptor);

        assertEquals(Arrays.asList(null, "rules/Rules.xlsx"), PackageMojoTest.modulePaths(descriptor));
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
