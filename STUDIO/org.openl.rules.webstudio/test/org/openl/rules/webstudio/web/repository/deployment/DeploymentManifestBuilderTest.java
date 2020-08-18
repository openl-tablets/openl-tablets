package org.openl.rules.webstudio.web.repository.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.Test;

public class DeploymentManifestBuilderTest {

    @Test
    public void testNullableValues() {
        Manifest manifest = new DeploymentManifestBuilder()
                .setBuildBranch(null)
                .setBuiltBy(null)
                .setImplementationTitle(null)
                .setImplementationVersion(null)
                .setBuildNumber(null)
                .build();
        assertEquals(0, manifest.getEntries().size());
        Attributes mainAttributes = manifest.getMainAttributes();
        assertEquals(8, mainAttributes.size());
        assertEquals("", mainAttributes.getValue("Build-Number"));
        assertEquals("", mainAttributes.getValue("Built-By"));
        assertEquals("", mainAttributes.getValue("Build-Branch"));
        assertEquals("", mainAttributes.getValue("Implementation-Version"));
        assertEquals("", mainAttributes.getValue("Implementation-Title"));
        assertEquals("1.0", mainAttributes.getValue("Manifest-Version"));
        assertNotNull(mainAttributes.getValue("Build-Date"));
        assertTrue(mainAttributes.getValue("Created-By").startsWith("OpenL WebStudio v."));
        assertTrue(mainAttributes.getValue("Created-By").length() > 18);
    }

    @Test
    public void testNonNullValues() {
        Manifest manifest = new DeploymentManifestBuilder()
                .setBuildBranch("master")
                .setBuiltBy("John Smith")
                .setImplementationTitle("My Rules")
                .setImplementationVersion("DEFAULT_2020-11-12_11-11-11")
                .setBuildNumber("eqsdsdsd1212sds")
                .build();
        assertEquals(0, manifest.getEntries().size());
        Attributes mainAttributes = manifest.getMainAttributes();
        assertEquals(8, mainAttributes.size());
        assertEquals("eqsdsdsd1212sds", mainAttributes.getValue("Build-Number"));
        assertEquals("John Smith", mainAttributes.getValue("Built-By"));
        assertEquals("master", mainAttributes.getValue("Build-Branch"));
        assertEquals("DEFAULT_2020-11-12_11-11-11", mainAttributes.getValue("Implementation-Version"));
        assertEquals("My Rules", mainAttributes.getValue("Implementation-Title"));
        assertEquals("1.0", mainAttributes.getValue("Manifest-Version"));
        assertNotNull(mainAttributes.getValue("Build-Date"));
        assertTrue(mainAttributes.getValue("Created-By").startsWith("OpenL WebStudio v."));
        assertTrue(mainAttributes.getValue("Created-By").length() > 18);
    }

    @Test
    public void testEmpty() {
        Manifest manifest = new DeploymentManifestBuilder().build();
        assertEquals(0, manifest.getEntries().size());
        Attributes mainAttributes = manifest.getMainAttributes();
        assertEquals(3, mainAttributes.size());
        assertNull(mainAttributes.getValue("Build-Number"));
        assertNull(mainAttributes.getValue("Built-By"));
        assertNull(mainAttributes.getValue("Build-Branch"));
        assertNull(mainAttributes.getValue("Implementation-Version"));
        assertNull(mainAttributes.getValue("Implementation-Title"));
        assertEquals("1.0", mainAttributes.getValue("Manifest-Version"));
        assertNotNull(mainAttributes.getValue("Build-Date"));
        assertTrue(mainAttributes.getValue("Created-By").startsWith("OpenL WebStudio v."));
        assertTrue(mainAttributes.getValue("Created-By").length() > 18);
    }
}
