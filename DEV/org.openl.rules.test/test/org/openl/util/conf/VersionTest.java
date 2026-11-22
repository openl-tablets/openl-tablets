/*
 * Created on May 25, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author snshor
 */
class VersionTest {

    @Test
    void testParseValidVersions() {
        assertEquals("1.2.3", Version.parseVersion("1.2.3", 0, "..").toString());
        assertEquals("12312.212.322", Version.parseVersion("x_12312.212.322.zx", 2, "..").toString());
        assertEquals("2.2.2_4", Version.parseVersion("x_2.2.2_04.zx", 2, ".._").toString());
    }

    @Test
    void testParseInvalidVersions() {
        assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("x_.12312.212.322.zx", 2, ".._"));
        assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("x_1.2..2.zx", 2, ".._"));
        assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("", 0, ".."));
    }

    @Test
    void testParseVersion() {
        var version = Version.parseVersion("x_9.1.44", 2, "..");

        assertEquals("9.1.44", version.toString());

        assertTrue(Version.parseVersion("11.1.1", 0, "..").compareTo(version) > 0);

        assertEquals(0, Version.parseVersion("9.1.44", 0, "..").compareTo(version));
        assertEquals(Version.parseVersion("9.1.44", 0, ".."), version);

        assertTrue(Version.parseVersion("9.1.43", 0, "..").compareTo(version) < 0);
    }

}
