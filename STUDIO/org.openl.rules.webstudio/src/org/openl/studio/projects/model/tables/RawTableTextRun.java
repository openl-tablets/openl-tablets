package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * A piece of a cell text formatted with a font of its own.
 *
 * <p>The texts of the runs of a cell, put together, give its text. A run with a style draws its text with
 * that style alone: an attribute absent from it is at its default, not taken from the cell. A run without a
 * style takes the font of the cell.
 *
 * <p>The font a table theme gives a run names the theme as its source, as the style of a cell does.
 *
 * @param text  the text of the run
 * @param style the font of the run, or {@code null} when the run takes the font of the cell
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A piece of a cell text formatted with a font of its own")
public record RawTableTextRun(
        @Parameter(description = "Text of the run")
        String text,

        @Parameter(description = """
                Font of the run: colour, bold, italic, underline and strikeout. An attribute absent from it is at \
                its default. Absent when the run takes the font of the cell. The font a table theme gives the run \
                names the theme as its source.""")
        @Nullable RawTableCellStyle style
) {
}
