package org.openl.studio.projects.service.tables.theme;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetSymbols;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;

/**
 * Decides which look a theme gives each cell of the body of a Spreadsheet table.
 *
 * <p>The header and the properties are themed as every kind themes them, see {@link ThemeLayouts}. The first row
 * under them names the columns and gets the title look, and the title of the column of steps, such as {@code Step},
 * gets the step title look over it. The first column names the steps and gets the step look. Every other cell holds
 * the value of a step and gets the value look. Every cell starts from the look of the whole table, and the last row
 * gets the last-row look laid over its own.
 *
 * <p>A step whose name is merged across its row is a heading that splits the steps into sections: the compiler takes
 * it for a step with no value. It gets the section look over the step look. A step or a column whose name ends with
 * {@code *} is marked for the result of the Spreadsheet, and gets the marked look over its own.
 *
 * <p>A Spreadsheet that returns a type other than {@code SpreadsheetResult} returns the value of one step: the step
 * named {@code RETURN}, or else the last step. That step gets the result look over its own, and so does a column named
 * {@code RETURN}, which the compiler takes in its place. The steps are told as the compiler tells them from the text of
 * the table, so a table being edited is themed before it is compiled.
 */
final class SpreadsheetThemeLayout {

    /** The name of the step or the column whose value a Spreadsheet returns, as the compiler names it. */
    private static final String RETURN = "RETURN";

    /** What starts the name of a row or a column that describes the others rather than names a step. */
    private static final String DESCRIPTION = "//";

    /** The type a Spreadsheet returns no value as. */
    private static final String VOID = "void";

    /** No row or column of the body. */
    private static final int NONE = -1;

    private SpreadsheetThemeLayout() {
    }

    /**
     * The places of the body of a Spreadsheet, read as it is written. The header names the type the Spreadsheet
     * returns.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
     */
    static BodyLayout.Placed layOut(ThemedBody body) {
        return new BodyLayout.Placed(body.rows(), Places.of(body));
    }

    /**
     * The looks of the places of one body, each laid over the base once rather than once per cell.
     *
     * @param title        the look of a column title
     * @param stepTitle    the look of the title of the column of steps
     * @param step         the look of a step name
     * @param section      the look of a step heading a section
     * @param value        the look of the value of a step
     * @param marked       the look laid over a name marked for the result
     * @param result       the look laid over the name of the step whose value the Spreadsheet returns
     * @param stepsEnd     the last column of the sheet the column of steps takes
     * @param resultRow    the row of the body holding the step whose value the Spreadsheet returns, if any
     * @param resultColumn the column of the body named {@code RETURN}, if any
     */
    @Builder
    private record Places(ThemeStyle title,
                          ThemeStyle stepTitle,
                          ThemeStyle step,
                          ThemeStyle section,
                          ThemeStyle value,
                          @Nullable ThemeStyle marked,
                          @Nullable ThemeStyle result,
                          int stepsEnd,
                          int resultRow,
                          int resultColumn) implements ThemeLayouts.PlaceLook {

        /**
         * The places of a body.
         *
         * <p>A Spreadsheet returning {@code void} returns nothing. A column named {@code RETURN} is returned in place
         * of any step; otherwise the step the compiler returns is found among the rows.
         */
        static Places of(ThemedBody body) {
            var base = body.base();
            var look = body.look();
            var rows = body.rows();
            var type = HeaderRuns.returnType(body.header());
            var returnColumn = VOID.equals(type) ? NONE : returnColumnOf(rows);
            return Places.builder()
                    .title(base.with(look.titles()))
                    .stepTitle(base.with(look.titles()).with(look.stepTitle()))
                    .step(base.with(look.steps()))
                    .section(base.with(look.steps()).with(look.sections()))
                    .value(base.with(look.values()))
                    .marked(look.marked())
                    .result(look.result())
                    .stepsEnd(rows.getCell(0, 0).getAbsoluteRegion().getRight())
                    .resultRow(VOID.equals(type) || returnColumn != NONE ? NONE : resultRowOf(rows, type))
                    .resultColumn(returnColumn)
                    .build();
        }

        /** The look of a cell of the body, by the place it stands in. */
        @Override
        public ThemeStyle at(ICell cell, int column, int row) {
            if (row == 0) {
                return column == 0 ? stepTitle : resultIf(markedIf(title, cell), column == resultColumn);
            }
            if (column > 0) {
                return value;
            }
            // A step name merged over the values of its row heads a section.
            var name = cell.getAbsoluteRegion().getRight() > stepsEnd ? section : step;
            return resultIf(markedIf(name, cell), row == resultRow);
        }

        private ThemeStyle markedIf(ThemeStyle style, ICell cell) {
            return isMarked(cell.getStringValue()) ? style.with(marked) : style;
        }

        private ThemeStyle resultIf(ThemeStyle style, boolean returned) {
            return returned ? style.with(result) : style;
        }
    }

