package org.openl.rules.table;

import java.util.stream.IntStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * How the cells of a sheet move when lines are put into a table, taken out of it, or the whole table moves.
 *
 * <p>A reference to a cell that moves follows it. A reference to a cell that stays where it is keeps its address,
 * whether the cell lies beside the table, above or below it, or in the table before the place. A reference to a cell
 * that is taken away is lost.
 */
@NullMarked
public sealed interface CellMove {

    /**
     * Where an area of cells goes.
     *
     * @param area the area a reference names, a single cell being an area of one cell
     * @return the area the reference names after the move, the same object when nothing of it moves, or {@code null}
     * when all its cells are taken away
     */
    @Nullable
    IGridRegion moved(IGridRegion area);

    /**
     * The direction lines run in: a row spans the columns, and rows follow one another down the sheet.
     */
    enum Axis {
        ROWS {
            @Override
            public int first(IGridRegion region) {
                return region.getTop();
            }

            @Override
            public int last(IGridRegion region) {
                return region.getBottom();
            }

            @Override
            public IGridRegion span(IGridRegion region, int first, int last) {
                return new GridRegion(first, region.getLeft(), last, region.getRight());
            }

            @Override
            public Axis across() {
                return COLUMNS;
            }
        },
        COLUMNS {
            @Override
            public int first(IGridRegion region) {
                return region.getLeft();
            }

            @Override
            public int last(IGridRegion region) {
                return region.getRight();
            }

            @Override
            public IGridRegion span(IGridRegion region, int first, int last) {
                return new GridRegion(region.getTop(), first, region.getBottom(), last);
            }

            @Override
            public Axis across() {
                return ROWS;
            }
        };

        /** The first line of the region. */
        public abstract int first(IGridRegion region);

        /** The last line of the region. */
        public abstract int last(IGridRegion region);

        /** The same region over other lines, its extent across them kept. */
        public abstract IGridRegion span(IGridRegion region, int first, int last);

        /** The other direction. */
        public abstract Axis across();
    }

    /**
     * The lines of a table from a line to its end move along the table by a number of lines, as Excel shifts cells.
     *
     * <p>A positive offset makes room for new lines in front of the first line moved. A negative one takes away the
     * lines it moves over, the last line before the first moved included.
     *
     * <p>A range that runs across the lines of the table follows its cells and grows over the new lines or shrinks by
     * the lines taken away, as Excel does. A range reaching past the table across the lines, such as a whole column
     * when columns move, keeps its address.
     *
     * @param table  the region of the table
     * @param axis   the lines that move: rows or columns
     * @param first  the first line that moves, as an index of the sheet
     * @param offset how many lines the cells move further along the table, negative to move them back
     */
    record Shift(IGridRegion table, Axis axis, int first, int offset) implements CellMove {

        @Override
        public @Nullable IGridRegion moved(IGridRegion area) {
            var across = axis.across();
            if (across.first(area) < across.first(table) || across.last(area) > across.last(table)) {
                return area;
            }
            var from = axis.first(area);
            var to = axis.last(area);
            var end = axis.last(table);
            // The lines moved go by the offset, the others stay: the farthest ends of the lines kept lie at the ends
            // of the area or next to the ends of the lines moved or taken away.
            var lines = IntStream.of(from, to, first - 1, first, end, end + 1, first + offset - 1)
                    .filter(line -> line >= from && line <= to)
                    .map(this::line)
                    .filter(line -> line >= 0)
                    .summaryStatistics();
            if (lines.getCount() == 0) {
                return null;
            }
            if (lines.getMin() == from && lines.getMax() == to) {
                return area;
            }
            return axis.span(area, lines.getMin(), lines.getMax());
        }

        /** Where a line of the area goes: its index after the move, or {@code -1} when it is taken away. */
        private int line(int line) {
            if (line >= first && line <= axis.last(table)) {
                return line + offset;
            }
            return line >= first + offset && line < first ? -1 : line;
        }
    }

    /**
     * A whole table moves to another place of its sheet, as Excel moves cells cut and pasted.
     *
     * <p>A range moves when it lies in the table. A range reaching out of the table keeps its address.
     *
     * @param from the region of the table before the move
     * @param to   the region of the table after the move, of the same size
     */
    record Transfer(IGridRegion from, IGridRegion to) implements CellMove {

        @Override
        public IGridRegion moved(IGridRegion area) {
            if (!GridRegionUtils.contains(from, area.getLeft(), area.getTop())
                    || !GridRegionUtils.contains(from, area.getRight(), area.getBottom())) {
                return area;
            }
            return GridRegionUtils.move(area, to.getLeft() - from.getLeft(), to.getTop() - from.getTop());
        }
    }
}
