package org.openl.rules.datatype.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;

/**
 * A datatype body either titles its columns or opens straight with a field. Everything that reads a datatype —
 * the binder that compiles it and the editor that shows it — decides that here, so they cannot disagree about
 * where the fields start.
 */
class DatatypeHelperColumnTitlesTest {

    private static ILogicalTable body(String[]... rows) {
        var table = mock(ILogicalTable.class);
        when(table.getHeight()).thenReturn(rows.length);
        when(table.getWidth()).thenReturn(rows.length == 0 ? 0 : rows[0].length);
        for (var rowId = 0; rowId < rows.length; rowId++) {
            var row = mock(ILogicalTable.class);
            when(table.getRow(rowId)).thenReturn(row);
            for (var column = 0; column < rows[rowId].length; column++) {
                var cell = mock(ICell.class);
                when(cell.getStringValue()).thenReturn(rows[rowId][column]);
                when(row.getCell(column, 0)).thenReturn(cell);
            }
        }
        return table;
    }

    @Test
    void aRowNamingBothTypeAndNameTitlesTheColumns() {
        var table = body(new String[]{"Type", "Name", "Default", "Mandatory", "Description", "Example"},
                new String[]{"String", "field1", "", "", "", ""});

        assertTrue(DatatypeHelper.hasColumnTitles(table));
        assertEquals(Map.of("Type", 0, "Name", 1, "Default", 2, "Mandatory", 3, "Description", 4, "Example", 5),
                DatatypeHelper.getColumnTitlesOrder(table));
    }

    // A titled body may write its columns in any order, so a reader takes the position from the title.
    @Test
    void titlesCarryTheirOwnPositions() {
        var table = body(new String[]{"Name", "Type"}, new String[]{"field1", "String"});

        assertEquals(Map.of("Name", 0, "Type", 1), DatatypeHelper.getColumnTitlesOrder(table));
    }

    // Without both of them the first row is already a field, which is what the legacy layout looks like.
    @Test
    void oneTitleAloneIsAFieldRow() {
        assertFalse(DatatypeHelper.hasColumnTitles(body(new String[]{"Type", "field1"})));
        assertFalse(DatatypeHelper.hasColumnTitles(body(new String[]{"String", "Name"})));
        assertTrue(DatatypeHelper.getColumnTitlesOrder(body(new String[]{"Type", "field1"})).isEmpty());
    }

    @Test
    void aLegacyBodyDeclaresNoTitles() {
        var table = body(new String[]{"String", "field1", "someDefault"});

        assertFalse(DatatypeHelper.hasColumnTitles(table));
        assertTrue(DatatypeHelper.getColumnTitlesOrder(table).isEmpty());
    }

    // A title row may leave a cell blank; asking an immutable list about null is not allowed.
    @Test
    void aBlankTitleCellIsNotATitle() {
        var table = body(new String[]{"Type", "Name", null, ""}, new String[]{"String", "field1", "", ""});

        assertTrue(DatatypeHelper.hasColumnTitles(table));
        assertEquals(Map.of("Type", 0, "Name", 1), DatatypeHelper.getColumnTitlesOrder(table));
    }

    @Test
    void anEmptyBodyDeclaresNoTitles() {
        assertFalse(DatatypeHelper.hasColumnTitles(body()));
        assertFalse(DatatypeHelper.hasColumnTitles(null));
    }
}
