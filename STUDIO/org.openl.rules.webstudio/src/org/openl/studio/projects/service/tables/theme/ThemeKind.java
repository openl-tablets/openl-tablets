package org.openl.studio.projects.service.tables.theme;

import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.service.tables.OpenLTableUtils;

/**
 * A kind of table a theme styles: the part of a theme the kind takes its look from, and the layout of its body.
 *
 * <p>Every kind takes the part of the theme named after it. A kind that looks like another, such as a Test table that
 * looks like a Data table, is an alias in the theme file, never a rule here. A new kind of table is one constant here
 * and one part of {@link TableTheme}.
 */
@RequiredArgsConstructor
enum ThemeKind {

    DATATYPE(TableTheme::datatype, DatatypeThemeLayout::fields),
    VOCABULARY(TableTheme::vocabulary, DatatypeThemeLayout::values),
    SPREADSHEET(TableTheme::spreadsheet, SpreadsheetThemeLayout::layOut),
    DATA(TableTheme::data, DataThemeLayout::data),
    TEST(TableTheme::test, DataThemeLayout::calls),
    RUN(TableTheme::run, DataThemeLayout::calls),
    RULES(TableTheme::rules, DecisionThemeLayout::rules),
    SIMPLE_RULES(TableTheme::simpleRules, DecisionThemeLayout::rules),
    SMART_RULES(TableTheme::smartRules, DecisionThemeLayout::rules),
    SIMPLE_LOOKUP(TableTheme::simpleLookup, DecisionThemeLayout::rules),
    SMART_LOOKUP(TableTheme::smartLookup, DecisionThemeLayout::rules),
    CONDITIONS(TableTheme::conditions, DecisionThemeLayout::conditions),
    ACTIONS(TableTheme::actions, DecisionThemeLayout::actions),
    RETURNS(TableTheme::returns, DecisionThemeLayout::actions),
    ENVIRONMENT(TableTheme::environment, NamedValuesThemeLayout::layOut),
    PROPERTIES(TableTheme::properties, NamedValuesThemeLayout::layOut),
    CONSTANTS(TableTheme::constants, DatatypeThemeLayout::fields);

    /** The part of a theme that tells what the kind changes in the base. */
    private final Function<TableTheme, TableTheme.@Nullable Look> part;

    /** How the kind lays out its body. */
    @Getter
    private final BodyLayout layout;

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
     * The kind of a table, told as OpenL Studio tells the kinds apart.
     *
     * <p>A Datatype whose header declares the type of its values is a Vocabulary. A decision table is of the kind its
     * header keyword names. A table of any other kind has no kind a theme styles.
     *
     * @param table  the table
     * @param header the text of its header
     * @return the kind, or {@code null} for a table no theme styles
     */
    static @Nullable ThemeKind of(IOpenLTable table, String header) {
        return switch (OpenLTableUtils.kindOf(table)) {
            case DATATYPE -> OpenLTableUtils.isVocabularyHeader(header) ? VOCABULARY : DATATYPE;
            case SPREADSHEET -> SPREADSHEET;
            case DATA -> DATA;
            case TEST -> TEST;
            case RUN -> RUN;
            case RULES -> decisionOf(table.getSyntaxNode());
            case CONDITIONS -> CONDITIONS;
            case ACTIONS -> ACTIONS;
            case RETURNS -> RETURNS;
            case ENVIRONMENT -> ENVIRONMENT;
            case PROPERTIES -> PROPERTIES;
            case CONSTANTS -> CONSTANTS;
            default -> null;
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
