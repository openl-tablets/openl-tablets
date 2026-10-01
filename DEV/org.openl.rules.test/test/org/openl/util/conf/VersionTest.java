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
class VersionTest {

    @Test
    void testIsVersion() {
        var t1 = "1.2.3";

        assertTrue(Version.isVersion(t1, 0, ".."));

        var t2 = "x_12312.212.322.zx";

        assertTrue(Version.isVersion(t2, 2, ".."));

        var t3 = "x_2.2.2_04.zx";

        assertTrue(Version.isVersion(t3, 2));

        var t4 = "x_.12312.212.322.zx";

        assertFalse(Version.isVersion(t4, 2));

        var t5 = "x_1.2..2.zx";

        assertFalse(Version.isVersion(t5, 2));
    }

    @Test
    void testParseVersion() {
        Version version = Version.parseVersion("x_9.1.44", 2, "..");

        assertEquals("9.1.44", version.toString());

        assertTrue(new Version(11, 1, 1, -1, null).compareTo(version) > 0);

        assertEquals(0, new Version(9, 1, 44, -1, null).compareTo(version));

        assertTrue(new Version(9, 1, 43, -1, null).compareTo(version) < 0);

        var vx = "c:/exlipse/plugins/org.openl.eclipse.j_1.3.4/lib/apache/xyz_7.3.5.jar";

        assertEquals(new Version(7, 3, 5, -1, ".."), Version.extractVersion(vx, ".."));

    }

}
