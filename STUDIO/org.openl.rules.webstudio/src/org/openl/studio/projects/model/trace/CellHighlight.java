package org.openl.studio.projects.model.trace;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One highlighted cell of a traced table, addressed in A1 notation so the client can overlay it onto the
 * raw table grid returned by the Tables API.
 *
 * @param cell  cell address in A1 notation (for example {@code C7}), matching the raw table's addresses
 * @param state how the cell should be highlighted
 */
@Schema(description = "trace.type.cell-highlight.desc")
public record CellHighlight(
        @Schema(description = "trace.field.cell-highlight.cell.desc")
        String cell,

        @Schema(description = "trace.field.cell-highlight.state.desc")
        HighlightState state) {
}
