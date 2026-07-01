package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Represents a single cell in raw table format with explicit span information.
 * <p>
 * Cells can be in one of two states:
 * <ul>
 * <li><b>Origin Cell</b> (top-left of merged region):
 *   <ul>
 *     <li>{@code value} - contains the actual cell content
 *     <li>{@code colspan} - number of columns this cell spans (>= 1, null if colspan == 1)
 *     <li>{@code rowspan} - number of rows this cell spans (>= 1, null if rowspan == 1)
 *     <li>{@code covered} - null
 *   </ul>
 * <li><b>Covered Cell</b> (part of merged region but not the origin):
 *   <ul>
 *     <li>{@code value} - null (the actual value is in the origin cell)
 *     <li>{@code colspan} - null (indicates this cell is covered)
 *     <li>{@code rowspan} - null (indicates this cell is covered)
 *     <li>{@code covered} - true (indicates this is a covered/masked cell)
 *   </ul>
 * </ul>
 * <p>
 * Example for a 2x2 merged cell at position (0,0):
 * <ul>
 *   <li>Cell(0,0) = origin cell spans 2x2 ({@code colspan=2, rowspan=2})
 *   <li>Cell(0,1) = covered by column span ({@code covered=true})
 *   <li>Cell(1,0) = covered by row span ({@code covered=true})
 *   <li>Cell(1,1) = covered by both spans ({@code covered=true})
 * </ul>
 * <p>
 * Fields with null values are excluded from JSON response due to {@code @JsonInclude(NON_NULL)} annotation.
 * Clients can easily detect merged cells by checking: {@code colspan > 1 || rowspan > 1}
 *
 * @param cell    Read-only cell address in A1 notation (e.g. {@code B3}); null for covered cells. The value
 *                matches the {@code cell} address reported by compilation messages, so clients can correlate a
 *                message with the exact cell in the matrix.
 * @param value   Cell value (null if covered by another cell's span)
 * @param colspan Number of columns this cell spans (>= 2 means merging, null if single column or covered)
 * @param rowspan Number of rows this cell spans (>= 2 means merging, null if single row or covered)
 * @param covered Whether this cell is covered by another cell's span (true for masked cells, null otherwise)
 * @author Vladyslav Pikus
 */
@Builder
public record RawTableCell(
        @Schema(description = "Read-only cell address in A1 notation (e.g. 'B3'); absent for covered cells. "
                + "Matches the cell address reported by compilation messages.")
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        String cell,

        @Schema(description = "Value of the cell (null if covered by another cell's span)")
        @CellValueConstraint
        Object value,

        @Schema(description = "Number of columns this cell spans (>=2 means merging, null if single column or covered)")
        Integer colspan,

        @Schema(description = "Number of rows this cell spans (>=2 means merging, null if single row or covered)")
        Integer rowspan,

        @Schema(description = "Whether this cell is covered by another cell's span (true for masked cells, null otherwise)")
        Boolean covered,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        RawTableCellStyle style
) {

    public static final RawTableCell COVERED_CELL = RawTableCell.builder().covered(true).build();

    public RawTableCell {
        if (Boolean.TRUE.equals(covered)) {
            cell = null;
            value = null;
            colspan = null;
            rowspan = null;
            style = null;
        } else {
            colspan = (colspan != null && colspan > 1) ? colspan : null;
            rowspan = (rowspan != null && rowspan > 1) ? rowspan : null;
            covered = null;
        }
    }

}
