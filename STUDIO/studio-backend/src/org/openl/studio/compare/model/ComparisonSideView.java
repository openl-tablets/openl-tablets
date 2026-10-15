package org.openl.studio.compare.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellStyle;

/**
 * One side of a compared table: the table as it stands in that file, and the cells of it that read
 * differently in the other file.
 *
 * @param source       the table as a matrix of cells, with its merges and its Excel styling
 * @param styles       the Excel styles the cells name by index, each once
 * @param changedCells addresses of the cells that differ, in A1 notation
 */
@Builder
public record ComparisonSideView(

        @Schema(description = "The table as a matrix of cells, with its merges and its Excel styling")
        List<List<RawTableCell>> source,

        @Parameter(description = """
                The Excel styles the cells of the side use, each once: a cell and a piece of its text name their \
                style by its index in this list.""")
        List<RawTableCellStyle> styles,

        @Schema(description = """
                Addresses of the cells that read differently in the other file, in A1 notation. A row \
                that the other file does not have at all is reported cell by cell.""")
        List<String> changedCells) {
}
