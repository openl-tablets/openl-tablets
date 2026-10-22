package org.openl.rules.xls.merge.diff;

/**
 * A cell of a merged table, by its row and its column counted from the top left cell of the table, from 0.
 */
public record TableCell(int row, int column) {
}
