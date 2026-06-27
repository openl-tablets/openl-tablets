package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the session-shared compilation holder's standalone logic (the syntax-node index and
 * shutdown safety). The end-to-end dependency reuse across projects is covered by the cross-dependency
 * integration test.
 */
class WorkspaceCompilationContextTest {

    @Test
    void syntaxNodeIndexIsKeyedPerProjectAndStablePerLookup() {
        var context = new WorkspaceCompilationContext(mock(WebStudio.class));

        var a1 = context.syntaxNodesByProject("A");
        var a2 = context.syntaxNodesByProject("A");
        var b = context.syntaxNodesByProject("B");

        assertSame(a1, a2, "same project must map to the same node set");
        assertNotSame(a1, b, "different projects must map to different node sets");
        assertTrue(context.syntaxNodesPerProject().containsKey("A"));
        assertTrue(context.syntaxNodesPerProject().containsKey("B"));
    }

    @Test
    void shutdownWithoutManagerClearsIndexAndDoesNotThrow() {
        var context = new WorkspaceCompilationContext(mock(WebStudio.class));
        context.syntaxNodesByProject("A");

        context.shutdown();

        assertTrue(context.syntaxNodesPerProject().isEmpty());
        assertTrue(context.syntaxNodes().isEmpty());
    }
}
