package org.openl.rules.webstudio.web.trace.debug;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The current line being evaluated inside a table frame.
 *
 * <p>For a spreadsheet this is a cell with its row and column; for a decision table a condition or a
 * fired rule; for a TBasic algorithm an operation. Coordinates that do not apply are {@code -1}.
 *
 * @param kind           short location type ({@code cell}, {@code dtrule}, {@code operation})
 * @param row            cell row index, or {@code -1}
 * @param column         cell column index, or {@code -1}
 * @param ref            short cell reference such as {@code R2C3}, or {@code null}
 * @param label          human-readable description, or {@code null}
 * @param breakpointRefs breakpoint key suffixes this location can match (a cell reference, or the
 *                       rule-fired marker plus each fired rule name); empty when it cannot carry one
 */
public record CurrentLocation(String kind, int row, int column, @Nullable String ref, @Nullable String label,
                              List<String> breakpointRefs) {

    private static final String CELL = "cell";
    private static final String DT_RULE = "dtrule";
    private static final String OPERATION = "operation";

    /** Breakpoint key suffix that suspends when any decision-table rule fires (all of its conditions matched). */
    public static final String RULE_FIRED_REF = "rule";

    public CurrentLocation {
        breakpointRefs = List.copyOf(breakpointRefs);
    }

    /** A spreadsheet cell location. */
    public static CurrentLocation cell(int row, int column) {
        return cell(row, column, null);
    }

    /** A spreadsheet cell location with a step name. */
    public static CurrentLocation cell(int row, int column, @Nullable String label) {
        String ref = cellRef(row, column);
        return new CurrentLocation(CELL, row, column, ref, label, List.of(ref));
    }

    /** The short reference of a spreadsheet cell, such as {@code R2C3}. This is the breakpoint key suffix. */
    public static String cellRef(int row, int column) {
        return "R" + row + "C" + column;
    }

    /**
     * A decision-table fired-rule location.
     *
     * <p>It matches a breakpoint on any rule firing ({@link #RULE_FIRED_REF}) as well as one on any of
     * the specific rules that fired.
     *
     * @param ruleNames the names of the rules that fired (one for a single match, several for a collect)
     */
    public static CurrentLocation dtRule(List<String> ruleNames) {
        List<String> refs = new ArrayList<>(ruleNames.size() + 1);
        refs.add(RULE_FIRED_REF);
        refs.addAll(ruleNames);
        // A single rule is its own breakpoint key; a collect that fired many rules uses the any-rule key so
        // the step stays a single valid target (uri#rule) instead of an un-targetable comma-joined list.
        String ref = ruleNames.isEmpty() ? null
                : ruleNames.size() == 1 ? ruleNames.get(0)
                : RULE_FIRED_REF;
        return new CurrentLocation(DT_RULE, -1, -1, ref, ruleLabel(ruleNames), refs);
    }

    /** Compact label for the fired rules: the names, or the first few with a {@code +N more} for a big collect. */
    private static @Nullable String ruleLabel(List<String> ruleNames) {
        if (ruleNames.isEmpty()) {
            return null;
        }
        if (ruleNames.size() <= 3) {
            return String.join(", ", ruleNames);
        }
        return String.join(", ", ruleNames.subList(0, 3)) + " +" + (ruleNames.size() - 3) + " more";
    }

    /** A TBasic operation location. */
    public static CurrentLocation operation(@Nullable String label) {
        return new CurrentLocation(OPERATION, -1, -1, null, label, List.of());
    }
}
