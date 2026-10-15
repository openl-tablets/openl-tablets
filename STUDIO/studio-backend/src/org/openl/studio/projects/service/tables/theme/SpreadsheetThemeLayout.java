package org.openl.studio.projects.service.tables.theme;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.SpreadsheetHeaderDefinition;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.util.OpenClassUtils;

/**
 * Decides which look a theme gives each cell of the body of a Spreadsheet table.
 *
 * <p>The header and the properties are themed as every kind themes them, see {@link TableRegionLayout}. The first row
 * under them names the columns and gets the title look, and the title of the column of steps, such as {@code Step},
 * gets the step title look over it. The first column names the steps and gets the step look. Every other cell holds
 * the value of a step and gets the value look. Every cell starts from the look of the whole table, and the last row
 * gets the last-row look laid over its own.
 *
 * <p>A step whose name is merged across its row is a heading that splits the steps into sections: the compiler takes
 * it for a step with no value. It gets the section look over the step look. A step or a column the compiler read as
 * marked for the result of the Spreadsheet, its name ending with {@code *}, gets the marked look over its own.
 *
 * <p>The step a Spreadsheet returns is the one the compiler returns: the step named {@code RETURN}, or else the last
 * step. Its name gets the result look over its own, and every cell of its row the result row look: a line it draws
 * above or below goes round the step, however many rows of the sheet the step takes. A Spreadsheet returning
 * {@code SpreadsheetResult} returns every step, and its last step closes the calculation: it is themed as the result
 * too. A column named {@code RETURN}, which the compiler returns in place of a step, gets the result look over its
 * title. A Spreadsheet returning {@code void} returns nothing.
 *
 * <p>Every step and column is the one the compiler read.
 */
final class SpreadsheetThemeLayout {

    /** No row or column of the sheet. */
    private static final int NONE = -1;

    private SpreadsheetThemeLayout() {
    }

    /**
     * The places of the body of a Spreadsheet the compiler read: the steps down its first column and the columns across
     * its first row.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed layOut(ThemedBody body) {
        if (!(body.compiled().node().getMetaInfoReader() instanceof SpreadsheetMetaInfoReader reader)) {
            return BodyLayout.Placed.plain(body);
        }
        return new BodyLayout.Placed(body.rows(), Places.of(body, reader));
    }

    /**
     * The parts of the places of one body.
     *
     * @param stepsEnd      the last column of the sheet the column of steps takes
     * @param markedRows    the first rows of the sheet the steps marked for the result take
     * @param markedColumns the first columns of the sheet the columns marked for the result take
     * @param resultRows    the rows of the sheet the step the Spreadsheet returns takes, if any
     * @param resultColumn  the first column of the sheet the column the Spreadsheet returns takes, if any
     */
    @Builder
    private record Places(int stepsEnd,
                          Set<Integer> markedRows,
                          Set<Integer> markedColumns,
                          @Nullable Span resultRows,
                          int resultColumn) implements TableRegionLayout.PlaceLook {

        private static final Parts TITLE = Parts.BASE.with(Parts.Part.TITLES);
        private static final Parts STEP_TITLE = TITLE.with(Parts.Part.STEP_TITLE);
        private static final Parts STEP = Parts.BASE.with(Parts.Part.STEPS);
        private static final Parts SECTION = STEP.with(Parts.Part.SECTIONS);
        private static final Parts VALUE = Parts.BASE.with(Parts.Part.VALUES);

        /**
         * The places of a body, as the compiler read the Spreadsheet.
         *
         * <p>A Spreadsheet returning {@code void} returns nothing. Otherwise it returns the step or the column the
         * compiler took for its result, or every step, and then its last step closes the calculation.
         */
        static Places of(ThemedBody body, SpreadsheetMetaInfoReader reader) {
            var spreadsheet = reader.getBoundNode();
            var read = new Read(spreadsheet.getStructureBuilder());
            var returned = OpenClassUtils.isVoid(spreadsheet.getHeader().getType())
                    ? Optional.<SpreadsheetHeaderDefinition>empty()
                    : read.returned();
            return Places.builder()
                    .stepsEnd(body.rows().getCell(0, 0).getAbsoluteRegion().getRight())
                    .markedRows(read.marked(true))
                    .markedColumns(read.marked(false))
                    .resultRows(returned.filter(SpreadsheetHeaderDefinition::isRow).map(read::rowsOf).orElse(null))
                    .resultColumn(returned.filter(header -> !header.isRow()).map(read::columnOf).orElse(NONE))
                    .build();
        }

        /** The parts of a cell of the body, by the place it stands in. */
        @Override
        public Parts at(ICell cell, int column, int row) {
            if (row == 0) {
                if (column == 0) {
                    return STEP_TITLE;
                }
                var named = markedColumns.contains(cell.getAbsoluteColumn()) ? TITLE.with(Parts.Part.MARKED) : TITLE;
                return cell.getAbsoluteColumn() == resultColumn ? named.with(Parts.Part.RESULT) : named;
            }
            if (column > 0) {
                return resultRowIf(VALUE, cell);
            }
            // A step name merged over the values of its row heads a section.
            var name = cell.getAbsoluteRegion().getRight() > stepsEnd ? SECTION : STEP;
            name = markedRows.contains(cell.getAbsoluteRow()) ? name.with(Parts.Part.MARKED) : name;
            return resultRowIf(inResult(cell) ? name.with(Parts.Part.RESULT) : name, cell);
        }

        /** Whether a cell stands in the row of the step the Spreadsheet returns. */
        private boolean inResult(ICell cell) {
            var region = cell.getAbsoluteRegion();
            return resultRows != null && resultRows.from() <= region.getTop() && region.getBottom() <= resultRows.to();
        }

        /**
         * The parts of a cell with the result row laid over them in the row of the step the Spreadsheet returns, its
         * lines above and below on the edges of the step only.
         */
        private Parts resultRowIf(Parts parts, ICell cell) {
            if (resultRows == null || !inResult(cell)) {
                return parts;
            }
            var region = cell.getAbsoluteRegion();
            return parts.withAround(Parts.Part.RESULT_ROW, region.getTop() <= resultRows.from(),
                    region.getBottom() >= resultRows.to());
        }
    }

