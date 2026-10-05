package org.openl.studio.projects.service.tables.read;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellBorder;
import org.openl.studio.projects.model.tables.RawTableCellBorderSide;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableStyleSource;

/**
 * Moves the lines the table theme draws on the top or the left of a cell to the cell above it or on its left.
 *
 * <p>The screen draws the side of the upper or the left cell over the side of its neighbour. The workbook reader
 * gives a line two cells share to the upper or the left cell for that reason, and a line the theme gives the lower
 * or the right cell would be hidden there by the line of the grid. Moved the same way, the theme is drawn as the
 * workbook it is written into is drawn.
 *
 * <p>A line moves only to cells the theme reaches, the ones whose style names the theme as its source: the first row
 * of the table keeps its top, and a cell beside one the theme does not reach keeps its side. Where the neighbour
 * draws a line of its own on that side, the two are drawn as one, as the workbook draws them: the line of the upper or
 * the left cell, as wide as the wider of the two.
 *
 * <p>Rows read a window at a time meet the rows of the next window. The line the theme draws over the first row
 * under a window is drawn by the last row of the window as well, while the first row of the next window keeps it.
 * Windows drawn one under the other draw the line as the whole table does, and a window drawn alone draws it too.
 */
final class ThemeLines {

    /** The cells of the table, row by row, a cell covered by a merged region marked as covered. */
    private final List<List<RawTableCell>> matrix;

    /** The place of the cell holding each place of the table: the cell itself, or the one whose region covers it. */
    private final Place[][] owners;

    private ThemeLines(List<List<RawTableCell>> matrix) {
        this.matrix = matrix;
        this.owners = owners(matrix);
    }

    /**
     * Moves the lines of the theme across the cells of a table.
     *
     * @param matrix the cells of the table, row by row, a cell covered by a merged region marked as covered
     * @param below  the line the theme draws over the cell under the rows read, by the column of the cell, or
     *               {@code null} where it draws none or no row of the table is under them
     */
    static void share(List<List<RawTableCell>> matrix, IntFunction<@Nullable RawTableCellBorderSide> below) {
        new ThemeLines(matrix).share(below);
    }

    private void share(IntFunction<@Nullable RawTableCellBorderSide> below) {
        for (var row = 0; row < matrix.size(); row++) {
            for (var column = 0; column < matrix.get(row).size(); column++) {
                share(row, column);
            }
        }
        if (!matrix.isEmpty()) {
            closeLastRow(below);
        }
    }

    /** Draws the lines over the row under the rows read by the last row read, where the theme reaches it. */
    private void closeLastRow(IntFunction<@Nullable RawTableCellBorderSide> below) {
        var last = owners[owners.length - 1];
        for (var column = 0; column < last.length; column++) {
            var line = below.apply(column);
            var owner = last[column];
            if (line != null && owner != null && themed(Set.of(owner))) {
                change(owner, withBottom(line));
            }
        }
    }

    /** Moves the top and the left line of one cell, when the theme draws one there. */
    private void share(int row, int column) {
        var border = borderOf(matrix.get(row).get(column));
        if (border == null) {
            return;
        }
        var cell = matrix.get(row).get(column);
        var here = new Place(row, column);
        var top = border.top();
        if (top != null && row > 0) {
            move(here, ownersOf(row - 1, column, 1, spanOf(cell.colspan())),
                    withBottom(top),
                    sides -> sides.toBuilder().top(null).build());
        }
        var left = border.left();
        if (left != null && column > 0) {
            move(here, ownersOf(row, column - 1, spanOf(cell.rowspan()), 1),
                    sides -> sides.toBuilder().right(shared(sides.right(), left)).build(),
                    sides -> sides.toBuilder().left(null).build());
        }
    }

    /** Moves one line of a cell onto its neighbours on that side, when the theme reaches every one of them. */
    private void move(Place here, Set<Place> neighbours, UnaryOperator<RawTableCellBorder> toNeighbour,
                      UnaryOperator<RawTableCellBorder> fromHere) {
        if (themed(neighbours)) {
            neighbours.forEach(place -> change(place, toNeighbour));
            change(here, fromHere);
        }
    }

