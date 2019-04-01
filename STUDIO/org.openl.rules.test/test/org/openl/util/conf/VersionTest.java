/*
 * Created on May 25, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util.conf;

import org.junit.Assert;

import junit.framework.TestCase;

/**
 * @author snshor
 */
public class VersionTest extends TestCase {

    /**
     * Constructor for VersionTest.
     *
     * @param name
     */
    public VersionTest(String name) {
        super(name);
    }

    /*
     * Test for boolean equals(Object)
     */
    public void testEqualsObject() {
    }

    public void testIsVersion() {
        String t1 = "1.2.3";

        Assert.assertTrue(Version.isVersion(t1, 0, ".."));

        String t2 = "x_12312.212.322.zx";

        Assert.assertTrue(Version.isVersion(t2, 2, ".."));

        String t3 = "x_2.2.2_04.zx";

        Assert.assertTrue(Version.isVersion(t3, 2));

        String t4 = "x_.12312.212.322.zx";

        Assert.assertFalse(Version.isVersion(t4, 2));

        String t5 = "x_1.2..2.zx";

        Assert.assertFalse(Version.isVersion(t5, 2));
    }

    public void testParseVersion() throws Exception {
        Version v9_1_44 = Version.parseVersion("x_9.1.44", 2, "..");

        Assert.assertEquals("9.1.44", v9_1_44.toString());

        Assert.assertTrue(new Version(11, 1, 1, -1, null).compareTo(v9_1_44) > 0);

        Assert.assertTrue(new Version(9, 1, 44, -1, null).compareTo(v9_1_44) == 0);

        Assert.assertTrue(new Version(9, 1, 43, -1, null).compareTo(v9_1_44) < 0);

        String vx = "c:/exlipse/plugins/org.openl.eclipse.j_1.3.4/lib/apache/xyz_7.3.5.jar";

        Assert.assertEquals(new Version(7, 3, 5, -1, ".."), Version.extractVersion(vx, ".."));

    }

}
