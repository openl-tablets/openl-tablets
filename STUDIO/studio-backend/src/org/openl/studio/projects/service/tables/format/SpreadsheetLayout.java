package org.openl.studio.projects.service.tables.format;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.SpreadsheetHeaderDefinition;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.util.OpenClassUtils;

/**
 * Decides what each cell of the body of a Spreadsheet table is: its role.
 *
 * <p>The header and the properties are laid out as every kind lays them out, see {@link TableRegionLayout}. The first
 * row under them names the columns: their titles are titles of what the table gives, and the title of the column of
 * steps, such as {@code Step}, a title of what it takes. The first column names the steps, the keys of the table.
 * Every other cell holds the value of a step, which the table gives.
 *
 * <p>A step whose name is merged across its row is a heading that splits the steps into sections: the compiler takes
 * it for a step with no value.
 *
 * <p>What a Spreadsheet returns is what the compiler returns:
 * <ul>
 *     <li>A single value: the step named {@code RETURN}, or the last step when none is so named, its name and its
 *     cells; or a column named {@code RETURN}, its title and its cells, in place of the step.</li>
 *     <li>A {@code SpreadsheetResult} with steps or columns marked {@code *}: the marked steps × the marked columns.
 *     When only steps are marked, every column the result keeps (not marked {@code ~}) is returned, and the other way
 *     round. The names of the marked steps and the titles of the marked columns are returned too.</li>
 *     <li>A {@code SpreadsheetResult} with nothing marked {@code *} returns every step not marked {@code ~}, nearly
 *     the whole table, and a Spreadsheet returning {@code void} nothing: no cell is the result.</li>
 * </ul>
 *
 * <p>Every step and column is the one the compiler read.
 */
final class SpreadsheetLayout {

    private SpreadsheetLayout() {
    }

    /**
     * The places of the body of a Spreadsheet the compiler read: the steps down its first column and the columns across
     * its first row.
     *
     * @param body the body of the table
     * @return the body and what each of its places is
     */
    static BodyLayout.Placed layOut(TableBody body) {
        if (!(body.compiled().node().getMetaInfoReader() instanceof SpreadsheetMetaInfoReader reader)) {
            return BodyLayout.Placed.plain(body);
        }
        return new BodyLayout.Placed(body.rows(), Places.of(body, reader));
    }

    /**
     * What the places of one body are.
     *
     * @param stepsEnd the last column of the sheet the column of steps takes
     * @param returned what the Spreadsheet returns
     */
    private record Places(int stepsEnd, Returned returned) implements TableRegionLayout.PlaceRoles {

        private static final Place TITLE = Place.of(RawTableRole.OUTPUT_TITLE);
        private static final Place STEP_TITLE = Place.of(RawTableRole.INPUT_TITLE);
        private static final Place STEP = Place.of(RawTableRole.KEY);
        private static final Place SECTION = Place.of(RawTableRole.SECTION);
        private static final Place VALUE = Place.of(RawTableRole.OUTPUT);

        /**
         * The places of a body, as the compiler read the Spreadsheet.
         */
        static Places of(TableBody body, SpreadsheetMetaInfoReader reader) {
            var spreadsheet = reader.getBoundNode();
            var read = new Read(spreadsheet.getStructureBuilder());
            var isVoid = OpenClassUtils.isVoid(spreadsheet.getHeader().getType());
            return new Places(body.rows().getCell(0, 0).getAbsoluteRegion().getRight(),
                    isVoid ? Returned.NOTHING : read.returnedCells());
        }

        /** What a cell of the body is, by the place it stands in. */
        @Override
        public Place at(ICell cell, int column, int row) {
            if (row == 0) {
                if (column == 0) {
                    return STEP_TITLE;
                }
                return returned.titles(cell) ? TITLE.as(RawTableRole.RESULT) : TITLE;
            }
            if (column > 0) {
                return returned.holds(cell) ? VALUE.as(RawTableRole.RESULT) : VALUE;
            }
            // A step name merged over the values of its row heads a section.
            var section = cell.getAbsoluteRegion().getRight() > stepsEnd;
            if (section) {
                return SECTION;
            }
            return returned.names(cell) ? STEP.as(RawTableRole.RESULT) : STEP;
        }

    }

