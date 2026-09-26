package org.openl.rules.lang.xls.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;

class HeaderSyntaxNodeTest {

    @Test
    void readsTheTextOfItsCell() {
        var node = new HeaderSyntaxNode(cellWith("Rules String greeting(Integer hour)"), null);

        assertEquals("Rules String greeting(Integer hour)", node.getSourceString());
    }

    @Test
    void anEmptyCellGivesAnEmptyText() {
        var node = new HeaderSyntaxNode(cellWith(null), null, false, new String[0]);

        assertEquals("", node.getSourceString());
    }

    @Test
    void refusesAHeaderWithoutItsCell() {
        var error = assertThrows(NullPointerException.class, () -> new HeaderSyntaxNode(null, null));

        assertEquals("module", error.getMessage());
    }

    private static GridCellSourceCodeModule cellWith(String text) {
        var table = mock(IGridTable.class);
        var cell = mock(ICell.class);
        when(table.getCell(0, 0)).thenReturn(cell);
        when(cell.getStringValue()).thenReturn(text);
        return new GridCellSourceCodeModule(table);
    }
}
