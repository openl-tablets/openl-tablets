package org.openl.studio.projects.service.tables.theme;

import java.util.Optional;
import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.AliasDatatypeMetaInfoReader;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.service.tables.OpenLTableUtils;

/**
 * A kind of table a theme styles: the part of a theme the kind takes its look from, the layout of its body, and how
 * the compiler reads it.
 *
 * <p>Every kind takes the part of the theme named after it. A kind that looks like another, such as a Test table that
 * looks like a Data table, is an alias in the theme file, never a rule here. A new kind of table is one constant here
 * and one part of {@link TableTheme}.
 */
@RequiredArgsConstructor
enum ThemeKind {

    DATATYPE(TableTheme::datatype, DatatypeThemeLayout::fields, CompiledReads::datatype),
    VOCABULARY(TableTheme::vocabulary, DatatypeThemeLayout::values, CompiledReads::vocabulary),
    SPREADSHEET(TableTheme::spreadsheet, SpreadsheetThemeLayout::layOut, CompiledReads::spreadsheet),
    TBASIC(TableTheme::tbasic, TBasicThemeLayout::layOut, CompiledReads::tbasic),
    METHOD(TableTheme::method, BodyLayout.Placed::plain, CompiledReads.ALWAYS),
    DATA(TableTheme::data, DataThemeLayout::data, CompiledReads::data),
    TEST(TableTheme::test, DataThemeLayout::calls, CompiledReads::data),
    RUN(TableTheme::run, DataThemeLayout::calls, CompiledReads::data),
    RULES(TableTheme::rules, DecisionThemeLayout::rules, CompiledReads::decision),
    SIMPLE_RULES(TableTheme::simpleRules, DecisionThemeLayout::rules, CompiledReads::decision),
    SMART_RULES(TableTheme::smartRules, DecisionThemeLayout::rules, CompiledReads::decision),
    SIMPLE_LOOKUP(TableTheme::simpleLookup, DecisionThemeLayout::rules, CompiledReads::decision),
    SMART_LOOKUP(TableTheme::smartLookup, DecisionThemeLayout::rules, CompiledReads::decision),
    COLUMN_MATCH(TableTheme::columnMatch, ColumnMatchThemeLayout::layOut, CompiledReads::columnMatch),
    CONDITIONS(TableTheme::conditions, DecisionThemeLayout::conditions, CompiledReads::declarations),
    ACTIONS(TableTheme::actions, DecisionThemeLayout::actions, CompiledReads::declarations),
    RETURNS(TableTheme::returns, DecisionThemeLayout::actions, CompiledReads::declarations),
    ENVIRONMENT(TableTheme::environment, NamedValuesThemeLayout::layOut, CompiledReads.ALWAYS),
    PROPERTIES(TableTheme::properties, NamedValuesThemeLayout::layOut, CompiledReads::properties),
    CONSTANTS(TableTheme::constants, DatatypeThemeLayout::constants, CompiledReads::constants);

    /** The part of a theme that tells what the kind changes in the base. */
    private final Function<TableTheme, TableTheme.@Nullable Look> part;

    /** How the kind lays out its body. */
    @Getter
    private final BodyLayout layout;

    /** How the compiler read a table of the kind, see {@link CompiledReads}. */
    private final Function<TableSyntaxNode, Optional<Boolean>> read;

    /**
     * The look a theme gives a table of this kind: the base, extended by the part named after the kind.
     *
     * @param theme the theme
     * @return the look of the kind
     */
    TableTheme.Look lookIn(TableTheme theme) {
        return theme.lookOf(part.apply(theme));
    }

    /**
     * How the compiler read a table of this kind.
     *
     * @param node the table
     * @return whether the compiler read the table with its rows and columns swapped, or empty for a table it read none
     *         of
     */
    Optional<Boolean> readOf(TableSyntaxNode node) {
        return read.apply(node);
    }

    /**
     * The kind of a table, told as OpenL Studio tells the kinds apart.
     *
     * <p>A Datatype the compiler bound as the type of the values it lists is a Vocabulary. A decision table is of the
     * kind its header keyword names. A table OpenL Studio shows as of the type Other has no kind a theme styles: a
     * table of no kind OpenL knows, and a part of a table split into several.
     *
     * @param table the table
     * @return the kind, or {@code null} for a table no theme styles
     */
    static @Nullable ThemeKind of(IOpenLTable table) {
        return switch (OpenLTableUtils.kindOf(table)) {
            case DATATYPE -> table.getSyntaxNode().getMetaInfoReader() instanceof AliasDatatypeMetaInfoReader
                    ? VOCABULARY
                    : DATATYPE;
            case SPREADSHEET -> SPREADSHEET;
            case TBASIC -> TBASIC;
            case METHOD -> METHOD;
            case DATA -> DATA;
            case TEST -> TEST;
            case RUN -> RUN;
            case RULES -> decisionOf(table.getSyntaxNode());
            case COLUMN_MATCH -> COLUMN_MATCH;
            case CONDITIONS -> CONDITIONS;
            case ACTIONS -> ACTIONS;
            case RETURNS -> RETURNS;
            case ENVIRONMENT -> ENVIRONMENT;
            case PROPERTIES -> PROPERTIES;
            case CONSTANTS -> CONSTANTS;
            case OTHER -> null;
        };
    }

    /** The kind of a decision table, by the keyword of its header. */
    private static ThemeKind decisionOf(TableSyntaxNode node) {
        if (DecisionTableHelper.isSimpleDecisionTable(node)) {
            return SIMPLE_RULES;
        }
        if (DecisionTableHelper.isSmartDecisionTable(node)) {
            return SMART_RULES;
        }
        if (DecisionTableHelper.isSimpleLookupTable(node)) {
            return SIMPLE_LOOKUP;
        }
        return DecisionTableHelper.isSmartLookupTable(node) ? SMART_LOOKUP : RULES;
    }
}
