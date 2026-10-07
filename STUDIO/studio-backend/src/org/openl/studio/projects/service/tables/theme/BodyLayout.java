package org.openl.studio.projects.service.tables.theme;

import org.openl.rules.table.ILogicalTable;

/**
 * How a kind of table lays out its body: the places it reads the body in, and the look of each place.
 *
 * <p>A layout tells the look of a place only. {@link ThemeLayouts} gives that look to every cell of the sheet the
 * place takes, and the last-row look to every cell that reaches the bottom of the table.
 */
@FunctionalInterface
interface BodyLayout {

    /**
     * The places of a body and the look of each, for a table the compiler read.
     *
     * @param body the body of a table, and what its layout knows about the table
     * @return the body as the layout reads it, and the look of each of its places
     */
    Placed layOut(ThemedBody body);

    /**
     * A body as a layout reads it.
     *
     * @param places the body, a place in each of its cells: read as written, or with its rows and columns swapped
     * @param look   the look of each place
     */
    record Placed(ILogicalTable places, ThemeLayouts.PlaceLook look) {

        /** A body whose every place takes the look every cell starts from. */
        static Placed plain(ThemedBody body) {
            var base = body.base();
            return new Placed(body.rows(), (cell, column, row) -> base);
        }
    }
}
