package org.openl.rules.common.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CommonVersionImplTest {

    @Test
    void testCompareToTheSameRevision() {
        var version1 = new CommonVersionImpl("2.3.17");
        var version2 = new CommonVersionImpl("7.5.17");
        assertEquals(0, version1.compareTo(version2));
        assertEquals(0, version2.compareTo(version1));
        assertEquals(version1, version2);
        assertEquals(version2, version1);
    }

    @Test
    void testCompareToZeroRevision() {
        var version1 = new CommonVersionImpl("2.3.0");
        var version2 = new CommonVersionImpl("7.5.0");
        assertEquals(0, version1.compareTo(version2));
        assertEquals(0, version2.compareTo(version1));
        assertEquals(version1, version2);
        assertEquals(version2, version1);
    }

    @ParameterizedTest(name = "{0} < {1}")
    @CsvSource({"2.3.0, 2.3.1", "2.4.0, 2.3.1", "2.3.0, 2.4.1", "2.3.2, 2.4.1", "2.3.1, 2.4.2", "1.3.2, 2.4.1"})
    void testCompareTo(String lower, String higher) {
        var version1 = new CommonVersionImpl(lower);
        var version2 = new CommonVersionImpl(higher);
        assertEquals(-1, version1.compareTo(version2));
        assertEquals(1, version2.compareTo(version1));
        assertNotEquals(version1, version2);
        assertNotEquals(version2, version1);
    }

    @Test
    void testRevision() {
        var version = new CommonVersionImpl("17");
        assertEquals(32767, version.getMajor());
        assertEquals(32767, version.getMinor());
        assertEquals("17", version.getRevision());
        assertEquals("17", version.getVersionName());
    }

    @Test
    void testVersion() {
        var version = new CommonVersionImpl("34.6");
        assertEquals(34, version.getMajor());
        assertEquals(6, version.getMinor());
        assertEquals("0", version.getRevision());
        assertEquals("34.6.0", version.getVersionName());
    }

    @Test
    void testVersionAndRevision() {
        var version = new CommonVersionImpl("2.7.4");
        assertEquals(2, version.getMajor());
        assertEquals(7, version.getMinor());
        assertEquals("4", version.getRevision());
        assertEquals("2.7.4", version.getVersionName());
    }

    @Test
    void testExtraVersion() {
        var version = new CommonVersionImpl("3.5.7.a11");
        assertEquals(3, version.getMajor());
        assertEquals(5, version.getMinor());
        assertEquals("7", version.getRevision());
        assertEquals("3.5.7", version.getVersionName());
    }
}
