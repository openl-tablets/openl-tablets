package org.openl.studio.projects.service.tables;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellMetaInfo;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.service.tables.read.RawTableCells;

/**
 * A raw read of a table, each cell with the style and the pieces of text the styles of the read give it, so a test
 * reads a style as the screen draws it rather than by its index.
 *
 * @author Yury Molchan
 */
public final class StyledRead {

    /** The cells, row by row. */
    public final List<List<StyledCell>> source;

    /** A read of a table, its styles taken from the dictionary of the read. */
    public StyledRead(RawTableView view) {
        this(view.source, view.styles);
    }

    /** The cells of a table read as a matrix, their styles taken from the dictionary of the read. */
    public StyledRead(RawTableCells cells) {
        this(cells.source(), cells.styles());
    }

    private StyledRead(List<List<RawTableCell>> cells, @Nullable List<RawTableCellStyle> styles) {
        var rows = new ArrayList<List<StyledCell>>(cells.size());
        for (var row : cells) {
            rows.add(row.stream().map(cell -> StyledCell.of(cell, styles)).toList());
        }
        source = rows;
    }

    /**
     * A cell of the read with its style.
     *
     * @param raw   the cell as the read reports it
     * @param style the style the cell names, or {@code null} for none
     * @param runs  the pieces of its text with their styles, or {@code null} when the text takes the font of the cell
     */
    public record StyledCell(RawTableCell raw, @Nullable RawTableCellStyle style, @Nullable List<StyledRun> runs) {

        static StyledCell of(RawTableCell cell, @Nullable List<RawTableCellStyle> styles) {
            return new StyledCell(cell, styleAt(styles, cell.style()), runsOf(cell, styles));
        }

        public @Nullable Object value() {
            return raw.value();
        }

        public @Nullable String cell() {
            return raw.cell();
        }

        public @Nullable String formula() {
            return raw.formula();
        }

        public @Nullable String comment() {
            return raw.comment();
        }

        public @Nullable Integer colspan() {
            return raw.colspan();
        }

        public @Nullable Integer rowspan() {
            return raw.rowspan();
        }

        public @Nullable Boolean covered() {
            return raw.covered();
        }

        public @Nullable RawTableCellMetaInfo metaInfo() {
            return raw.metaInfo();
        }

        /** The pieces of the text of a cell, each with the text it covers and its style. */
        private static @Nullable List<StyledRun> runsOf(RawTableCell cell, @Nullable List<RawTableCellStyle> styles) {
            if (cell.runs() == null) {
                return null;
            }
            var text = String.valueOf(cell.value());
            var runs = new ArrayList<StyledRun>();
            var from = 0;
            for (var run : cell.runs()) {
                runs.add(new StyledRun(text.substring(from, run.to()), styleAt(styles, run.style())));
                from = run.to();
            }
            return runs;
        }

        private static @Nullable RawTableCellStyle styleAt(@Nullable List<RawTableCellStyle> styles,
                                                          @Nullable Integer index) {
            return index == null || styles == null ? null : styles.get(index);
        }
    }

    /**
     * A piece of the text of a cell with its style.
     *
     * @param text  the text of the piece
     * @param style the font of the piece, or {@code null} when it takes the font of the cell
     */
    public record StyledRun(String text, @Nullable RawTableCellStyle style) {
    }
}
