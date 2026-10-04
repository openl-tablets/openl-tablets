package org.openl.studio.projects.service.tables.theme;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * A table theme as its file describes it: the name it is shown by, and the look it gives each kind of table.
 *
 * <p>A theme is one look for every kind of table it styles: a Datatype, a Vocabulary and a Spreadsheet. The base is the
 * skin every table shares, such as its header and its properties, and each kind extends it. The key of a kind,
 * {@code datatype}, {@code vocabulary} or {@code spreadsheet}, writes only what the kind changes: every part it writes
 * is laid over the same part of the base, attribute by attribute, and every part it leaves out is the one of the base.
 * A kind the theme writes nothing for takes the base alone.
 *
 * @param name        the name OpenL Studio shows the theme by
 * @param base        the skin every kind of table extends, or {@code null} for a theme whose kinds write everything
 * @param datatype    what a Datatype table changes in the base, or {@code null} for nothing
 * @param vocabulary  what a Vocabulary table, whose header declares the type of its values, changes in the base, or
 *                    {@code null} for nothing
 * @param spreadsheet what a Spreadsheet table changes in the base, or {@code null} for nothing
 */
public record TableTheme(String name,
                         @Nullable Look base,
                         @Nullable Look datatype,
                         @Nullable Look vocabulary,
                         @Nullable Look spreadsheet) {

    /**
     * The look a kind of table takes: the base, extended by what the kind changes in it.
     *
     * @param kind what the kind changes in the base, one of the kinds of this theme such as {@link #spreadsheet()}, or
     *             {@code null} for nothing
     * @return the look of the kind
     */
    Look lookOf(@Nullable Look kind) {
        var skin = base == null ? Look.builder().build() : base;
        return kind == null ? skin : skin.extendedBy(kind);
    }

    /** Lays one look over another, either of which may be missing. */
    private static @Nullable ThemeStyle lay(@Nullable ThemeStyle under, @Nullable ThemeStyle over) {
        return under == null ? over : under.with(over);
    }

    /**
     * The look of one kind of table.
     *
     * <p>Each kind takes the parts that suit it, and a part of another kind is not used. A Datatype takes the titles,
     * the types, the names and the values. A Vocabulary has one column of values: of the body only the values apply
     * to it. A Spreadsheet takes the titles, the step title, the steps, the values, the sections, the marked names
     * and the result. Every kind takes the style, the header, the properties and the last row.
     *
     * @param style      the look every cell of the table starts from
     * @param header     the look of the header, the first row of the table
     * @param properties the look of the rows of table properties under the header, such as the line that closes
     *                   them
     * @param titles     the look of the row naming the columns: of a Datatype that has one, and of a Spreadsheet
     * @param stepTitle  the look of the title of the column of steps of a Spreadsheet, such as {@code Step}, laid
     *                   over the titles
     * @param type       the look of the column of field types of a Datatype
     * @param name       the look of the column of field names of a Datatype
     * @param steps      the look of the column of step names of a Spreadsheet
     * @param values     the look of the values: the defaults and the other columns of a Datatype, the values of a
     *                   Vocabulary, and the cells of the steps of a Spreadsheet
     * @param sections   the look of a step of a Spreadsheet whose name is merged across its row, laid over the
     *                   steps: a heading that splits the steps into sections
     * @param marked     the look of a step or a column of a Spreadsheet whose name is marked with {@code *} for the
     *                   result, laid over its own look
     * @param result     the look of the step whose value a Spreadsheet returns, when it returns a type other than
     *                   {@code SpreadsheetResult}, laid over its own look
     * @param lastRow    the look laid over the last row, such as the line that closes the table
     */
    @Builder
    public record Look(@Nullable ThemeStyle style,
                       @Nullable Header header,
                       @Nullable ThemeStyle properties,
                       @Nullable ThemeStyle titles,
                       @Nullable ThemeStyle stepTitle,
                       @Nullable ThemeStyle type,
                       @Nullable ThemeStyle name,
                       @Nullable ThemeStyle steps,
                       @Nullable ThemeStyle values,
                       @Nullable ThemeStyle sections,
                       @Nullable ThemeStyle marked,
                       @Nullable ThemeStyle result,
                       @Nullable ThemeStyle lastRow) {

        /**
         * This look extended by another: every part the other writes is laid over the same part of this one.
         *
         * @param over the look that extends this one
         * @return the look both give together
         */
        Look extendedBy(Look over) {
            return Look.builder()
                    .style(lay(style, over.style))
                    .header(header == null ? over.header : header.extendedBy(over.header))
                    .properties(lay(properties, over.properties))
                    .titles(lay(titles, over.titles))
                    .stepTitle(lay(stepTitle, over.stepTitle))
                    .type(lay(type, over.type))
                    .name(lay(name, over.name))
                    .steps(lay(steps, over.steps))
                    .values(lay(values, over.values))
                    .sections(lay(sections, over.sections))
                    .marked(lay(marked, over.marked))
                    .result(lay(result, over.result))
                    .lastRow(lay(lastRow, over.lastRow))
                    .build();
        }
    }

    /**
     * The look of the header row.
     *
     * <p>The header text is formatted in pieces, as the compiler reads it: the keyword, the name of the table, the
     * type the header names, and the parameters of a Spreadsheet. The text itself is never changed.
     *
     * @param style      the look of the header cell
     * @param keyword    the look of the keyword, such as {@code Datatype} or {@code Spreadsheet}
     * @param name       the look of the name of the table
     * @param type       the look of the type the header names besides the table: the type of the values of a
     *                   Vocabulary, such as {@code <String>}, the parent a Datatype extends, such as
     *                   {@code extends Parent}, or the type a Spreadsheet returns, such as {@code SpreadsheetResult}
     * @param parameters the look of the parameters of a Spreadsheet, such as {@code (Policy policy)}
     */
    @Builder
    public record Header(@Nullable ThemeStyle style,
                         @Nullable ThemeStyle keyword,
                         @Nullable ThemeStyle name,
                         @Nullable ThemeStyle type,
                         @Nullable ThemeStyle parameters) {

        /**
         * This header extended by another: every piece the other writes is laid over the same piece of this one.
         *
         * @param over the header that extends this one, or {@code null} for none
         * @return the header both give together
         */
        Header extendedBy(@Nullable Header over) {
            if (over == null) {
                return this;
            }
            return Header.builder()
                    .style(lay(style, over.style))
                    .keyword(lay(keyword, over.keyword))
                    .name(lay(name, over.name))
                    .type(lay(type, over.type))
                    .parameters(lay(parameters, over.parameters))
                    .build();
        }
    }
}
