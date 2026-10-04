package org.openl.studio.projects.service.tables.theme;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * A table theme as its file describes it: the name it is shown by, and the look it gives each kind of table.
 *
 * <p>A theme is one look for every kind of table it styles: a Datatype, a Vocabulary, a Spreadsheet, a Data, a Test,
 * a Run and a decision table — a Rules, a SimpleRules, a SmartRules, a SimpleLookup or a SmartLookup table. The base
 * is the skin every table shares, such as its header and its properties, and each kind extends it. The key of a kind,
 * {@code datatype}, {@code vocabulary}, {@code spreadsheet}, {@code data}, {@code test}, {@code run},
 * {@code rules}, {@code simpleRules}, {@code smartRules}, {@code simpleLookup} or {@code smartLookup}, writes only
 * what the kind changes: every part it writes is laid over the same part of the base, attribute by attribute, and
 * every part it leaves out is the one of the base. A kind the theme writes nothing for takes the base alone.
 *
 * @param name         the name OpenL Studio shows the theme by
 * @param base         the skin every kind of table extends, or {@code null} for a theme whose kinds write everything
 * @param datatype     what a Datatype table changes in the base, or {@code null} for nothing
 * @param vocabulary   what a Vocabulary table, whose header declares the type of its values, changes in the base,
 *                     or {@code null} for nothing
 * @param spreadsheet  what a Spreadsheet table changes in the base, or {@code null} for nothing
 * @param data         what a Data table changes in the base, or {@code null} for nothing
 * @param test         what a Test table changes in the base, or {@code null} for nothing
 * @param run          what a Run table changes in the base, or {@code null} for nothing
 * @param rules        what a Rules table changes in the base, or {@code null} for nothing
 * @param simpleRules  what a SimpleRules table changes in the base, or {@code null} for nothing
 * @param smartRules   what a SmartRules table changes in the base, or {@code null} for nothing
 * @param simpleLookup what a SimpleLookup table changes in the base, or {@code null} for nothing
 * @param smartLookup  what a SmartLookup table changes in the base, or {@code null} for nothing
 */
public record TableTheme(String name,
                         @Nullable Look base,
                         @Nullable Look datatype,
                         @Nullable Look vocabulary,
                         @Nullable Look spreadsheet,
                         @Nullable Look data,
                         @Nullable Look test,
                         @Nullable Look run,
                         @Nullable Look rules,
                         @Nullable Look simpleRules,
                         @Nullable Look smartRules,
                         @Nullable Look simpleLookup,
                         @Nullable Look smartLookup) {

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
     * and the result. A Data, a Test and a Run table take the names, the titles, the values, the IDs and the empty
     * values. A decision table takes the code, the titles, the values, the horizontal conditions, the return titles,
     * the returns and the groups. Every kind takes the style, the header, the properties and the last row.
     *
     * @param style        the look every cell of the table starts from
     * @param header       the look of the header, the first row of the table
     * @param properties   the look of the rows of table properties under the header, such as the line that closes
     *                     them
     * @param titles       the look of the row naming the columns: of a Datatype that has one, of a Spreadsheet, the
     *                     titles of a Data, a Test or a Run table, and the titles of the conditions of a decision table
     *                     and of the column it names its rules in
     * @param stepTitle    the look of the title of the column of steps of a Spreadsheet, such as {@code Step}, laid
     *                     over the titles
     * @param type         the look of the column of field types of a Datatype
     * @param name         the look of the field names: the column of field names of a Datatype, and the rows of a
     *                     Data, a Test or a Run table that name its fields and the tables they take their values from
     * @param steps        the look of the column of step names of a Spreadsheet
     * @param values       the look of the values: the defaults and the other columns of a Datatype, the values of a
     *                     Vocabulary, the cells of the steps of a Spreadsheet, the values of a Data, a Test or a Run
     *                     table, and the values the conditions of a decision table are checked against and the names
     *                     of its rules
     * @param sections     the look of a step of a Spreadsheet whose name is merged across its row, laid over the
     *                     steps: a heading that splits the steps into sections
     * @param marked       the look of a step or a column of a Spreadsheet whose name is marked with {@code *} for the
     *                     result, laid over its own look
     * @param result       the look of the step whose value a Spreadsheet returns, when it returns a type other than
     *                     {@code SpreadsheetResult}, laid over its own look
     * @param ids          the look laid over the values that name a row of a Data table: the IDs of a Data table, and
     *                     the values a Test or a Run table takes from a Data table by their IDs
     * @param empty        the look laid over a value of a Data, a Test or a Run table that is not filled
     * @param code         the look of the rows a Rules table declares its columns in: the kind of each column, such
     *                     as {@code C1} or {@code RET1}, its expression and its parameters; a line it draws above or
     *                     below goes round the rows, not round each of them
     * @param horizontals  the look of the values of a horizontal condition, across the top of a lookup
     * @param returnTitles the look of the title of a column a decision table returns or acts in
     * @param returns      the look of the values a decision table returns or acts with
     * @param groups       the look laid over the first rule of a group and over the rule after the group, such as
     *                     the line that sets the group apart: a group is the rules a value of a condition is merged
     *                     over
     * @param lastRow      the look laid over the last row, such as the line that closes the table
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
                       @Nullable ThemeStyle ids,
                       @Nullable ThemeStyle empty,
                       @Nullable ThemeStyle code,
                       @Nullable ThemeStyle horizontals,
                       @Nullable ThemeStyle returnTitles,
                       @Nullable ThemeStyle returns,
                       @Nullable ThemeStyle groups,
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
                    .ids(lay(ids, over.ids))
                    .empty(lay(empty, over.empty))
                    .code(lay(code, over.code))
                    .horizontals(lay(horizontals, over.horizontals))
                    .returnTitles(lay(returnTitles, over.returnTitles))
                    .returns(lay(returns, over.returns))
                    .groups(lay(groups, over.groups))
                    .lastRow(lay(lastRow, over.lastRow))
                    .build();
        }
    }

    /**
     * The look of the header row.
     *
     * <p>The header text is formatted in pieces, as the compiler reads it: the keyword, the name of the table, the
     * type the header names, and the parameters of a Spreadsheet or a decision table. The text itself is never
     * changed.
     *
     * @param style      the look of the header cell
     * @param keyword    the look of the keyword, such as {@code Datatype}, {@code Spreadsheet} or {@code Rules}
     * @param name       the look of the name of the table
     * @param type       the look of the type the header names besides the table: the type of the values of a
     *                   Vocabulary, such as {@code <String>}, the parent a Datatype extends, such as
     *                   {@code extends Parent}, the type a Spreadsheet or a decision table returns, such as
     *                   {@code SpreadsheetResult} or {@code Collect Error[]}, the type of the rows of a Data table,
     *                   such as {@code Policy}, or the method a Test or a Run table calls
     * @param parameters the look of the parameters of a Spreadsheet or a decision table, such as
     *                   {@code (Policy policy)}
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
