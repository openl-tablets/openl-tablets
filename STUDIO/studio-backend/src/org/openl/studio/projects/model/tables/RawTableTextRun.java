package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A piece of a cell text formatted with a font of its own.
 *
 * <p>A run names where it ends in the text of its cell: it starts where the run before it ends, the first one at the
 * start of the text, and the runs of a cell together cover its text. The text itself is not repeated.
 *
 * <p>A run with a style draws its text with that style alone: an attribute absent from it is at its default, not
 * taken from the cell. A run without a style takes the font of the cell.
 *
 * @param to    the index in the text of the cell after the last character of the run
 * @param style the font of the run, as an index into the styles of the read, or {@code null} when the run takes the
 *              font of the cell
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A piece of a cell text formatted with a font of its own")
public record RawTableTextRun(
        @Parameter(description = """
                Index in the text of the cell after the last character of the run. A run starts where the run \
                before it ends, the first one at the start of the text.""")
        int to,

        @Parameter(description = """
                Font of the run, as an index into the styles of the read: colour, bold, italic, underline and \
                strikeout. An attribute absent from it is at its default. Absent when the run takes the font of the \
                cell.""")
        @Nullable Integer style
) {
}
