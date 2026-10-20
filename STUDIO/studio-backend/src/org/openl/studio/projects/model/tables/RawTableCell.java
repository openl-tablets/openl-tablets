package org.openl.studio.projects.model.tables;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

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
 * @param value   Cell value as a scalar or one-dimensional scalar array. Null if covered by another cell's span.
 * @param formula Formula the cell was written with, as Excel writes it; null when the cell holds a plain value.
 *                A cell carries both, so a screen can show either without asking again.
 * @param colspan Number of columns this cell spans (>= 2 means merging, null if single column or covered)
 * @param rowspan Number of rows this cell spans (>= 2 means merging, null if single row or covered)
 * @param covered Whether this cell is covered by another cell's span (true for masked cells, null otherwise)
 * @param comment note a reader left on the cell in Excel, if any
 * @param style   Excel styling of the cell, as an index into the styles of the read, when the read asked for the
 *                styles. Absent for a cell in no style at all
 * @param metaInfo What the compiler knows about the cell, when the read asked for it
 * @param runs    Pieces of the cell text formatted with fonts of their own, each named by where it ends in the text,
 *                when the read asked for the styles and the text does not take the font of the cell
 * @param type    The type of the value of the cell, as the compiler reads it, when it differs from the type of the
 *                region of the cell; read with the styles
 * @param mergedArea The whole area of a merged cell the edge of a window cuts, in A1 notation, when the read asks for
 *                the exact rows; the cell then holds the part of it inside the window and the value of the merged cell
 * @author Vladyslav Pikus
 */
@Builder(toBuilder = true)
public record RawTableCell(
        @Schema(description = """
                Read-only cell address in A1 notation (e.g. 'B3'); absent for covered cells. \
                Matches the cell address reported by compilation messages.""")
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        String cell,

        @Parameter(description = """
                Value of the cell: a string, a number, a boolean or a one-dimensional array of these values. \
                Null if covered by another cell's span.""")
        @Schema(oneOf = {NullableCellValueSchema.class, Number.class, Boolean.class, CellValueArraySchema.class})
        @CellValueConstraint
        @Nullable Object value,

        @Schema(description = """
                Formula the cell was written with, as Excel writes it (for example '=B2*C2'); absent when the \
                cell holds a plain value. The cell carries it beside the value the formula computed, so a \
                screen showing formulas needs no second read.""")
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Nullable String formula,

        @Schema(description = """
                Note a reader left on the cell in Excel; absent when the cell carries none. A screen marks such \
                a cell and shows the note when the reader looks at it.""")
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Nullable String comment,

        @Schema(description = "Number of columns this cell spans (>=2 means merging, null if single column or covered)")
        Integer colspan,

        @Schema(description = "Number of rows this cell spans (>=2 means merging, null if single row or covered)")
        Integer rowspan,

        @Schema(description = "Whether this cell is covered by another cell's span (true for masked cells, null otherwise)")
        Boolean covered,

        @Schema(description = """
                Excel style of the cell, as an index into the styles of the read, read when the read asks for the \
                styles. Absent for a cell in no style at all.""")
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Nullable Integer style,

        @Schema(description = """
                What the compiler knows about the cell: the pieces of its text that refer to something, the \
                type it holds, whether a decision table returns it, and the editor it asks for. Read only \
                when the read asks for it, and absent when the compiler has nothing to say.""")
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Nullable RawTableCellMetaInfo metaInfo,

        @Parameter(description = """
                Pieces of the cell text formatted with fonts of their own, such as a header with a grey keyword \
                and a bold name, each named by where it ends in the text. Read with the styles, and absent when \
                the text takes the font of the cell.""")
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Nullable List<RawTableTextRun> runs,

        @Parameter(description = """
                The type of the value of the cell, as the compiler reads it, when it differs from the type of the \
                region of the cell. Read with the styles.""")
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Nullable RawTableValueType type,

        @Schema(description = """
                The whole area of a merged cell the edge of the window cuts, in A1 notation (for example 'B4:B299'), \
                when the read asks for the exact rows. The cell holds the part of the merged cell inside the window, \
                and the value of the merged cell.""")
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Nullable String mergedArea
) {

    public static final RawTableCell COVERED_CELL = RawTableCell.builder().covered(true).build();

    public RawTableCell {
        if (Boolean.TRUE.equals(covered)) {
            cell = null;
            value = null;
            formula = null;
            colspan = null;
            rowspan = null;
            style = null;
            metaInfo = null;
            runs = null;
            type = null;
            mergedArea = null;
        } else {
            colspan = (colspan != null && colspan > 1) ? colspan : null;
            rowspan = (rowspan != null && rowspan > 1) ? rowspan : null;
            covered = null;
        }
    }

}
