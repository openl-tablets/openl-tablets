package org.openl.studio.projects.model.tables;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * How the cells of a table are written: the ways of entering a value the table needs, and which cell asks for which.
 *
 * <p>The ways of entering a value are listed once and pointed at by index, because a whole column of a table
 * usually asks for the same one. A cell that is not listed is written as plain text.
 *
 * <p>How much a table can say follows from its kind, and the kind is named in the answer. A table that can
 * only be read as the cells it holds answers as {@link RawTableEditorsView}; one that declares what a part of
 * it holds — the column of a Data or a Test table, the condition of a decision table, the cells a lookup's
 * rules meet in — answers as {@link DeclaredTableEditorsView}, which says it once for the whole part.
 *
 * @author Vladyslav Pikus
 */
@Getter
@SuperBuilder
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"kind", "editors", "cells", "areas"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RawTableEditorsView.class, name = RawTableEditorsView.KIND),
        @JsonSubTypes.Type(value = DeclaredTableEditorsView.class, name = DeclaredTableEditorsView.KIND)
})
@Schema(description = "The ways of entering a value a table's cells ask for")
public abstract class TableEditorsView {

    // A table whose cells all take plain text answers with an empty list rather than with nothing at all: the
    // screen reading this is told that it asked, and does not have to tell an empty answer from a missing one.
    @Builder.Default
    @Schema(description = "Ways of entering a value this table needs, pointed at by index from `cells` and `areas`")
    private final List<TableCellEditorView> editors = List.of();

    @Builder.Default
    @Schema(description = "Cells asking for an editor of their own; a cell that is absent takes the editor of "
            + "the part of the table it stands in, or plain text where no part holds it")
    private final List<Cell> cells = List.of();

    /** Which way the table answers: cell by cell, or by the parts it declares. */
    @Schema(description = "How the table answers: 'raw' as the cells it holds, 'declared' by the parts it declares",
            allowableValues = {RawTableEditorsView.KIND, DeclaredTableEditorsView.KIND})
    public abstract String getKind();

    /** The answer of a table that says nothing about how any of its cells is written. */
    public static TableEditorsView nothing() {
        return RawTableEditorsView.builder().build();
    }

    /**
     * One cell and the way it is written.
     *
     * @param row    0-based row of the cell in the table's matrix
     * @param column 0-based column of the cell in the table's matrix
     * @param editor index into {@code editors}
     */
    @Schema(name = "TableCellEditorRef", description = "A cell and the editor it asks for")
    public record Cell(
            @Schema(description = "0-based row of the cell in the matrix the raw read returns")
            Integer row,
            @Schema(description = "0-based column of the cell in the matrix the raw read returns")
            Integer column,
            @Schema(description = "0-based index into `editors`")
            Integer editor
    ) {
    }
}
