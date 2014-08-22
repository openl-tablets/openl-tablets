package org.openl.rules.webstudio.web.test;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.richfaces.model.TreeNode;

import static org.junit.Assert.*;

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
        assertEquals("foo,bar", helper.format(new String[]{"foo", "bar"}));
    }

    @Test
    public void testIsExplanationValue() {
        Helper helper = new Helper();
        assertTrue(helper.isExplanationValue(DoubleValue.ONE));
        assertFalse(helper.isExplanationValue(null));
        assertFalse(helper.isExplanationValue("Str"));
    }
}

