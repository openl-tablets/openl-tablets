package org.openl.studio.projects.service.tables.theme;

/**
 * Decides which look a theme gives each cell of the body of an Environment or a Properties table: the tables that
 * name a value in each row.
 *
 * <p>An Environment table names a setting in its first column, such as {@code import} or {@code dependency}, and a
 * Properties table names a property there, such as {@code scope}. The first column gets the name look and the values
 * after it the value look. Every cell starts from the look of the whole table, and a cell that reaches the bottom of
 * the table gets the last-row look over its own.
 *
 * <p>Both tables are read as they are written, as the properties of any table are.
 */
final class NamedValuesThemeLayout {

    private NamedValuesThemeLayout() {
    }

    /**
     * The places of the body of a table that names a value in each row, read as it is written.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
     */
    static BodyLayout.Placed layOut(ThemedBody body) {
        var name = body.base().with(body.look().name());
        var values = body.base().with(body.look().values());
        return new BodyLayout.Placed(body.rows(), (cell, column, row) -> column == 0 ? name : values);
    }
}
