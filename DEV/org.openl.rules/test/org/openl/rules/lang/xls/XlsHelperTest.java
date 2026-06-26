package org.openl.rules.lang.xls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class XlsHelperTest {

    @Test
    void recognizesKnownTableHeaders() {
        assertTrue(XlsHelper.isKnownTableHeader("Datatype"));
        assertTrue(XlsHelper.isKnownTableHeader("Rules"));
        assertTrue(XlsHelper.isKnownTableHeader("Spreadsheet"));
        // only the first token decides the type
        assertTrue(XlsHelper.isKnownTableHeader("Datatype Greeting"));
        assertTrue(XlsHelper.isKnownTableHeader("Rules void greeting(int hour)"));
        // keyword aliases are recognized too
        assertTrue(XlsHelper.isKnownTableHeader("DT void rule()"));
        assertTrue(XlsHelper.isKnownTableHeader("Calc result"));
        assertTrue(XlsHelper.isKnownTableHeader("Test greeting"));
    }

    @Test
    void rejectsUnknownTableHeaders() {
        assertFalse(XlsHelper.isKnownTableHeader("Foo"));
        assertFalse(XlsHelper.isKnownTableHeader("NotATable bar"));
        assertFalse(XlsHelper.isKnownTableHeader("42"));
        assertFalse(XlsHelper.isKnownTableHeader(""));
        assertFalse(XlsHelper.isKnownTableHeader("   "));
        assertFalse(XlsHelper.isKnownTableHeader(null));
    }
}
