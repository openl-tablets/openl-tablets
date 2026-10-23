package org.openl.studio.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EntityVersionsTest {

    @Test
    void anEntityTagIsTheVersionQuoted() {
        assertEquals("\"v1\"", EntityVersions.entityTag("v1"));
    }

    @Test
    void anIfMatchHeaderNamesTheVersionOfItsTag() {
        assertEquals("v1", EntityVersions.expectedVersion(" \"v1\" "));
        assertEquals("v1", EntityVersions.expectedVersion("v1"), "an unquoted tag is taken as it is");
    }

    /** If-Match compares tags strongly: what names no single strong tag matches no version. */
    @Test
    void aWeakTagAnEmptyTagAndAListMatchNoVersion() {
        assertEquals("W/\"v1\"", EntityVersions.expectedVersion("W/\"v1\""));
        assertEquals("", EntityVersions.expectedVersion("\"\""));
        assertEquals("v1\", \"v2", EntityVersions.expectedVersion("\"v1\", \"v2\""));
    }

    @Test
    void anIfMatchHeaderThatNamesNoVersionAsksForNoCheck() {
        assertNull(EntityVersions.expectedVersion(null));
        assertNull(EntityVersions.expectedVersion(" "));
        assertNull(EntityVersions.expectedVersion("*"));
    }
}