    /** The first and the last row of the sheet a step takes. */
    private record Span(int from, int to) {
    }

    /**
     * The steps and the columns of a Spreadsheet as the compiler read them.
     *
     * @param builder how the compiler read the body of the table
     */
    private record Read(SpreadsheetStructureBuilder builder) {

        /**
         * The header of the step or the column the Spreadsheet returns, or its last step for a Spreadsheet that returns
         * every step.
         */
        Optional<SpreadsheetHeaderDefinition> returned() {
            return Optional.ofNullable(builder.getReturnHeaderDefinition())
                    .or(() -> builder.getRowHeaders().keySet().stream()
                            .max(Integer::compareTo)
                            .map(builder.getRowHeaders()::get));
        }

        /**
         * The first rows of the sheet the steps marked for the result take, or the first columns of the columns marked
         * so.
         *
         * @param rows whether to tell the steps, rather than the columns
         */
        Set<Integer> marked(boolean rows) {
            var headers = rows ? builder.getRowHeaders() : builder.getColumnHeaders();
            return headers.values().stream()
                    .filter(header -> header.getDefinition().isAsteriskPresented())
                    .map(this::nameOf)
                    .map(name -> rows ? name.getTop() : name.getLeft())
                    .collect(Collectors.toUnmodifiableSet());
        }

        /** The rows of the sheet the name of a step takes. */
        Span rowsOf(SpreadsheetHeaderDefinition header) {
            var name = nameOf(header);
            return new Span(name.getTop(), name.getBottom());
        }

        /** The first column of the sheet the name of a column takes. */
        int columnOf(SpreadsheetHeaderDefinition header) {
            return nameOf(header).getLeft();
        }

        /**
         * Where the compiler read the name of a step or a column: in the first column of the body, under the titles of
         * the columns, or in its first row, after the column of steps.
         */
        private IGridRegion nameOf(SpreadsheetHeaderDefinition header) {
            var body = builder.getTableBody();
            var cell = header.isRow() ? body.getCell(0, header.getRow() + 1) : body.getCell(header.getColumn() + 1, 0);
            return cell.getAbsoluteRegion();
        }
    }
}
