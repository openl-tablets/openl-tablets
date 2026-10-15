package org.openl.studio.projects.service.tables.format;

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
 * A kind of table OpenL Studio lays out in the roles of its cells: the layout of its body, and how the compiler reads
 * it. A new kind of table is one constant here.
 */
@RequiredArgsConstructor
enum LayoutKind {

    DATATYPE(DatatypeLayout::fields, CompiledReads::datatype),
    VOCABULARY(DatatypeLayout::values, CompiledReads::vocabulary),
    SPREADSHEET(SpreadsheetLayout::layOut, CompiledReads::spreadsheet),
    TBASIC(TBasicLayout::layOut, CompiledReads::tbasic),
    METHOD(BodyLayout.Placed::code, CompiledReads.ALWAYS),
    DATA(DataLayout::data, CompiledReads::data),
    TEST(DataLayout::calls, CompiledReads::data),
    RUN(DataLayout::calls, CompiledReads::data),
    RULES(DecisionLayout::rules, CompiledReads::decision),
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
    CONSTANTS(DatatypeLayout::constants, CompiledReads::constants);

    /** How the kind lays out its body. */
    @Getter
    private final BodyLayout layout;

    /** How the compiler read a table of the kind, see {@link CompiledReads}. */
    private final Function<TableSyntaxNode, Optional<Boolean>> read;

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
     * kind its header keyword names. A table OpenL Studio shows as of the type Other has no kind: a table of no kind
     * OpenL knows, and a part of a table split into several.
     *
     * @param table the table
     * @return the kind, or {@code null} for a table of the type Other
     */
    static @Nullable LayoutKind of(IOpenLTable table) {
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
