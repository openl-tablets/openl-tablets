package org.openl.rules.tableeditor.model.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.junit.jupiter.api.Test;

import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.ui.ICellStyle;

/**
 * The alignment and the indent a cell is laid out with, as Excel draws them.
 */
class TableViewerTest {

    private final TableViewer viewer = new TableViewer(mock(IGrid.class), null, null);

    private static ICell cell(HorizontalAlignment align, int indent, int nativeType) {
        var style = mock(ICellStyle.class);
        when(style.getHorizontalAlignment()).thenReturn(align);
        when(style.getVerticalAlignment()).thenReturn(VerticalAlignment.BOTTOM);
        when(style.getIndent()).thenReturn(indent);
        var cell = mock(ICell.class);
        when(cell.getStyle()).thenReturn(style);
        when(cell.getNativeType()).thenReturn(nativeType);
        return cell;
    }

    private CellModel laidOut(ICell cell) {
        return viewer.buildCell(cell, new CellModel(0, 0));
    }

    @Test
    void keepsTheIndentOfACell() {
        var model = laidOut(cell(HorizontalAlignment.GENERAL, 4, IGrid.CELL_TYPE_STRING));

        assertEquals(4, model.getIndent());
        assertNull(model.getHalign());
    }

    @Test
    void alignsANumberRight() {
        assertEquals("right", laidOut(cell(HorizontalAlignment.GENERAL, 0, IGrid.CELL_TYPE_NUMERIC)).getHalign());
    }

    @Test
    void alignsAnIndentedNumberLeft() {
        var model = laidOut(cell(HorizontalAlignment.GENERAL, 2, IGrid.CELL_TYPE_NUMERIC));

        assertNull(model.getHalign());
        assertEquals(2, model.getIndent());
    }

    @Test
    void centersAcrossTheSelection() {
        assertEquals("center",
                laidOut(cell(HorizontalAlignment.CENTER_SELECTION, 0, IGrid.CELL_TYPE_STRING)).getHalign());
    }
}
