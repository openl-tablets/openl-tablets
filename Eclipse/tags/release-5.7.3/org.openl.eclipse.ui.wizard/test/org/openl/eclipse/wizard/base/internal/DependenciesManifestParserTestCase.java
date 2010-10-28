package org.openl.eclipse.wizard.base.internal;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Aliaksandr Antonik.
 */
public class DependenciesManifestParserTestCase extends TestCase {
    private static void assertEquals(String[] expected, String[] actual) {
        if (expected == null) {
            Assert.assertNull("string array not null", actual);
        }
        Assert.assertNotNull("string array is null", actual);

        Assert.assertEquals("array size differ", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals("elements differ in position " + i, expected[i], actual[i]);
        }
    }

    public void testDependenciesInEnd() {
        DependenciesManifestParser parser = new DependenciesManifestParser(new File("test-resources/1.MANIFEST"));
        assertEquals(new String[] { "org.openl.core", "org.openl.rules.helpers", "org.openl.conf.ant", "org.openl.j",
                "org.openl.rules" }, parser.getDependencies());

    }

    public void testDependenciesInMiddle() {
        DependenciesManifestParser parser = new DependenciesManifestParser(new File("test-resources/2.MANIFEST"));
        assertEquals(new String[] { "org.openl.core", "org.openl.rules.helpers", "org.openl.conf.ant", "org.openl.j",
                "org.openl.rules" }, parser.getDependencies());
    }

    public void testNoDeps() {
        DependenciesManifestParser parser = new DependenciesManifestParser(new File("test-resources/3.MANIFEST"));
        Assert.assertNull(parser.getDependencies());
    }

    public void testNoFile() {
        DependenciesManifestParser parser = new DependenciesManifestParser(new File("doesnotexist"));
        Assert.assertNull(parser.getDependencies());
    }
}
