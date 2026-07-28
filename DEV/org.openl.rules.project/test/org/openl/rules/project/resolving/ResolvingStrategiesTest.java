package org.openl.rules.project.resolving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.openl.util.ZipUtils;

class ResolvingStrategiesTest {

    @Test
    void testDescriptor() throws Exception {
        var strategy = new ProjectDescriptorBasedResolvingStrategy();
        var projectFolder = new File("test-resources/descriptor");
        assertTrue(strategy.isRulesProject(projectFolder.toPath()));

        var descriptor = strategy.resolveProject(projectFolder.toPath());
        assertNotNull(descriptor);

        assertEquals(1, descriptor.getModules().size());
        assertEquals(1, descriptor.getClasspath().size());

        var module = descriptor.getModules().getFirst();
        assertEquals("rules/Tutorial_1.xls", module.getRulesRootPath());
        assertTrue(module.getRulesPath().isAbsolute());
        assertTrue(module.getRulesPath().startsWith(projectFolder.toPath().toAbsolutePath()));

        var nonProjectFolder = new File("test-resources");
        assertFalse(strategy.isRulesProject(nonProjectFolder.toPath()));
    }

    @Test
    void testSimple() throws Exception {
        var resolvingStrategy = new SimpleXlsResolvingStrategy();
        var projectFolder = new File("test-resources/excel/");
        assertTrue(resolvingStrategy.isRulesProject(projectFolder.toPath()));
        var descriptor = resolvingStrategy.resolveProject(projectFolder.toPath());
        assertEquals(projectFolder.getName(), descriptor.getName());
        assertEquals(projectFolder.getCanonicalPath(), descriptor.getProjectFolder().toRealPath().toString());
        assertEquals(2, descriptor.getModules().size());
        var moduleFirst = descriptor.getModules().getFirst();
        assertEquals("Rules", moduleFirst.getName());
        assertEquals("Rules.xls", moduleFirst.getRulesRootPath());
        assertTrue(moduleFirst.getRulesPath().isAbsolute());
        assertTrue(moduleFirst.getRulesPath().startsWith(projectFolder.toPath().toAbsolutePath()));

        var moduleSecond = descriptor.getModules().get(1);
        assertEquals("Rules2", moduleSecond.getName());
        assertEquals("Rules2.xls", moduleSecond.getRulesRootPath());
        assertTrue(moduleSecond.getRulesPath().isAbsolute());
        assertTrue(moduleSecond.getRulesPath().startsWith(projectFolder.toPath().toAbsolutePath()));
    }

    @Test
    void testSimpleZip() throws Exception {
        Path projectZip = Path.of("test-resources/Tutorial 1%20+.zip");
        try (FileSystem fs = FileSystems.newFileSystem(ZipUtils.toJarURI(projectZip), Collections.emptyMap())) {
            var zipRoot = fs.getPath("/");
            var resolvingStrategy = new SimpleXlsResolvingStrategy();
            assertTrue(resolvingStrategy.isRulesProject(zipRoot));

            var descriptor = resolvingStrategy.resolveProject(zipRoot);
            assertEquals("Tutorial 1%20+.zip", descriptor.getName());
            assertEquals(zipRoot, descriptor.getProjectFolder());
            assertEquals(1, descriptor.getModules().size());

            var module1 = descriptor.getModules().getFirst();
            assertEquals("Tutorial1 - Intro to Decision Tables", module1.getName());
            assertEquals("Tutorial1 - Intro to Decision Tables.xlsx", module1.getRulesRootPath());
            assertTrue(module1.getRulesPath().isAbsolute());
            assertTrue(module1.getRulesPath().startsWith(zipRoot));
        }
    }

}
