package org.openl.rules.lang.xls.syntax;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CellSyntaxNodeTest {

    @Test
    void aCellWithoutSourceHasNoText() {
        var node = new CellSyntaxNode("cell", null);

        assertNull(node.getCellSource());
        assertNull(node.getSourceString());
    }
}
