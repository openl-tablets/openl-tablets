package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TableKindTest {

    @Test
    void mapsDisplayLabelsBothWays() {
        assertEquals("Rules", TableKind.RULES.value());
        assertEquals("Column Match", TableKind.COLUMN_MATCH.value());
        assertEquals("Other", TableKind.OTHER.value());
        assertEquals(TableKind.RULES, TableKind.fromValue("Rules"));
        assertEquals(TableKind.COLUMN_MATCH, TableKind.fromValue("Column Match"));
        assertEquals(TableKind.OTHER, TableKind.fromValue("Other"));
    }

    @Test
    void rejectsAValueOutsideTheTableKinds() {
        // Dispatcher is a graph node kind, not a table kind, so it is not a TableKind.
        assertThrows(IllegalArgumentException.class, () -> TableKind.fromValue("Dispatcher"));
        assertThrows(IllegalArgumentException.class, () -> TableKind.fromValue("Nope"));
    }

    @Test
    void graphNodeKindReusesTableLabelsAndAddsDispatcher() {
        assertEquals("Rules", TableGraphNodeKind.RULES.value());
        assertEquals("Column Match", TableGraphNodeKind.COLUMN_MATCH.value());
        assertEquals("Dispatcher", TableGraphNodeKind.DISPATCHER.value());
        assertEquals(TableGraphNodeKind.RULES, TableGraphNodeKind.fromValue("Rules"));
        assertEquals(TableGraphNodeKind.DISPATCHER, TableGraphNodeKind.fromValue("Dispatcher"));
    }

    @Test
    void graphNodeKindRejectsUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> TableGraphNodeKind.fromValue("Nope"));
    }
}
