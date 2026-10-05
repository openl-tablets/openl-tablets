package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * One side of a cell border read from the workbook.
 *
 * @param style    line style
 * @param width    line width in pixels
 * @param color    line colour as {@code #rrggbb}, absent when black (the default)
 * @param colorKey the key of the table theme file the line colour is set at, such as
 *                 {@code spreadsheet.resultRow.border.top.color}, which names the side the file sets, whichever side
 *                 the line is drawn on; set by the table theme only, so a screen can draw the colour of the key its
 *                 own way
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "One side of a cell border: line style, width and colour")
public record RawTableCellBorderSide(
        @Parameter(description = "Line style")
        RawTableBorderLineStyle style,

        @Parameter(description = "Line width in pixels")
        Integer width,

        @Parameter(description = "Line colour as #rrggbb; absent when black (the default)")
        @Nullable String color,

        @Parameter(description = "Key of the table theme file the line colour is set at, such as "
                + "spreadsheet.resultRow.border.top.color; set by the table theme only")
        @Nullable String colorKey
) {
}
