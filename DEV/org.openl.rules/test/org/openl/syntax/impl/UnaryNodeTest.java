package org.openl.syntax.impl;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class UnaryNodeTest {

    @Test
    void replacesOperand() {
        var node = new UnaryNode("op.unary.negative", null, new IdentifierNode("identifier", null, "a", null), null);
        var operand = new IdentifierNode("identifier", null, "b", null);

        node.replaceLeft(operand);

        assertSame(operand, node.getChild(0));
    }
}
