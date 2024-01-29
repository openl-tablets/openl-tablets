/*
 * Created on May 25, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author snshor
 */
public class VersionTest {

    /*
     * Test for boolean equals(Object)
     */
    @Test
    public void testEqualsObject() {
    }

    @Test
    public void testIsVersion() {
        String t1 = "1.2.3";

        assertTrue(Version.isVersion(t1, 0, ".."));

        String t2 = "x_12312.212.322.zx";

        assertTrue(Version.isVersion(t2, 2, ".."));

        String t3 = "x_2.2.2_04.zx";

        assertTrue(Version.isVersion(t3, 2));

        String t4 = "x_.12312.212.322.zx";

        assertFalse(Version.isVersion(t4, 2));

        String t5 = "x_1.2..2.zx";

        assertFalse(Version.isVersion(t5, 2));
    }

    @Test
    public void testParseVersion() throws Exception {
        Version v9_1_44 = Version.parseVersion("x_9.1.44", 2, "..");

        assertEquals("9.1.44", v9_1_44.toString());

        assertTrue(new Version(11, 1, 1, -1, null).compareTo(v9_1_44) > 0);

        assertEquals(0, new Version(9, 1, 44, -1, null).compareTo(v9_1_44));

        assertTrue(new Version(9, 1, 43, -1, null).compareTo(v9_1_44) < 0);

        String vx = "c:/exlipse/plugins/org.openl.eclipse.j_1.3.4/lib/apache/xyz_7.3.5.jar";

        assertEquals(new Version(7, 3, 5, -1, ".."), Version.extractVersion(vx, ".."));

    }

}