    /**
     * What a Spreadsheet returns: the cells of its rows and its columns, and the names and the titles returned with
     * them, by the rows and the columns of the sheet.
     *
     * @param rows    the rows of the sheet of the steps returned, or {@code null} for every step
     * @param columns the columns of the sheet of the columns returned, or {@code null} for every column
     * @param names   the rows of the sheet of the steps whose names are returned
     * @param titles  the columns of the sheet of the columns whose titles are returned
     */
    private record Returned(@Nullable Set<Integer> rows, @Nullable Set<Integer> columns, Set<Integer> names,
                            Set<Integer> titles) {

        /** Nothing returned. */
        static final Returned NOTHING = new Returned(Set.of(), Set.of(), Set.of(), Set.of());

        /** Whether a cell of the values of the steps is returned. */
        boolean holds(ICell cell) {
            return (rows == null || rows.contains(cell.getAbsoluteRow()))
                    && (columns == null || columns.contains(cell.getAbsoluteColumn()));
        }

        /** Whether the name of a step is returned. */
        boolean names(ICell cell) {
            return names.contains(cell.getAbsoluteRow());
        }

        /** Whether the title of a column is returned. */
        boolean titles(ICell cell) {
            return titles.contains(cell.getAbsoluteColumn());
        }
    }

    /**
     * The steps and the columns of a Spreadsheet as the compiler read them.
     *
     * @param builder how the compiler read the body of the table
     */
    private record Read(SpreadsheetStructureBuilder builder) {

        /**
         * What the Spreadsheet returns: the step or the column named {@code RETURN} or the last step, for a
         * Spreadsheet returning a single value, or else the steps and the columns of its result marked {@code *}.
         */
        Returned returnedCells() {
            var single = builder.getReturnHeaderDefinition();
            if (single != null) {
                var cells = sheetLines(single);
                return single.isRow()
                        ? new Returned(cells, null, cells, Set.of())
                        : new Returned(null, cells, Set.of(), cells);
            }
            var steps = builder.getRowHeaders().values();
            var columns = builder.getColumnHeaders().values();
            var markedSteps = linesOf(steps, header -> header.getDefinition().isAsteriskPresented());
            var markedColumns = linesOf(columns, header -> header.getDefinition().isAsteriskPresented());
            if (markedSteps.isEmpty() && markedColumns.isEmpty()) {
                return Returned.NOTHING;
            }
            return new Returned(
                    markedSteps.isEmpty() ? linesOf(steps, header -> !header.getDefinition().isTildePresented())
                            : markedSteps,
                    markedColumns.isEmpty() ? linesOf(columns, header -> !header.getDefinition().isTildePresented())
                            : markedColumns,
                    markedSteps,
                    markedColumns);
        }

        /** The rows or the columns of the sheet the steps or the columns a filter keeps take. */
        private Set<Integer> linesOf(Collection<SpreadsheetHeaderDefinition> headers,
                                     Predicate<SpreadsheetHeaderDefinition> kept) {
            return headers.stream()
                    .filter(Objects::nonNull)
                    .filter(kept)
                    .flatMap(header -> sheetLines(header).stream())
                    .collect(Collectors.toUnmodifiableSet());
        }

        /** The rows of the sheet the name of a step takes, or the columns the title of a column takes. */
        private Set<Integer> sheetLines(SpreadsheetHeaderDefinition header) {
            var name = nameOf(header);
            var from = header.isRow() ? name.getTop() : name.getLeft();
            var to = header.isRow() ? name.getBottom() : name.getRight();
            return IntStream.rangeClosed(from, to).boxed().collect(Collectors.toUnmodifiableSet());
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
