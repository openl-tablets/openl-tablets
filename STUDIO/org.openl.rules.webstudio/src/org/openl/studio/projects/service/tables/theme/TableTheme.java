package org.openl.studio.projects.service.tables.theme;

import org.jspecify.annotations.Nullable;

/**
 * A table theme as its file describes it: the name it is shown by, and the look it gives each kind of table.
 *
 * <p>A theme styles only the kinds it names a look for: {@code datatype} for a Datatype, {@code vocabulary} for a
 * Vocabulary. A table of a kind it names none for is left as it is.
 *
 * <p>The base is a look to build the others from, and styles no table by itself. A kind extends it with the YAML
 * merge key ({@code <<: *base}) and replaces whatever part of it the kind writes itself.
 *
 * @param name       the name OpenL Studio shows the theme by
 * @param base       the look the looks of the kinds are built from
 * @param datatype   the look of a Datatype table, or {@code null} to leave Datatype tables as they are
 * @param vocabulary the look of a Vocabulary table, whose header declares the type of its values, or
 *                   {@code null} to leave Vocabulary tables as they are
 */
public record TableTheme(String name, @Nullable Look base, @Nullable Look datatype, @Nullable Look vocabulary) {

    /** The look a table of the given kind takes, or {@code null} when the theme leaves such a table as it is. */
    @Nullable
    Look lookOf(boolean vocabularyTable) {
        return vocabularyTable ? vocabulary : datatype;
    }

    /**
     * The look of one kind of table.
     *
     * <p>A Vocabulary has one column of values: of the body only {@code values} and {@code lastRow} apply to it.
     *
     * @param style   the look every cell of the table starts from
     * @param header  the look of the header, the first row of the table
     * @param titles  the look of the row naming the columns, for a Datatype that has one
     * @param type    the look of the column of field types
     * @param name    the look of the column of field names
     * @param values  the look of every other column: defaults, descriptions, examples, or the values of a
     *                Vocabulary
     * @param lastRow the look laid over the last row, such as the line that closes the table
     */
    public record Look(@Nullable ThemeStyle style,
                       @Nullable Header header,
                       @Nullable ThemeStyle titles,
                       @Nullable ThemeStyle type,
                       @Nullable ThemeStyle name,
                       @Nullable ThemeStyle values,
                       @Nullable ThemeStyle lastRow) {
    }

    /**
     * The look of the header row.
     *
     * <p>The header text is formatted in pieces: the keyword, the name of the type, and what follows it, which is
     * the type of the values of a Vocabulary or the parent a Datatype extends. The text itself is never changed.
     *
     * @param style   the look of the header cell
     * @param keyword the look of the keyword, such as {@code Datatype}
     * @param name    the look of the name of the type
     * @param type    the look of what follows the name, such as {@code <String>} or {@code extends Parent}
     */
    public record Header(@Nullable ThemeStyle style,
                         @Nullable ThemeStyle keyword,
                         @Nullable ThemeStyle name,
                         @Nullable ThemeStyle type) {
    }
}
