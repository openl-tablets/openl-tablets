package org.openl.studio.projects.service.tables.theme;

import org.openl.studio.projects.model.tables.RawTableRole;

/**
 * Decides which look a theme gives each cell of the body of an Environment or a Properties table: the tables that
 * name a value in each row.
 *
 * <p>An Environment table names a setting in its first column, such as {@code import} or {@code dependency}, and a
 * Properties table names a property there, such as {@code scope}. The first column is the key of the table, and gets
 * the name look; the values after it are its values, and get the value look. Every cell starts from the look of the whole table, and a cell that reaches the bottom of
 * the table gets the last-row look over its own.
 *
 * <p>Both tables are read as they are written, as the properties of any table are: the loader reads every Environment
 * table by its rows, and the compiler binds a Properties table as the properties section of its tables.
 */
final class NamedValuesThemeLayout {

    /** The name of a setting or a property, a key of the table. */
    private static final Place NAME = Place.of(RawTableRole.KEY, Parts.BASE.with(Parts.Part.NAME));

    /** The value of a setting or a property. */
    private static final Place VALUE = Place.of(RawTableRole.VALUE, Parts.BASE.with(Parts.Part.VALUES));

    private NamedValuesThemeLayout() {
    }

    /**
     * The places of the body of a table that names a value in each row, read as it is written.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed layOut(ThemedBody body) {
        return new BodyLayout.Placed(body.rows(), (cell, column, row) -> column == 0 ? NAME : VALUE);
    }
}
