package org.openl.rules.common.impl;

import static org.junit.Assert.*;

import org.junit.Test;

public class CommonVersionImplTest {

    @Test
    public void testCompareToTheSameRevision() throws Exception {
        CommonVersionImpl version1 = new CommonVersionImpl("2.3.17");
        CommonVersionImpl version2 = new CommonVersionImpl("7.5.17");
        assertEquals(0, version1.compareTo(version2));
        assertEquals(0, version2.compareTo(version1));
        assertTrue(version1.equals(version2));
        assertTrue(version2.equals(version1));
    }

    @Test
    public void testCompareToZeroRevision() throws Exception {
        CommonVersionImpl version1 = new CommonVersionImpl("2.3.0");
        CommonVersionImpl version2 = new CommonVersionImpl("7.5.0");
        assertEquals(0, version1.compareTo(version2));
        assertEquals(0, version2.compareTo(version1));
        assertTrue(version1.equals(version2));
        assertTrue(version2.equals(version1));
    }

    @Test
    public void testCompareToRevision() throws Exception {
        CommonVersionImpl version1 = new CommonVersionImpl("2.3.0");
        CommonVersionImpl version2 = new CommonVersionImpl("2.3.1");
        assertEquals(-1, version1.compareTo(version2));
        assertEquals(1, version2.compareTo(version1));
        assertFalse(version1.equals(version2));
        assertFalse(version2.equals(version1));
    }

    @Test
    public void testCompareToRevision2() throws Exception {
        CommonVersionImpl version1 = new CommonVersionImpl("2.4.0");
        CommonVersionImpl version2 = new CommonVersionImpl("2.3.1");
        assertEquals(-1, version1.compareTo(version2));
        assertEquals(1, version2.compareTo(version1));
        assertFalse(version1.equals(version2));
        assertFalse(version2.equals(version1));
    }

    @Test
    public void testCompareToRevision3() throws Exception {
        CommonVersionImpl version1 = new CommonVersionImpl("2.3.0");
        CommonVersionImpl version2 = new CommonVersionImpl("2.4.1");
        assertEquals(-1, version1.compareTo(version2));
        assertEquals(1, version2.compareTo(version1));
        assertFalse(version1.equals(version2));
        assertFalse(version2.equals(version1));
    }

    @Test
    public void testCompareToMinor() throws Exception {
        CommonVersionImpl version1 = new CommonVersionImpl("2.3.2");
        CommonVersionImpl version2 = new CommonVersionImpl("2.4.1");
        assertEquals(-1, version1.compareTo(version2));
        assertEquals(1, version2.compareTo(version1));
        assertFalse(version1.equals(version2));
        assertFalse(version2.equals(version1));
    }

    @Test
    public void testCompareToMinor2() throws Exception {
        CommonVersionImpl version1 = new CommonVersionImpl("2.4.2");
        CommonVersionImpl version2 = new CommonVersionImpl("2.3.1");
        assertEquals(1, version1.compareTo(version2));
        assertEquals(-1, version2.compareTo(version1));
        assertFalse(version1.equals(version2));
        assertFalse(version2.equals(version1));
    }

    @Test
    public void testCompareToMajor() throws Exception {
        CommonVersionImpl version1 = new CommonVersionImpl("1.3.2");
        CommonVersionImpl version2 = new CommonVersionImpl("2.4.1");
        assertEquals(-1, version1.compareTo(version2));
        assertEquals(1, version2.compareTo(version1));
        assertFalse(version1.equals(version2));
        assertFalse(version2.equals(version1));
    }

    @Test
    public void testRevision() throws Exception {
        CommonVersionImpl version = new CommonVersionImpl("17");
        assertEquals(32767, version.getMajor());
        assertEquals(32767, version.getMinor());
        assertEquals("17", version.getRevision());
        assertEquals("17", version.getVersionName());
    }

    @Test
    public void testVersion() throws Exception {
        CommonVersionImpl version = new CommonVersionImpl("34.6");
        assertEquals(34, version.getMajor());
        assertEquals(6, version.getMinor());
        assertEquals("0", version.getRevision());
        assertEquals("34.6.0", version.getVersionName());
    }

    @Test
    public void testVersionAndRevision() throws Exception {
        CommonVersionImpl version = new CommonVersionImpl("2.7.4");
        assertEquals(2, version.getMajor());
        assertEquals(7, version.getMinor());
        assertEquals("4", version.getRevision());
        assertEquals("2.7.4", version.getVersionName());
    }

    @Test
    public void testExtraVersion() throws Exception {
        CommonVersionImpl version = new CommonVersionImpl("3.5.7.a11");
        assertEquals(3, version.getMajor());
        assertEquals(5, version.getMinor());
        assertEquals("7", version.getRevision());
        assertEquals("3.5.7", version.getVersionName());
    }
}
