package org.openl.rules.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.rules.dt.DecisionTableHelper;

/**
 * Covers the cells a transformation empties or widens, in the transformed table and in its transposed one.
 */
class TransformedGridTableTest {

    /** The table as it stands: its first cell two columns wide, its last cell empty. */
    private static final CoordinatesTransformer SHAPED = new CoordinatesTransformer() {
        @Override
        public int getHeight() {
            return 2;
        }

        @Override
        public int getWidth() {
            return 2;
        }

        @Override
        public int getColumn(int col, int row) {
            return col;
        }

        @Override
        public int getRow(int col, int row) {
            return row;
        }

        @Override
        public boolean isEmpty(int col, int row) {
            return col == 1 && row == 1;
        }

        @Override
        public int getCellWidth(int col, int row) {
            return col == 0 && row == 0 ? 2 : 0;
        }
    };

    /** The cells a, b on the first row and c, d on the second. */
    private static IGridTable source() {
        var grid = DecisionTableHelper.createVirtualGrid();
        grid.setCellValue(0, 0, "a");
        grid.setCellValue(1, 0, "b");
        grid.setCellValue(0, 1, "c");
        grid.setCellValue(1, 1, "d");
        return new GridTable(0, 0, 1, 1, grid);
    }

    @Test
    void emptiesACellWhereItsSourceCellStands() {
        var source = source();
        var empty = new TransformedGridTable(source, SHAPED).getCell(1, 1);
        var sourceCell = source.getCell(1, 1);

        assertNull(empty.getObjectValue());
        assertNull(empty.getStringValue());
        assertNull(empty.getFormula());
        assertNull(empty.getComment());
        assertNull(empty.getNativeDate());
        assertEquals(0, empty.getNativeNumber());
        assertFalse(empty.getNativeBoolean());
        assertFalse(empty.hasNativeType());
        assertEquals(IGrid.CELL_TYPE_BLANK, empty.getType());
        assertEquals(IGrid.CELL_TYPE_BLANK, empty.getNativeType());
        assertTrue(empty.getTextRuns().isEmpty());
        assertNull(empty.getRegion());
        assertSame(empty, empty.getTopLeftCellFromRegion());
        assertEquals(1, empty.getWidth());
        assertEquals(1, empty.getHeight());

        assertEquals(sourceCell.getRow(), empty.getRow());
        assertEquals(sourceCell.getColumn(), empty.getColumn());
        assertEquals(1, empty.getAbsoluteRow());
        assertEquals(1, empty.getAbsoluteColumn());
        assertEquals(1, empty.getAbsoluteRegion().getTop());
        assertEquals(1, empty.getAbsoluteRegion().getLeft());
        assertEquals(sourceCell.getUri(), empty.getUri());
    }

    @Test
    void widensACellKeepingItsValue() {
        var table = new TransformedGridTable(source(), SHAPED);
        var wide = table.getCell(0, 0);

        assertEquals("a", wide.getStringValue());
        assertEquals("a", wide.getObjectValue());
        assertEquals(IGrid.CELL_TYPE_STRING, wide.getType());
        assertEquals(2, wide.getWidth());
        assertEquals(1, wide.getHeight());
        assertEquals("a", wide.getTopLeftCellFromRegion().getStringValue());
        assertFalse(table.getCell(1, 0) instanceof TransformedCell);
        assertEquals("b", table.getCell(1, 0).getStringValue());
    }

    @Test
    void transposedTableIsReadThroughTheTransformation() {
        var transposed = new TransformedGridTable(source(), SHAPED).transpose();

        assertInstanceOf(TransformedGridTable.class, transposed);
        assertFalse(transposed.isNormalOrientation());
        assertEquals(2, transposed.getWidth());
        assertEquals(2, transposed.getHeight());
        assertEquals("c", transposed.getCell(1, 0).getStringValue());
        assertEquals("b", transposed.getCell(0, 1).getStringValue());
        assertNull(transposed.getCell(1, 1).getStringValue());
        assertEquals(2, transposed.getCell(0, 0).getHeight());
        assertEquals(1, transposed.getCell(0, 0).getWidth());

        // A part of the table is read through the transformation as well, in both orientations
        var grid = source();
        var column = new TransformedGridTable(grid, SHAPED).getSubtable(1, 0, 1, 2);
        assertEquals("b", column.getCell(0, 0).getStringValue());
        assertNull(column.getCell(0, 1).getStringValue());
        var row = column.transpose();
        assertEquals("b", row.getCell(0, 0).getStringValue());
        assertNull(row.getCell(1, 0).getStringValue());
        assertEquals(grid.getSubtable(1, 1, 1, 1).getUri(), column.getSubtable(0, 1, 1, 1).getUri());
        assertNull(column.getSubtable(0, 1, 1, 1).getCell(0, 0).getStringValue());

        var back = transposed.transpose();
        assertTrue(back.isNormalOrientation());
        assertEquals("b", back.getCell(1, 0).getStringValue());
        assertEquals(2, back.getCell(0, 0).getWidth());
        assertNull(back.getCell(1, 1).getStringValue());
    }
}
