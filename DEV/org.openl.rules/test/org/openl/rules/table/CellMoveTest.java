package org.openl.rules.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A reference follows the cells that move and keeps the address of the cells that stay.
 */
class CellMoveTest {

    /** The table at B2:D5. */
    private static final IGridRegion TABLE = new GridRegion(1, 1, 4, 3);

    @ParameterizedTest
    @CsvSource({
            // a column put in front of column C moves C and D right
            "D4, E4",
            "C2, D2",
            "C3:D3, D3:E3",
            "D2:D5, E2:E5",
            // a range over the place grows over the new column
            "B3:D3, B3:E3",
            "A4:D4, A4:E4",
            // a range past the end of the table keeps the cells moved into it
            "B4:H4, B4:H4",
            "C4:H4, D4:H4"})
    void columnsInserted(String area, String expected) {
        assertEquals(expected, moved(new CellMove.Shift(TABLE, CellMove.Axis.COLUMNS, 2, 1), area));
    }

    @ParameterizedTest
    @ValueSource(strings = {"B4", "A4", "E4", "D1", "D6", "D10", "C1:D5", "C:C", "B2:B5"})
    void cellsThatStayKeepTheirAddress(String area) {
        var region = region(area);
        assertSame(region, new CellMove.Shift(TABLE, CellMove.Axis.COLUMNS, 2, 1).moved(region));
    }

    /** Lines put after the last line of the table move no cell, so the cells beside the table keep their address. */
    @ParameterizedTest
    @ValueSource(strings = {"E4", "F4", "D4", "B4:E4", "E2:E5"})
    void linesAddedAtTheEndMoveNothing(String area) {
        var region = region(area);
        assertSame(region, new CellMove.Shift(TABLE, CellMove.Axis.COLUMNS, 4, 1).moved(region));
        assertSame(region, new CellMove.Shift(TABLE, CellMove.Axis.ROWS, 5, 2).moved(region));
    }

    @ParameterizedTest
    @CsvSource({
            // a row put under row 3 moves rows 4 and 5 down
            "C4, C5",
            "C5, C6",
            "C3, C3",
            "C3:C5, C3:C6",
            "B4:D5, B5:D6",
            "F4, F4"})
    void rowsInserted(String area, String expected) {
        assertEquals(expected, moved(new CellMove.Shift(TABLE, CellMove.Axis.ROWS, 3, 1), area));
    }

    @ParameterizedTest
    @CsvSource({
            // column C is taken away, column D moves into its place
            "D4, C4",
            "B4, B4",
            "B3:D3, B3:C3",
            "C3:D3, C3",
            "B3:C3, B3",
            "A3:H3, A3:H3"})
    void columnsRemoved(String area, String expected) {
        assertEquals(expected, moved(new CellMove.Shift(TABLE, CellMove.Axis.COLUMNS, 3, -1), area));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C4", "C2:C5"})
    void cellsTakenAwayAreLost(String area) {
        assertNull(new CellMove.Shift(TABLE, CellMove.Axis.COLUMNS, 3, -1).moved(region(area)));
    }

    @ParameterizedTest
    @CsvSource({
            // the table moves from B2:D5 to F12:H15
            "C3, G13",
            "B2:D5, F12:H15",
            "C3:D4, G13:H14",
            // a range reaching out of the table stays
            "A1:C3, A1:C3",
            "D4:E4, D4:E4",
            "J9, J9"})
    void tableTransferred(String area, String expected) {
        assertEquals(expected, moved(new CellMove.Transfer(TABLE, new GridRegion(11, 5, 14, 7)), area));
    }

    private static String moved(CellMove move, String area) {
        var moved = move.moved(region(area));
        var range = new CellRangeAddress(moved.getTop(), moved.getBottom(), moved.getLeft(), moved.getRight());
        return area.indexOf(':') < 0 ? range.formatAsString().split(":")[0] : range.formatAsString();
    }

    private static IGridRegion region(String area) {
        var range = CellRangeAddress.valueOf(area);
        var top = range.getFirstRow() < 0 ? 0 : range.getFirstRow();
        var bottom = range.getLastRow() < 0 ? 1048575 : range.getLastRow();
        return new GridRegion(top, range.getFirstColumn(), bottom, range.getLastColumn());
    }
}
