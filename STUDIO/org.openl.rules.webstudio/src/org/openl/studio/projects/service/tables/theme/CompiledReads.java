package org.openl.studio.projects.service.tables.theme;

import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.openl.rules.cmatch.algorithm.MatchAlgorithmFactory;
import org.openl.rules.data.ITable;
import org.openl.rules.dt.DTInfo;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.AlgorithmMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.AliasDatatypeMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.ColumnMatchMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.ConstantsTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DataTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DatatypeTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DtColumnsDefinitionMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.PropertyTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;

/**
 * How the compiler read a table of each kind a theme styles: whether it read the table with its rows and columns
 * swapped, or nothing at all.
 *
 * <p>A layout takes the parts of a body from what the compiler read of the table. A table the compiler read none of
 * has no parts it knows: its type, its parent type or its tested table is missing, its body cannot be built, or its
 * header names nothing the compiler knows. Its body takes the look every cell starts from, the General format of the
 * shipped themes, rather than a look guessed from where its cells stand.
 *
 * <p>An Environment and a Method table have no compiled parts a theme needs: the loader reads an Environment by its
 * rows, and the body of a Method table is code.
 */
final class CompiledReads {

    /** A table read as it is written. */
    private static final Optional<Boolean> UPRIGHT = Optional.of(false);

    /** How the compiler reads a table it reads whatever it holds: as it is written. */
    static final Function<TableSyntaxNode, Optional<Boolean>> ALWAYS = node -> UPRIGHT;

    private CompiledReads() {
    }

    /**
     * A Datatype: its fields, read with a field in each row, or in each column of a transposed table. The fields are
     * read only once the parent type is found.
     */
    static Optional<Boolean> datatype(TableSyntaxNode node) {
        return node.getMetaInfoReader() instanceof DatatypeTableMetaInfoReader reader
                && reader.getBoundNode().getColumnTitlesOrder() != null
                ? turned(reader.getBoundNode().getTable())
                : Optional.empty();
    }

    /** A Vocabulary: its values, which the compiler read once it found the type of the values. */
    static Optional<Boolean> vocabulary(TableSyntaxNode node) {
        return node.getMetaInfoReader() instanceof AliasDatatypeMetaInfoReader ? UPRIGHT : Optional.empty();
    }

    /** A Constants table: a constant in each row, or in each column of a transposed table. */
    static Optional<Boolean> constants(TableSyntaxNode node) {
        return node.getMetaInfoReader() instanceof ConstantsTableMetaInfoReader reader
                ? turned(reader.getBoundNode().getNormalizedData())
                : Optional.empty();
    }

    /** A Spreadsheet: its steps and its columns, read once the body is bound. */
    static Optional<Boolean> spreadsheet(TableSyntaxNode node) {
        return node.getMetaInfoReader() instanceof SpreadsheetMetaInfoReader reader
                && reader.getBoundNode().getCells() != null
                && reader.getBoundNode().getStructureBuilder() != null
                ? UPRIGHT
                : Optional.empty();
    }

    /** A TBasic table: its steps, compiled once every row of it is read. */
    static Optional<Boolean> tbasic(TableSyntaxNode node) {
        return node.getMetaInfoReader() instanceof AlgorithmMetaInfoReader reader
                && reader.getBoundNode().getAlgorithm() != null
                && reader.getBoundNode().getAlgorithm().getAlgorithmSteps() != null
                ? UPRIGHT
                : Optional.empty();
    }

    /**
     * A Data, a Test or a Run table: a field in each column, or in each row of a transposed table. The compiler reads
     * none of a table whose type is missing or defined with errors, or whose tested table does not exist.
     */
    static Optional<Boolean> data(TableSyntaxNode node) {
        return node.getMetaInfoReader() instanceof DataTableMetaInfoReader reader
                && reader.getBoundNode().getTable() instanceof ITable compiled
                ? turned(compiled.getData())
                : Optional.empty();
    }

    /** A decision table: a rule in each row, or in each column of a table compiled transposed. */
    static Optional<Boolean> decision(TableSyntaxNode node) {
        return node.getMetaInfoReader() instanceof DecisionTableMetaInfoReader reader
                && reader.getBoundNode().getDecisionTable() instanceof DecisionTable decision
                ? Optional.of(decision.getDtInfo() instanceof DTInfo info && info.isTransposed())
                : Optional.empty();
    }

    /**
     * A ColumnMatch table: its rows, read under the ids of its columns, and the algorithm its header names, which tells
     * how many of the rows give what the table returns or scores.
     */
    static Optional<Boolean> columnMatch(TableSyntaxNode node) {
        if (!(node.getMetaInfoReader() instanceof ColumnMatchMetaInfoReader reader)) {
            return Optional.empty();
        }
        var columnMatch = reader.getBoundNode().getColumnMatch();
        return columnMatch != null && columnMatch.getRows() != null && specialRowsOf(columnMatch.getAlgorithm()) >= 0
                ? UPRIGHT
                : Optional.empty();
    }

    /**
     * How many rows the algorithm of a ColumnMatch table reads before its conditions, as the compiler of the algorithm
     * counts them.
     *
     * @param algorithm the name of the algorithm the header names, or {@code null} for a header naming none
     * @return the count of the rows, or {@code -1} for an algorithm the compiler does not know
     */
    static int specialRowsOf(@Nullable IOpenSourceCodeModule algorithm) {
        var name = algorithm == null ? null : algorithm.getCode();
        try {
            return MatchAlgorithmFactory.getAlgorithm(name).getSpecialRowCount();
        } catch (IllegalArgumentException unknown) {
            return -1;
        }
    }

    /**
     * A Conditions, an Actions or a Returns table: the declarations the compiler read, by their titles. The axes of the
     * table are told by the titles, see {@link DecisionThemeLayout#conditions}.
     */
    static Optional<Boolean> declarations(TableSyntaxNode node) {
        return node.getMetaInfoReader() instanceof DtColumnsDefinitionMetaInfoReader reader
                && reader.getBoundNode().getTitles() != null
                ? UPRIGHT
                : Optional.empty();
    }

    /** A Properties table: a property in each row, read once the table is bound. */
    static Optional<Boolean> properties(TableSyntaxNode node) {
        return node.getMetaInfoReader() instanceof PropertyTableMetaInfoReader ? UPRIGHT : Optional.empty();
    }

    /** Whether the compiler read a table with its rows and columns swapped, or empty for one it read none of. */
    private static Optional<Boolean> turned(@Nullable ILogicalTable compiled) {
        return Optional.ofNullable(compiled).map(read -> !read.isNormalOrientation());
    }
}
