package org.openl.studio.compare.model;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import org.openl.studio.projects.model.tables.RawTableCell;

/**
 * One side of a compared table: the table as it stands in that file, and the cells of it that read
 * differently in the other file.
 *
 * @param source       the table as a matrix of cells, with its merges and its Excel styling
 * @param changedCells addresses of the cells that differ, in A1 notation
 */
@Builder
public record ComparisonSideView(

        @Schema(description = "The table as a matrix of cells, with its merges and its Excel styling")
        List<List<RawTableCell>> source,

        @Schema(description = """
                Addresses of the cells that read differently in the other file, in A1 notation. A row \
                that the other file does not have at all is reported cell by cell.""")
        List<String> changedCells) {
}
