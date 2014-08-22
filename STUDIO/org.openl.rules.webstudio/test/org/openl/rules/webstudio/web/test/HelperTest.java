package org.openl.rules.webstudio.web.test;

import org.junit.Test;
import org.richfaces.model.TreeNode;

import static org.junit.Assert.assertSame;

public class HelperTest {

    @Test
    public void testGetRoot() {
        Helper helper = new Helper();
        SimpleParameterTreeNode parameter = new SimpleParameterTreeNode("FN", 123, null, null);
        TreeNode root = helper.getRoot(parameter);
        TreeNode child = root.getChild("FN");
        assertSame(parameter, child);
    }
}

