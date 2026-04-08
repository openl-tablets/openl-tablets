package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.Schema;

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
 *   <li>Cell(0,0) = RawTableCell("Header", 2, 2, null) - origin cell spans 2x2
 *   <li>Cell(0,1) = RawTableCell(null, null, null, true) - covered by column span
 *   <li>Cell(1,0) = RawTableCell(null, null, null, true) - covered by row span
 *   <li>Cell(1,1) = RawTableCell(null, null, null, true) - covered by both spans
 * </ul>
 * <p>
 * Fields with null values are excluded from JSON response due to {@code @JsonInclude(NON_NULL)} annotation.
 * Clients can easily detect merged cells by checking: {@code colspan > 1 || rowspan > 1}
 *
 * @param value   Cell value (null if covered by another cell's span)
 * @param colspan Number of columns this cell spans (>= 2 means merging, null if single column or covered)
 * @param rowspan Number of rows this cell spans (>= 2 means merging, null if single row or covered)
 * @param covered Whether this cell is covered by another cell's span (true for masked cells, null otherwise)
 * @author Vladyslav Pikus
 */
public record RawTableCell(
        @Schema(description = "Value of the cell (null if covered by another cell's span)")
        Object value,

        @Schema(description = "Number of columns this cell spans (>=2 means merging, null if single column or covered)")
        Integer colspan,

        @Schema(description = "Number of rows this cell spans (>=2 means merging, null if single row or covered)")
        Integer rowspan,

        @Schema(description = "Whether this cell is covered by another cell's span (true for masked cells, null otherwise)")
        Boolean covered
) {

    public static final RawTableCell COVERED = new RawTableCell(null, null, null, true);

    public RawTableCell {
        if (Boolean.TRUE.equals(covered)) {
            value = null;
            colspan = null;
            rowspan = null;
        } else {
            colspan = (colspan != null && colspan > 1) ? colspan : null;
            rowspan = (rowspan != null && rowspan > 1) ? rowspan : null;
            covered = null;
        }
    }

    public RawTableCell(Object value) {
        this(value, null, null, null);
    }

    public RawTableCell(Object value, int colspan, int rowspan) {
        this(value, colspan, rowspan, null);
    }

}
