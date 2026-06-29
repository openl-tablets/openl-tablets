package org.openl.rules.webstudio.web.trace.debug;

import org.jspecify.annotations.Nullable;

/**
 * The current line being evaluated inside a table frame.
 *
 * <p>For a spreadsheet this is a cell with its row and column; for a decision table a condition or a
 * fired rule; for a TBasic algorithm an operation. Coordinates that do not apply are {@code -1}.
 *
 * @param kind   short location type ({@code cell}, {@code dtrule}, {@code operation})
 * @param row    cell row index, or {@code -1}
 * @param column cell column index, or {@code -1}
 * @param ref    short cell reference such as {@code R2C3}, or {@code null}
 * @param label  human-readable description, or {@code null}
 */
public record CurrentLocation(String kind, int row, int column, @Nullable String ref, @Nullable String label) {

    private static final String CELL = "cell";
    private static final String DT_RULE = "dtrule";
    private static final String OPERATION = "operation";

    /** Breakpoint key suffix that suspends when a decision-table rule fires (all of its conditions matched). */
    public static final String RULE_FIRED_REF = "rule";

    /** A spreadsheet cell location. */
    public static CurrentLocation cell(int row, int column) {
        return cell(row, column, null);
    }

    /** A spreadsheet cell location with a step name. */
    public static CurrentLocation cell(int row, int column, @Nullable String label) {
        return new CurrentLocation(CELL, row, column, cellRef(row, column), label);
    }

    /** The short reference of a spreadsheet cell, such as {@code R2C3}. This is the breakpoint key suffix. */
    public static String cellRef(int row, int column) {
        return "R" + row + "C" + column;
    }

    /** A decision-table fired-rule location. */
    public static CurrentLocation dtRule(@Nullable String label) {
        return new CurrentLocation(DT_RULE, -1, -1, null, label);
    }

    /** A TBasic operation location. */
    public static CurrentLocation operation(@Nullable String label) {
        return new CurrentLocation(OPERATION, -1, -1, null, label);
    }

    /**
     * The breakpoint key suffix for this location, or {@code null} if it cannot carry a breakpoint.
     *
     * <p>A spreadsheet cell uses its cell reference. A fired decision-table rule uses
     * {@link #RULE_FIRED_REF}, so one breakpoint suspends on any rule firing. The displayed {@link #ref}
     * is left untouched.
     */
    public @Nullable String breakpointRef() {
        return DT_RULE.equals(kind) ? RULE_FIRED_REF : ref;
    }
}
