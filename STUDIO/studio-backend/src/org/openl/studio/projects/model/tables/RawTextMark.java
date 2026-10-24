package org.openl.studio.projects.model.tables;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A mark on a piece of a cell text: the piece is bold and in the colour of the mark.
 *
 * <p>The mark changes the text only. The cell keeps its style, and no cell style is added to the workbook. The mark
 * does not change how the cell is compiled.
 *
 * @param from  the index in the text of the cell of the first character of the piece
 * @param to    the index in the text of the cell after the last character of the piece
 * @param color the colour of the mark
 * @author Yury Molchan
 */
@Schema(description = "Mark on a piece of a cell text: the piece is bold and in the colour of the mark")
public record RawTextMark(
        @Parameter(description = "Index in the text of the cell of the first character of the piece.")
        @NotNull
        @Min(0)
        Integer from,

        @Parameter(description = "Index in the text of the cell after the last character of the piece.")
        @NotNull
        @Min(1)
        Integer to,

        @Parameter(description = "Colour of the mark.")
        @NotNull
        RawTextMarkColor color
) {
}
