package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How the cells of a table are written: the ways of entering a value the table needs, and which cell asks for which.
 *
 * <p>The ways of entering a value are listed once and pointed at by index, because a whole column of a table
 * usually asks for the same one. A cell that is not listed is written as plain text.
 *
 * @param editors the ways of entering a value this table needs, pointed at by {@code cells}
 * @param cells   the cells that ask for one of them
 * @author Vladyslav Pikus
 */
@Schema(description = "The ways of entering a value a table's cells ask for")
public record TableEditorsView(
        @Schema(description = "Ways of entering a value this table needs, pointed at by index from `cells`")
        List<TableCellEditorView> editors,

        @Schema(description = "Cells that ask for one of the editors; a cell that is absent is written as text")
        List<Cell> cells
) {

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
