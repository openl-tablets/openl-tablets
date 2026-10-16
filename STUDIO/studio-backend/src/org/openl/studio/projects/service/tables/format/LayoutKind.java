package org.openl.studio.projects.service.tables.format;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.AliasDatatypeMetaInfoReader;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.service.tables.OpenLTableUtils;

/**
 * A kind of table OpenL Studio lays out in the roles of its cells: the layout of its body, how the compiler reads it,
 * and how its text tells it is written when the compiler read none of it. A new kind of table is one constant here.
 */
@RequiredArgsConstructor
enum LayoutKind {

    DATATYPE(DatatypeLayout::fields, CompiledReads::datatype,
            new TextRead(DatatypeLayout::guessedFields, GuessedReads::typesTurned)),
    VOCABULARY(DatatypeLayout::values, CompiledReads::vocabulary),
    SPREADSHEET(SpreadsheetLayout::layOut, CompiledReads::spreadsheet, new TextRead(SpreadsheetLayout::guessed)),
    TBASIC(TBasicLayout::layOut, CompiledReads::tbasic),
    METHOD(BodyLayout.Placed::code, CompiledReads.ALWAYS),
    DATA(DataLayout::data, CompiledReads::data, new TextRead(DataLayout::guessedData, GuessedReads::fieldsTurned)),
    TEST(DataLayout::calls, CompiledReads::data, new TextRead(DataLayout::guessedCalls, GuessedReads::fieldsTurned)),
    RUN(DataLayout::calls, CompiledReads::data, new TextRead(DataLayout::guessedCalls, GuessedReads::fieldsTurned)),
    RULES(DecisionLayout::rules, CompiledReads::decision,
            new TextRead(DecisionLayout::guessedRules, GuessedReads::kindsTurned)),
    SIMPLE_RULES(DecisionLayout::rules, CompiledReads::decision, new TextRead(DecisionLayout::guessedRules)),
    SMART_RULES(DecisionLayout::rules, CompiledReads::decision, new TextRead(DecisionLayout::guessedRules)),
    SIMPLE_LOOKUP(DecisionLayout::rules, CompiledReads::decision, new TextRead(DecisionLayout::guessedRules)),
    SMART_LOOKUP(DecisionLayout::rules, CompiledReads::decision, new TextRead(DecisionLayout::guessedRules)),
    COLUMN_MATCH(ColumnMatchLayout::layOut, CompiledReads::columnMatch, new TextRead(ColumnMatchLayout::guessed)),
    CONDITIONS(DecisionLayout::conditions, CompiledReads::declarations,
            new TextRead(DecisionLayout::guessedConditions)),
    ACTIONS(DecisionLayout::actions, CompiledReads::declarations, new TextRead(DecisionLayout::guessedActions)),
    RETURNS(DecisionLayout::actions, CompiledReads::declarations, new TextRead(DecisionLayout::guessedActions)),
    ENVIRONMENT(NamedValuesLayout::layOut, CompiledReads.ALWAYS),
    PROPERTIES(NamedValuesLayout::layOut, CompiledReads::properties),
    CONSTANTS(DatatypeLayout::constants, CompiledReads::constants,
            new TextRead(DatatypeLayout::guessedConstants, GuessedReads::typesTurned));

    /** How the kind lays out its body as the compiler read it. */
    @Getter
    private final BodyLayout layout;

    /** How the compiler read a table of the kind, see {@link CompiledReads}. */
    private final Function<TableSyntaxNode, Optional<Boolean>> read;

    /** How a table of the kind the compiler read none of is read from its text, see {@link GuessedReads}. */
    @Getter
    private final TextRead text;

    /** A kind whose layout reads the sheet alone, so a table the compiler read none of is laid out alike. */
    LayoutKind(BodyLayout layout, Function<TableSyntaxNode, Optional<Boolean>> read) {
        this(layout, read, new TextRead(layout));
    }

    /**
     * How a table of a kind the compiler read none of is read from its text.
     *
     * @param layout how its body is laid out from its text
     * @param turned whether its text tells it is written with its rows and columns swapped
     */
    record TextRead(BodyLayout layout, Predicate<ILogicalTable> turned) {

        /** A table read as it is written. */
        TextRead(BodyLayout layout) {
            this(layout, body -> false);
        }
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
     * <p>A Datatype the compiler bound as the type of the values it lists is a Vocabulary, and so is one the compiler
     * read none of whose header names the type of its values, such as {@code Datatype Gender <String>}. A decision
     * table is of the
     * kind its header keyword names. A table OpenL Studio shows as of the type Other has no kind: a table of no kind
     * OpenL knows, and a part of a table split into several.
     *
     * @param table the table
     * @return the kind, or {@code null} for a table of the type Other
     */
    static @Nullable LayoutKind of(IOpenLTable table) {
        return switch (OpenLTableUtils.kindOf(table)) {
            case DATATYPE -> isVocabulary(table.getSyntaxNode()) ? VOCABULARY : DATATYPE;
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

    /** Whether a Datatype lists the values of a type: as the compiler bound it, or as its header names it. */
    private static boolean isVocabulary(TableSyntaxNode node) {
        if (node.getMetaInfoReader() instanceof AliasDatatypeMetaInfoReader) {
            return true;
        }
        return CompiledReads.datatype(node).isEmpty() && GuessedReads.isVocabulary(GuessedReads.headerOf(node));
    }

    /** The kind of a decision table, by the keyword of its header. */
    private static LayoutKind decisionOf(TableSyntaxNode node) {
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
