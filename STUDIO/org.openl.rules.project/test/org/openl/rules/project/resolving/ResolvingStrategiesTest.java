package org.openl.rules.project.resolving;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.ZipUtils;

public class ResolvingStrategiesTest {

    @Test
    public void testDescriptor() throws Exception {
        ProjectDescriptorBasedResolvingStrategy strategy = new ProjectDescriptorBasedResolvingStrategy();
        File projectFolder = new File("test-resources/descriptor");
        assertTrue(strategy.isRulesProject(projectFolder.toPath()));

        ProjectDescriptor descriptor = strategy.resolveProject(projectFolder.toPath());
        assertNotNull(descriptor);

        assertEquals(1, descriptor.getModules().size());
        assertEquals(1, descriptor.getClasspath().size());

        Module module = descriptor.getModules().get(0);
        assertEquals("rules/Tutorial_1.xls", module.getRulesRootPath().getPath());
        assertTrue(module.getRulesPath().isAbsolute());
        assertTrue(module.getRulesPath().startsWith(projectFolder.toPath().toAbsolutePath()));

        File nonProjectFolder = new File("test-resources");
        assertFalse(strategy.isRulesProject(nonProjectFolder.toPath()));
    }

    @Test
    public void testSimple() throws Exception {
        ResolvingStrategy resolvingStrategy = new SimpleXlsResolvingStrategy();
        File projectFolder = new File("test-resources/excel/");
        assertTrue(resolvingStrategy.isRulesProject(projectFolder.toPath()));
        ProjectDescriptor descriptor = resolvingStrategy.resolveProject(projectFolder.toPath());
        assertEquals(projectFolder.getName(), descriptor.getName());
        assertEquals(projectFolder.getCanonicalPath(), descriptor.getProjectFolder().toRealPath().toString());
        assertEquals(2, descriptor.getModules().size());
        Module moduleFirst = descriptor.getModules().get(0);
        assertEquals("Rules", moduleFirst.getName());
        assertEquals("Rules.xls", moduleFirst.getRulesRootPath().getPath());
        assertTrue(moduleFirst.getRulesPath().isAbsolute());
        assertTrue(moduleFirst.getRulesPath().startsWith(projectFolder.toPath().toAbsolutePath()));

        Module moduleSecond = descriptor.getModules().get(1);
        assertEquals("Rules2", moduleSecond.getName());
        assertEquals("Rules2.xls", moduleSecond.getRulesRootPath().getPath());
        assertTrue(moduleSecond.getRulesPath().isAbsolute());
        assertTrue(moduleSecond.getRulesPath().startsWith(projectFolder.toPath().toAbsolutePath()));
    }

    @Test
    public void testSimpleZip() throws Exception {
        Path projectZip = Paths.get("test-resources/Tutorial 1%20+.zip");
        try (FileSystem fs = FileSystems.newFileSystem(ZipUtils.toJarURI(projectZip), Collections.emptyMap())) {
            Path zipRoot = fs.getPath("/");
            ResolvingStrategy resolvingStrategy = new SimpleXlsResolvingStrategy();
            assertTrue(resolvingStrategy.isRulesProject(zipRoot));

            ProjectDescriptor descriptor = resolvingStrategy.resolveProject(zipRoot);
            assertEquals("Tutorial 1%20+.zip", descriptor.getName());
            assertEquals(zipRoot, descriptor.getProjectFolder());
            assertEquals(1, descriptor.getModules().size());

            Module module1 = descriptor.getModules().get(0);
            assertEquals("Tutorial1 - Intro to Decision Tables", module1.getName());
            assertEquals("Tutorial1 - Intro to Decision Tables.xlsx", module1.getRulesRootPath().getPath());
            assertTrue(module1.getRulesPath().isAbsolute());
            assertTrue(module1.getRulesPath().startsWith(zipRoot));
        }
    }

}
