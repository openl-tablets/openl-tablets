package org.openl.rules.webstudio.web.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.SimpleLogicalTable;

class HelperTest {

    @Test
    void testGetRoot() {
        var helper = new Helper();
        var parameter = new SimpleParameterTreeNode("FN", 123, null, null);
        var root = helper.getRoot(parameter);
        var child = root.getChild("FN");
        assertSame(parameter, child);
    }

    @Test
    void testFormat() {
        var helper = new Helper();
        assertEquals("null", helper.format(null));
        assertEquals("Str", helper.format("Str"));
        assertEquals("1", helper.format(1));
        assertEquals("0.1", helper.format(0.1));
        assertEquals("true", helper.format(true));
        assertEquals("foo,bar", helper.format(new String[]{"foo", "bar"}));
    }

    @Test
    void testFormatText() {
        var helper = new Helper();
        assertEquals("1.759999999999998", helper.formatText(1.759999999999998, true));
        assertEquals("null", helper.format(null));
        assertEquals("Str", helper.format("Str"));
        assertEquals("true", helper.format(true));
        assertEquals("foo,bar", helper.format(new String[]{"foo", "bar"}));
    }

    @Test
    void testIsSpreadsheetResult() {
        var sr = new SpreadsheetResult();
        sr.setLogicalTable(new SimpleLogicalTable(null)); // Real spreadsheet always contains a logical table from which it was created
        var helper = new Helper();
        assertTrue(helper.isSpreadsheetResult(sr));
        assertFalse(helper.isSpreadsheetResult(null));
        assertFalse(helper.isSpreadsheetResult("Str"));
    }
}
