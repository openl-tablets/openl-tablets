package org.openl.rules.tableeditor.model;

/**
 * The bounds a number is entered within.
 *
 * @param min     the smallest value the cell's type holds
 * @param max     the largest value the cell's type holds
 * @param intOnly {@code true} when only whole numbers are accepted
 */
public record RangeParam(Number min, Number max, boolean intOnly) {
}
