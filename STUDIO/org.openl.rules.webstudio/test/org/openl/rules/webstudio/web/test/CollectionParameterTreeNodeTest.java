package org.openl.rules.webstudio.web.test;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.openl.types.java.JavaOpenClass;

public class CollectionParameterTreeNodeTest {
    @Test
    public void nullValuesInPrimitiveArrays() {
        JavaOpenClass type = JavaOpenClass.getOpenClass(int[].class);
        ParameterRenderConfig config = new ParameterRenderConfig.Builder(type, null).build();
        CollectionParameterTreeNode node = new CollectionParameterTreeNode(config);

        // Check that addChild can handle null arguments
        node.addChild(0, null);
        assertArrayEquals(new int[] { 0 }, (int[]) node.getValueForced());

        // Check that replaceChild can handle null arguments
        node.replaceChild(node.getChild(0), new SimpleParameterTreeNode("0", 1, JavaOpenClass.INT, node));
        assertArrayEquals(new int[] { 1 }, (int[]) node.getValueForced());
        node.replaceChild(node.getChild(0), new SimpleParameterTreeNode("0", null, JavaOpenClass.INT, node));
        assertArrayEquals(new int[] { 0 }, (int[]) node.getValueForced());
    }
}