    /**
     * The row of the step whose value a Spreadsheet returns, as the compiler finds it, for a Spreadsheet that returns a
     * value and names no column {@code RETURN}.
     *
     * <p>The step named {@code RETURN} is returned, or else the last step, unless the Spreadsheet returns
     * {@code SpreadsheetResult}: then it returns the whole table. A row whose name is empty or describes the others is
     * not a step.
     *
     * @param type the type the Spreadsheet returns
     * @return the row of the body, or {@link #NONE} when the Spreadsheet returns no single step
     */
    private static int resultRowOf(ILogicalTable body, String type) {
        var last = NONE;
        for (var row = 1; row < body.getHeight(); row++) {
            var text = body.getCell(0, row).getStringValue();
            if (RETURN.equals(nameOf(text))) {
                return row;
            }
            last = isStep(text) ? row : last;
        }
        return returnsWholeTable(type) ? NONE : last;
    }

    /** Whether the type is the one a Spreadsheet returns every step of it as, by its simple name or its full one. */
    private static boolean returnsWholeTable(String type) {
        return SpreadsheetResult.class.getSimpleName().equals(type) || SpreadsheetResult.class.getName().equals(type);
    }

    /** The column of the body named {@code RETURN}, or {@link #NONE} for a body that has none. */
    private static int returnColumnOf(ILogicalTable body) {
        for (var column = 1; column < body.getWidth(); column++) {
            if (RETURN.equals(nameOf(body.getCell(column, 0).getStringValue()))) {
                return column;
            }
        }
        return NONE;
    }

    /** Whether the name of a row is the name of a step: it is not empty and does not describe the others. */
    private static boolean isStep(@Nullable String text) {
        return text != null && !text.isBlank() && !text.strip().startsWith(DESCRIPTION);
    }

    /**
     * Whether the name of a step or a column is marked for the result of the Spreadsheet, as the compiler reads it:
     * the name before the type it declares ends with {@code *}.
     */
    private static boolean isMarked(@Nullable String header) {
        return header != null && declaredName(header).endsWith(SpreadsheetSymbols.ASTERISK.toString());
    }

    /**
     * The name a step or a column is known by, as the compiler reads it: without the type it declares and without the
     * mark that puts it into the result or leaves it out.
     */
    private static @Nullable String nameOf(@Nullable String header) {
        if (header == null) {
            return null;
        }
        var name = declaredName(header);
        var marked = name.endsWith(SpreadsheetSymbols.ASTERISK.toString())
                || name.endsWith(SpreadsheetSymbols.TILDE.toString());
        return marked ? name.substring(0, name.length() - 1).strip() : name;
    }

    /** The name a header declares before the type it declares, the mark kept. */
    private static String declaredName(String header) {
        var delimiter = header.indexOf(SpreadsheetSymbols.TYPE_DELIMITER.toString());
        return (delimiter < 0 ? header : header.substring(0, delimiter)).strip();
    }
}
