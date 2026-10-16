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

    DATATYPE(DatatypeLayout::fields, CompiledReads::datatype, GuessedReads::typesTurned),
    VOCABULARY(DatatypeLayout::values, CompiledReads::vocabulary),
    SPREADSHEET(SpreadsheetLayout::layOut, CompiledReads::spreadsheet),
    TBASIC(TBasicLayout::layOut, CompiledReads::tbasic),
    METHOD(BodyLayout.Placed::code, CompiledReads.ALWAYS),
    DATA(DataLayout::data, CompiledReads::data, GuessedReads::fieldsTurned),
    TEST(DataLayout::calls, CompiledReads::data, GuessedReads::fieldsTurned),
    RUN(DataLayout::calls, CompiledReads::data, GuessedReads::fieldsTurned),
    RULES(DecisionLayout::rules, CompiledReads::decision, GuessedReads::kindsTurned),
    SIMPLE_RULES(DecisionLayout::rules, CompiledReads::decision),
    SMART_RULES(DecisionLayout::rules, CompiledReads::decision),
    SIMPLE_LOOKUP(DecisionLayout::rules, CompiledReads::decision),
    SMART_LOOKUP(DecisionLayout::rules, CompiledReads::decision),
    COLUMN_MATCH(ColumnMatchLayout::layOut, CompiledReads::columnMatch),
    CONDITIONS(DecisionLayout::conditions, CompiledReads::declarations),
    ACTIONS(DecisionLayout::actions, CompiledReads::declarations),
    RETURNS(DecisionLayout::actions, CompiledReads::declarations),
    ENVIRONMENT(NamedValuesLayout::layOut, CompiledReads.ALWAYS),
    PROPERTIES(NamedValuesLayout::layOut, CompiledReads::properties),
    CONSTANTS(DatatypeLayout::constants, CompiledReads::constants, GuessedReads::typesTurned);

    /** How the kind lays out its body. */
    @Getter
    private final BodyLayout layout;

    /** How the compiler read a table of the kind, see {@link CompiledReads}. */
    private final Function<TableSyntaxNode, Optional<Boolean>> read;

    /** Whether the text of the body of a table the compiler read none of tells it is written transposed. */
    private final Predicate<ILogicalTable> turned;

    /** A kind whose tables the compiler read none of are read as they are written. */
    LayoutKind(BodyLayout layout, Function<TableSyntaxNode, Optional<Boolean>> read) {
        this(layout, read, body -> false);
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
     * Whether the text of a table of this kind the compiler read none of tells it is written with its rows and columns
     * swapped, see {@link GuessedReads}.
     *
     * @param body the body of the table
     * @return {@code true} for a table its text tells is written transposed
     */
    boolean isTurned(ILogicalTable body) {
        return turned.test(body);
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
        var header = node.getGridTable().getCell(0, 0).getStringValue();
        return CompiledReads.datatype(node).isEmpty() && header != null && GuessedReads.isVocabulary(header);
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
