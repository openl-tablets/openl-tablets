package org.openl.rules.tableeditor.model;

import java.util.List;

/**
 * The bounds a number is entered within.
 *
 * @param min       the smallest value the cell's type holds
 * @param max       the largest value the cell's type holds
 * @param intOnly   {@code true} when only whole numbers are accepted
 * @param constants the names of the constants of the module the cell may hold instead of a number
 */
public record RangeParam(Number min, Number max, boolean intOnly, List<String> constants) {
}
