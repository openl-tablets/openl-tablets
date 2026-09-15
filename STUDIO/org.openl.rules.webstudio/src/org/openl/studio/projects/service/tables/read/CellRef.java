package org.openl.studio.projects.service.tables.read;

/**
 * Where a cell sits in the matrix a table is read as.
 *
 * @param row 0-based row
 * @param col 0-based column
 */
record CellRef(int row, int col) {
}
