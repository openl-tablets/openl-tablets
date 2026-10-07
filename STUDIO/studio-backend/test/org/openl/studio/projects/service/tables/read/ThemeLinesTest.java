package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.openl.studio.projects.model.tables.RawTableBorderLineStyle;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellBorder;
import org.openl.studio.projects.model.tables.RawTableCellBorderSide;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableStyleSource;

/** The lines of the theme moved onto the cell above or on the left, as the screen draws the workbook. */
class ThemeLinesTest {

    private static final String RED = "#ff0000";

    /** The rows read end the table: no row is under them. */
    private static final IntFunction<@Nullable RawTableCellBorderSide> NOTHING_UNDER = column -> null;

    @Test
    void movesTheTopLineOfACellOntoTheCellAbove() {
        var matrix = column(themed(RawTableCellBorder.builder().build()),
                themed(RawTableCellBorder.builder().top(line(1, null)).build()));

        ThemeLines.share(matrix, NOTHING_UNDER);

        assertEquals(line(1, null), borderOf(matrix, 0, 0).bottom());
        assertNull(matrix.get(1).getFirst().style().border(), "The lower cell keeps no line of its own");
    }

    @Test
    void drawsTheLineOfTheUpperCellAsWideAsTheWiderOfTheTwo() {
        // The workbook draws the line of the upper cell where both cells name one, as wide as the wider of them.
        var matrix = column(themed(RawTableCellBorder.builder().bottom(line(1, RED)).build()),
                themed(RawTableCellBorder.builder().top(line(2, null)).build()));

        ThemeLines.share(matrix, NOTHING_UNDER);

        assertEquals(line(2, RED), borderOf(matrix, 0, 0).bottom());
        assertNull(matrix.get(1).getFirst().style().border());
    }

    @Test
    void keepsTheLineOfTheLeftCellWhereBothNameOne() {
        List<List<RawTableCell>> matrix = new ArrayList<>();
        matrix.add(new ArrayList<>(List.of(themed(RawTableCellBorder.builder().right(line(2, RED)).build()),
                themed(RawTableCellBorder.builder().left(line(1, null)).build()))));

        ThemeLines.share(matrix, NOTHING_UNDER);

        assertEquals(line(2, RED), borderOf(matrix, 0, 0).right());
        assertNull(matrix.getFirst().get(1).style().border());
    }

    @Test
    void drawsTheLineOverTheRowUnderTheRowsReadByTheLastOfThem() {
        var matrix = column(themed(RawTableCellBorder.builder().build()), themed(RawTableCellBorder.builder().build()));

        ThemeLines.share(matrix, column -> line(1, RED));

        // Only the last row read meets the row under the rows read.
        assertNull(matrix.getFirst().getFirst().style().border());
        assertEquals(line(1, RED), borderOf(matrix, 1, 0).bottom());
    }

    @Test
    void leavesTheLineOfACellBesideOneTheThemeDoesNotReach() {
        // The upper cell keeps the style of the workbook: the line over the lower cell stays where the theme drew it.
        var workbook = RawTableCell.builder().value("x").style(RawTableCellStyle.builder().bold(true).build()).build();
        var matrix = column(workbook, themed(RawTableCellBorder.builder().top(line(1, null)).build()));

        ThemeLines.share(matrix, NOTHING_UNDER);

        assertNull(matrix.getFirst().getFirst().style().border(), "The workbook cell takes no line of the theme");
        assertEquals(line(1, null), borderOf(matrix, 1, 0).top());
    }

    private static RawTableCellBorderSide line(int width, @Nullable String color) {
        return RawTableCellBorderSide.builder().style(RawTableBorderLineStyle.SOLID).width(width).color(color).build();
    }

    private static RawTableCell themed(RawTableCellBorder border) {
        var style = RawTableCellStyle.builder()
                .border(border.isEmpty() ? null : border)
                .source(RawTableStyleSource.THEME)
                .build();
        return RawTableCell.builder().value("x").style(style).build();
    }

    private static List<List<RawTableCell>> column(RawTableCell upper, RawTableCell lower) {
        List<List<RawTableCell>> matrix = new ArrayList<>();
        matrix.add(new ArrayList<>(List.of(upper)));
        matrix.add(new ArrayList<>(List.of(lower)));
        return matrix;
    }

    private static RawTableCellBorder borderOf(List<List<RawTableCell>> matrix, int row, int column) {
        return matrix.get(row).get(column).style().border();
    }
}
