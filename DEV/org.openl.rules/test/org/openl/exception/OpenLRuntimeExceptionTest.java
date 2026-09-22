package org.openl.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.syntax.impl.IdentifierNode;

class OpenLRuntimeExceptionTest {

    @Test
    void aNodeWithoutSourceLeavesTheCodeUnknown() {
        var node = new IdentifierNode("identifier", null, "name", null);

        var exception = new OpenLRuntimeException("failed", node) {
        };

        assertEquals("failed", exception.getMessage());
        assertNull(exception.getSourceCode());
        assertEquals("NO_MODULE", exception.getSourceLocation());
    }
}
