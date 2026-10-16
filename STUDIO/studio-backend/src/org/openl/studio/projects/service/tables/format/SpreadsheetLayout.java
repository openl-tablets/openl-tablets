package org.openl.studio.projects.service.tables.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.SpreadsheetHeaderDefinition;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.calc.SpreadsheetSymbols;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.util.OpenClassUtils;

/**
 * Decides what each cell of the body of a Spreadsheet table is: its role.
 *
 * <p>The header and the properties are laid out as every kind lays them out, see {@link TableRegionLayout}. The first
 * row under them names the columns: their titles are titles of what the table gives, and the title of the column of
 * steps, such as {@code Step}, a title of what it takes. The first column names the steps: values the table holds,
 * names rather than values to type. Every other cell holds the value of a step, which the table gives.
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
 * <p>Every step and column is the one the compiler read. A Spreadsheet the compiler read none of is read from its
 * text the same way: the names of its steps down its first column, the titles of its columns across its first row,
 * and what it returns by the type its header names and by the marks of the names and the titles.
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
     * The places of the body of a Spreadsheet the compiler read none of, as its text names its steps and columns.
     *
     * @param body the body of the table
     * @return the body and what each of its places is
     */
    static BodyLayout.Placed guessed(TableBody body) {
        return new BodyLayout.Placed(body.rows(), Places.guessed(body));
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
        private static final Place STEP = Place.of(RawTableRole.VALUE).untyped();
        private static final Place SECTION = Place.of(RawTableRole.SECTION);
        private static final Place VALUE = Place.of(RawTableRole.OUTPUT);

        /**
         * The places of a body, as the compiler read the Spreadsheet.
         */
        static Places of(TableBody body, SpreadsheetMetaInfoReader reader) {
            var spreadsheet = reader.getBoundNode();
            var read = new Read(spreadsheet.getStructureBuilder());
            var isVoid = OpenClassUtils.isVoid(spreadsheet.getHeader().getType());
            return new Places(stepsEndOf(body), isVoid ? Returned.NOTHING : read.returnedCells());
        }

        /** The places of a body the compiler read none of, as its text tells them. */
        static Places guessed(TableBody body) {
            var type = GuessedReads.typeOf(GuessedReads.headerOf(body.compiled().node()));
            return new Places(stepsEndOf(body), new Guess(body.rows()).returnedCells(type));
        }

        /** The last column of the sheet the column of steps takes. */
        private static int stepsEndOf(TableBody body) {
            return body.rows().getCell(0, 0).getAbsoluteRegion().getRight();
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

        /**
         * A single step returned, or a single column.
         *
         * @param step  whether a step is returned, rather than a column
         * @param lines the rows of the sheet the step takes, or the columns the column takes
         */
        static Returned single(boolean step, Set<Integer> lines) {
            return step ? new Returned(lines, null, lines, Set.of()) : new Returned(null, lines, Set.of(), lines);
        }

        /**
         * What a {@code SpreadsheetResult} returns: the steps and the columns marked {@code *}, or with only one of
         * them marked, every one of the other the result keeps, not marked {@code ~}. With nothing marked, nothing is
         * the result.
         */
        static Returned marked(List<Line> steps, List<Line> columns) {
            var markedSteps = linesOf(steps, Line::marked);
            var markedColumns = linesOf(columns, Line::marked);
            if (markedSteps.isEmpty() && markedColumns.isEmpty()) {
                return NOTHING;
            }
            return new Returned(
                    markedSteps.isEmpty() ? linesOf(steps, line -> !line.dropped()) : markedSteps,
                    markedColumns.isEmpty() ? linesOf(columns, line -> !line.dropped()) : markedColumns,
                    markedSteps,
                    markedColumns);
        }

        /** The rows or the columns of the sheet the steps or the columns a filter keeps take. */
        private static Set<Integer> linesOf(List<Line> lines, Predicate<Line> kept) {
            return lines.stream()
                    .filter(kept)
                    .flatMap(line -> line.lines().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * A step or a column of a Spreadsheet, and the marks of its name or title.
     *
     * @param marked  whether its name ends with {@code *}: a {@code SpreadsheetResult} returns it
     * @param dropped whether its name ends with {@code ~}: a {@code SpreadsheetResult} keeps it out
     * @param lines   the rows of the sheet a step takes, or the columns a column takes
     */
    private record Line(boolean marked, boolean dropped, Set<Integer> lines) {

        /** The rows or the columns of the sheet a region takes along a step or a column. */
        static Set<Integer> linesOf(boolean step, IGridRegion region) {
            var from = step ? region.getTop() : region.getLeft();
            var to = step ? region.getBottom() : region.getRight();
            return IntStream.rangeClosed(from, to).boxed().collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * The steps and the columns of a Spreadsheet the compiler read none of, as its text names them: as the compiler
     * reads a name, the part before {@code :} names the step, and a {@code *} or a {@code ~} closing it marks it.
     *
     * @param body the body of the table
     */
    private record Guess(ILogicalTable body) {

        /** The name the compiler returns the step or the column of, whatever the type of the Spreadsheet. */
        private static final String RETURN = "RETURN";

        private static final String MARKED = SpreadsheetSymbols.ASTERISK.toString();
        private static final String DROPPED = SpreadsheetSymbols.TILDE.toString();

        /**
         * What the Spreadsheet returns, as the compiler tells it from the type its header names: nothing for
         * {@code void}; the step or the column named {@code RETURN}; for a {@code SpreadsheetResult}, the marked steps
         * and columns; or else the last step.
         */
        Returned returnedCells(String type) {
            if ("void".equals(type)) {
                return Returned.NOTHING;
            }
            var steps = named(true);
            var columns = named(false);
            var step = returnOf(steps);
            if (step != null) {
                return Returned.single(true, step.lines());
            }
            var column = returnOf(columns);
            if (column != null) {
                return Returned.single(false, column.lines());
            }
            if ("SpreadsheetResult".equals(type)) {
                return Returned.marked(linesOf(steps), linesOf(columns));
            }
            return steps.isEmpty() ? Returned.NOTHING : Returned.single(true, steps.getLast().line().lines());
        }

        /** The step or the column named {@code RETURN}, or {@code null} for none. */
        private static @Nullable Line returnOf(List<Named> named) {
            return named.stream().filter(line -> RETURN.equals(line.name())).map(Named::line).findFirst().orElse(null);
        }

        private static List<Line> linesOf(List<Named> named) {
            return named.stream().map(Named::line).toList();
        }

        /** The steps down the first column, or the columns across the first row, that have a name. */
        private List<Named> named(boolean step) {
            var named = new ArrayList<Named>();
            var count = step ? body.getHeight() : body.getWidth();
            for (var at = 1; at < count; at++) {
                var cell = step ? body.getCell(0, at) : body.getCell(at, 0);
                var name = nameOf(cell.getStringValue());
                if (!name.isEmpty()) {
                    var marked = name.endsWith(MARKED);
                    var dropped = name.endsWith(DROPPED);
                    var unmarked = marked || dropped ? name.substring(0, name.length() - 1).trim() : name;
                    named.add(new Named(unmarked,
                            new Line(marked, dropped, Line.linesOf(step, cell.getAbsoluteRegion()))));
                }
            }
            return named;
        }

        /** The name of a step or a column, its type left out. */
        private static String nameOf(@Nullable String text) {
            var name = Objects.requireNonNullElse(text, "");
            var type = name.indexOf(SpreadsheetSymbols.TYPE_DELIMITER.toString());
            return (type < 0 ? name : name.substring(0, type)).trim();
        }

        /**
         * A step or a column with its name.
         *
         * @param name the name, without the mark that closes it
         * @param line the step or the column
         */
        private record Named(String name, Line line) {
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
                return Returned.single(single.isRow(), sheetLines(single));
            }
            return Returned.marked(linesOf(builder.getRowHeaders().values()),
                    linesOf(builder.getColumnHeaders().values()));
        }

        /** The steps or the columns the compiler read, and their marks. */
        private List<Line> linesOf(Collection<SpreadsheetHeaderDefinition> headers) {
            return headers.stream()
                    .filter(Objects::nonNull)
                    .map(header -> new Line(header.getDefinition().isAsteriskPresented(),
                            header.getDefinition().isTildePresented(), sheetLines(header)))
                    .toList();
        }

        /** The rows of the sheet the name of a step takes, or the columns the title of a column takes. */
        private Set<Integer> sheetLines(SpreadsheetHeaderDefinition header) {
            return Line.linesOf(header.isRow(), nameOf(header));
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
