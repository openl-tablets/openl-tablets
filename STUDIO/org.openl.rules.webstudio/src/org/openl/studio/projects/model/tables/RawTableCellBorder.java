package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Cell borders, one entry per side. A side is absent when the cell has no explicit border there, so a cell
 * without borders carries none.
 *
 * @param top    top border, absent when none
 * @param right  right border, absent when none
 * @param bottom bottom border, absent when none
 * @param left   left border, absent when none
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cell borders per side; a side is absent when the cell has no border there")
public record RawTableCellBorder(
        RawTableCellBorderSide top,
        RawTableCellBorderSide right,
        RawTableCellBorderSide bottom,
        RawTableCellBorderSide left
) {
}