    /**
     * The line two cells share, drawn as the workbook draws it: the line the upper or the left cell has, when it
     * has one, as wide as the wider of the two.
     *
     * @param kept  the line of the upper or the left cell, or {@code null} when it has none
     * @param moved the line of the lower or the right cell
     */
    private static RawTableCellBorderSide shared(@Nullable RawTableCellBorderSide kept, RawTableCellBorderSide moved) {
        return Optional.ofNullable(kept)
                .map(line -> line.toBuilder().width(Math.max(widthOf(line), widthOf(moved))).build())
                .orElse(moved);
    }

    private static int widthOf(RawTableCellBorderSide side) {
        return Optional.ofNullable(side.width()).orElse(1);
    }

    /** Draws a line under a cell, as one with the line the cell has there. */
    private static UnaryOperator<RawTableCellBorder> withBottom(RawTableCellBorderSide line) {
        return sides -> sides.toBuilder().bottom(shared(sides.bottom(), line)).build();
    }

    /** The borders the theme draws a cell with, or {@code null} for a cell the theme does not reach. */
    private static @Nullable RawTableCellBorder borderOf(RawTableCell cell) {
        return isThemed(cell) ? cell.style().border() : null;
    }

    /** Whether the theme draws a cell: its style names the theme as its source. */
    private static boolean isThemed(RawTableCell cell) {
        return Optional.ofNullable(cell.style())
                .map(RawTableCellStyle::source)
                .filter(RawTableStyleSource.THEME::equals)
                .isPresent();
    }

    /** Whether there are cells at the places, and the theme reaches every one of them. */
    private boolean themed(Set<Place> places) {
        return !places.isEmpty()
                && places.stream().allMatch(place -> isThemed(matrix.get(place.row()).get(place.column())));
    }

    /** Changes the borders the theme draws one cell with, keeping the rest of the cell as it is. */
    private void change(Place place, UnaryOperator<RawTableCellBorder> sides) {
        var cell = matrix.get(place.row()).get(place.column());
        var style = cell.style();
        var border = sides.apply(Optional.ofNullable(style.border())
                .orElseGet(() -> RawTableCellBorder.builder().build()));
        var drawn = style.toBuilder().border(border.isEmpty() ? null : border).build();
        matrix.get(place.row()).set(place.column(), cell.toBuilder().style(drawn).build());
    }

    /**
     * The cells holding the places of a block of the table, each named once, or none at all when a place is held by
     * a cell the table read does not reach, such as a region that starts above the rows read.
     */
    private Set<Place> ownersOf(int row, int column, int rows, int columns) {
        var held = new LinkedHashSet<@Nullable Place>();
        for (var r = row; r < Math.min(row + rows, owners.length); r++) {
            for (var c = column; c < Math.min(column + columns, owners[r].length); c++) {
                held.add(owners[r][c]);
            }
        }
        return held.contains(null) ? Set.of() : Set.copyOf(held);
    }

    /** The owner of each place of a table. */
    private static Place[][] owners(List<List<RawTableCell>> matrix) {
        var holders = new Place[matrix.size()][];
        for (var row = 0; row < matrix.size(); row++) {
            holders[row] = new Place[matrix.get(row).size()];
        }
        for (var row = 0; row < matrix.size(); row++) {
            for (var column = 0; column < matrix.get(row).size(); column++) {
                hold(holders, matrix.get(row).get(column), row, column);
            }
        }
        return holders;
    }

    /** Names a cell as the holder of every place its region covers. */
    private static void hold(Place[][] owners, RawTableCell cell, int row, int column) {
        if (Boolean.TRUE.equals(cell.covered())) {
            return;
        }
        var holder = new Place(row, column);
        for (var r = row; r < Math.min(row + spanOf(cell.rowspan()), owners.length); r++) {
            for (var c = column; c < Math.min(column + spanOf(cell.colspan()), owners[r].length); c++) {
                owners[r][c] = holder;
            }
        }
    }

    private static int spanOf(@Nullable Integer span) {
        return Optional.ofNullable(span).orElse(1);
    }

    /** A place in the table, by its row and column. */
    private record Place(int row, int column) {
    }
}
