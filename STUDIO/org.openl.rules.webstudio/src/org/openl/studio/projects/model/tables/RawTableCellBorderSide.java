package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * One side of a cell border read from the workbook.
 *
 * @param style line style
 * @param width line width in pixels
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "One side of a cell border: line style and width")
public record RawTableCellBorderSide(
        RawTableBorderLineStyle style,
        Integer width
) {
}
