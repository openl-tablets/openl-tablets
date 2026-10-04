package org.openl.studio.projects.service.tables.theme;

import lombok.Builder;

import org.openl.rules.table.ILogicalTable;

/**
 * The body of a table a theme lays out, and what its layout knows about the table.
 *
 * @param rows       the body: the rows of the table under its header and the properties it declares
 * @param header     the text of the header, which tells some kinds how to lay out the body, such as the type a
 *                   Spreadsheet returns
 * @param base       the look every cell of the table starts from
 * @param look       the look of the table
 * @param transposed whether the compiler read the table with its rows and columns swapped
 */
@Builder
record ThemedBody(ILogicalTable rows,
                  String header,
                  ThemeStyle base,
                  TableTheme.Look look,
                  boolean transposed) {

    /** The body read the way the compiler reads it: with its rows and columns swapped when it is transposed. */
    ILogicalTable upright() {
        return transposed ? rows.transpose() : rows;
    }
}
