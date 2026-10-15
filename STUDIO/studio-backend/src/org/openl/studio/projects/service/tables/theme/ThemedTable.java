package org.openl.studio.projects.service.tables.theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.service.tables.theme.TableRegions.Region;

/**
 * The look a theme gives each cell of one table, drawn from the regions of the table.
 *
 * <p>Cells are named by their place on the sheet, not in the table. Every cell of a merged region takes the look, so
 * a border reaches the whole region. A cell the theme does not reach has no look and keeps its own.
 *
 * <p>The look of a region is worked out once, the first time one of its cells is asked for, so a read of a few rows of
 * a large table draws only the regions it reaches.
 */
public final class ThemedTable {

    /** How many rows of the table one bucket of the index of the regions spans. */
    private static final int BUCKET = 32;

    private final TableRegions regions;
    private final TableTheme.Look look;
    private final int top;
    private final int left;
    /** The regions reaching each bucket of rows of the table. */
    private final List<List<Region>> buckets;
    private final Map<Parts, ThemeStyle> styles = new HashMap<>();

    /**
     * The look a theme gives the cells of a table.
     *
     * @param regions the regions of the table
     * @param look    the look of the kind of the table in the theme
     * @param top     the row of the sheet the table starts at
     * @param left    the column of the sheet the table starts at
     */
    private ThemedTable(TableRegions regions, TableTheme.Look look, int top, int left) {
        this.regions = regions;
        this.look = look;
        this.top = top;
        this.left = left;
        var bottom = regions.theme().regions().stream().mapToInt(Region::bottom).max().orElse(-1);
        buckets = new ArrayList<>();
        for (var bucket = 0; bucket <= bottom / BUCKET; bucket++) {
            buckets.add(new ArrayList<>());
        }
        for (var region : regions.theme().regions()) {
            for (var bucket = region.top() / BUCKET; bucket <= region.bottom() / BUCKET; bucket++) {
                buckets.get(bucket).add(region);
            }
        }
    }

    /**
     * The look a theme gives the cells of a table laid out in regions.
     *
     * @param table   the table, which tells its kind
     * @param grid    the table as it stands on its grid, header included
     * @param regions the regions of the table, or {@code null} for a table of a kind no theme styles
     * @param theme   the theme
     * @return the look of the cells, or {@code null} for a table of a kind no theme styles
     */
    static @Nullable ThemedTable of(IOpenLTable table, IGridTable grid, @Nullable TableRegions regions,
                                    TableTheme theme) {
        var kind = ThemeKind.of(table);
        if (regions == null || kind == null) {
            return null;
        }
        var region = grid.getRegion();
        return new ThemedTable(regions, kind.lookIn(theme), region.getTop(), region.getLeft());
    }

    /**
     * The look of a cell, or {@code null} when the theme does not reach it.
     *
     * @param row    the row of the cell on the sheet
     * @param column the column of the cell on the sheet
     */
    public @Nullable ThemedCell at(int row, int column) {
        var inTable = row - top;
        var bucket = inTable < 0 ? -1 : inTable / BUCKET;
        if (bucket < 0 || bucket >= buckets.size()) {
            return null;
        }
        for (var region : buckets.get(bucket)) {
            if (region.holds(inTable, column - left)) {
                return cellOf(region, inTable, column - left);
            }
        }
        return null;
    }

    /**
     * Gives every cell the theme reaches to an action, with its look.
     *
     * @param action what to do with a cell, named by its place on the sheet, and its look
     */
    public void forEach(BiConsumer<Cell, ThemedCell> action) {
        for (var region : regions.theme().regions()) {
            for (var row = region.top(); row <= region.bottom(); row++) {
                for (var column = region.left(); column <= region.right(); column++) {
                    action.accept(new Cell(top + row, left + column), cellOf(region, row, column));
                }
            }
        }
    }

    /** The look of a cell of a region, named in table coordinates. */
    private ThemedCell cellOf(Region region, int row, int column) {
        var style = styles.computeIfAbsent(region.parts(), parts -> parts.lookIn(look));
        // Only the cell holding the header text formats it in pieces.
        return row == 0 && column == 0 ? new ThemedCell(style, runsOf(style)) : new ThemedCell(style, List.of());
    }

    /** The pieces of the header text, each in the look the theme gives its part. */
    private List<ThemedRun> runsOf(ThemeStyle cell) {
        var header = Optional.ofNullable(look.header()).orElseGet(() -> TableTheme.Header.builder().build());
        return regions.theme()
                .header()
                .stream()
                .map(piece -> new ThemedRun(piece.start(), piece.end(), piece.part().in(cell, header)))
                .toList();
    }

    /**
     * The place of a cell on the sheet.
     *
     * @param row    the row of the cell on the sheet
     * @param column the column of the cell on the sheet
     */
    public record Cell(int row, int column) {
    }

    /**
     * The look the theme gives one cell.
     *
     * @param style the look of the cell
     * @param runs  the pieces the theme formats the cell text in: the pieces of the header text for the cell holding
     *              it, and none for any other cell
     */
    public record ThemedCell(ThemeStyle style, List<ThemedRun> runs) {

        /**
         * Whether the text keeps the pieces the workbook formats it in. It does when the theme formats it in no
         * pieces of its own and names nothing of the font of the cell; otherwise the text is drawn and written in the
         * font of its cell.
         *
         * @return {@code true} when the pieces of the workbook stay
         */
        public boolean keepsOwnRuns() {
            return runs.isEmpty() && !style.hasFont();
        }
    }

    /**
     * A piece of a cell text and the look the theme gives it.
     *
     * @param start the index of the first character of the piece
     * @param end   the index after the last character of the piece
     * @param style the look of the piece: the cell look with the look of its part laid over it
     */
    public record ThemedRun(int start, int end, ThemeStyle style) {
    }
}
