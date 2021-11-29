package org.openl.rules.webstudio.web.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.rules.calc.StubSpreadSheetResult;
import org.richfaces.model.TreeNode;

public class HelperTest {

    @Test
    public void testGetRoot() {
        Helper helper = new Helper();
        SimpleParameterTreeNode parameter = new SimpleParameterTreeNode("FN", 123, null, null);
        TreeNode root = helper.getRoot(parameter);
        TreeNode child = root.getChild("FN");
        assertSame(parameter, child);
    }

    @Test
    public void testFormat() {
        Helper helper = new Helper();
        assertEquals("null", helper.format(null));
        assertEquals("Str", helper.format("Str"));
        assertEquals("1", helper.format(1));
        assertEquals("0.1", helper.format(0.1));
        assertEquals("true", helper.format(true));
        assertEquals("foo,bar", helper.format(new String[] { "foo", "bar" }));
    }

    @Test
    public void testFormatText() {
        Helper helper = new Helper();
        assertEquals("1.759999999999998", helper.formatText(1.759999999999998, true));
        assertEquals("null", helper.format(null));
        assertEquals("Str", helper.format("Str"));
        assertEquals("true", helper.format(true));
        assertEquals("foo,bar", helper.format(new String[] { "foo", "bar" }));
    }

    @Test
    public void testIsSpreadsheetResult() {
        Helper helper = new Helper();
        assertTrue(helper.isSpreadsheetResult(new StubSpreadSheetResult()));
        assertFalse(helper.isSpreadsheetResult(null));
        assertFalse(helper.isSpreadsheetResult("Str"));
    }
}
