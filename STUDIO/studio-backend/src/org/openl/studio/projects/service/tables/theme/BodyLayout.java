package org.openl.studio.projects.service.tables.theme;

import java.util.Set;

import org.openl.rules.table.ILogicalTable;
import org.openl.studio.projects.model.tables.RawTableRole;

/**
 * How a kind of table lays out its body: the places it reads the body in, what each place is, and the lines that
 * start a group.
 *
 * <p>A layout tells what a place is only. {@link TableRegionLayout} gives it to every cell of the sheet the place
 * takes, and the last row to every cell that reaches the bottom of the table.
 */
@FunctionalInterface
interface BodyLayout {

    /**
     * The places of a body and the parts of each, for a table the compiler read.
     *
     * @param body the body of a table, and what its layout knows about the table
     * @return the body as the layout reads it, and the parts of each of its places
     */
    Placed layOut(ThemedBody body);

    /**
     * A body as a layout reads it.
     *
     * @param places       the body, a place in each of its cells: read as written, or with its rows and columns
     *                     swapped
     * @param look         what each place is
     * @param groupRows    the rows of the sheet that start a group, a line drawn above each
     * @param groupColumns the columns of the sheet that start a group, a line drawn on the left of each
     */
    record Placed(ILogicalTable places, TableRegionLayout.PlaceLook look, Set<Integer> groupRows,
                  Set<Integer> groupColumns) {

        /** The places of a body with no groups. */
        Placed(ILogicalTable places, TableRegionLayout.PlaceLook look) {
            this(places, look, Set.of(), Set.of());
        }

        /** The value every place of a body the compiler read none of takes, with the look every cell starts from. */
        private static final Place PLAIN = Place.of(RawTableRole.VALUE, Parts.BASE);

        /** The code every place of the body of a Method table holds, with the look every cell starts from. */
        private static final Place CODE = Place.code(RawTableRole.VALUE, Parts.BASE);

        /** A body the compiler read none of: every place holds a value. */
        static Placed plain(ThemedBody body) {
            return new Placed(body.rows(), (cell, column, row) -> PLAIN);
        }

        /** A body of code, as a Method table writes it: every place holds a value that is code. */
        static Placed code(ThemedBody body) {
            return new Placed(body.rows(), (cell, column, row) -> CODE);
        }
    }
}
