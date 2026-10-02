package org.openl.syntax.impl;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class BinaryNodeTest {

    @Test
    void replacesLeftOperandOnly() {
        var right = new IdentifierNode("identifier", null, "b", null);
        var node = new BinaryNode("op.binary.add", null, new IdentifierNode("identifier", null, "a", null), right, null);
        var left = new IdentifierNode("identifier", null, "c", null);

        node.replaceLeft(left);

        assertSame(left, node.getChild(0));
        assertSame(right, node.getChild(1));
    }
}
