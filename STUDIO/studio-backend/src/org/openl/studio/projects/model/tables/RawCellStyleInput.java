package org.openl.studio.projects.model.tables;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * The styling to set on cells: background, font and alignment.
 *
 * <p>Only the attributes named here are set. An attribute left out is not touched, so a cell keeps the styling it
 * already carries — the way a toolbar button changes one thing about a cell and nothing else.
 *
 * <p>The attributes are the ones the raw read reports back, except the borders and the vertical alignment, which
 * are read-only. A colour is one of the palette of Excel: a theme colour, made lighter or darker, or a standard colour.
 *
 * @param background background colour
 * @param color      font colour
 * @param align      horizontal alignment; {@code left} puts a cell back to the default
 * @param bold       whether the font is bold
 * @param italic     whether the font is italic
 * @param underline  whether the font is underlined
 * @param indent     left indent in Excel indent units; {@code 0} takes the indent away
 * @author Vladyslav Pikus
 */
@Schema(description = """
        Styling to set on the cells: background, font and alignment. An attribute that is left out is not \
        touched, so the cells keep the styling they already carry.""")
public record RawCellStyleInput(
        @Parameter(description = "Background colour: a theme colour of Excel, made lighter or darker, or a standard colour.")
        @Valid
        @Nullable RawCellColor background,

        @Parameter(description = "Font colour: a theme colour of Excel, made lighter or darker, or a standard colour.")
        @Valid
        @Nullable RawCellColor color,

        @Parameter(description = "Horizontal alignment; `left` puts the cells back to the default alignment.")
        @Nullable RawTableHorizontalAlign align,

        @Parameter(description = "Whether the font is bold.")
        @Nullable Boolean bold,

        @Parameter(description = "Whether the font is italic.")
        @Nullable Boolean italic,

        @Parameter(description = "Whether the font is underlined.")
        @Nullable Boolean underline,

        @Parameter(description = "Left indent in Excel indent units; 0 takes the indent away.")
        @Min(0)
        @Max(15)
        @Nullable Integer indent
) {

    /** Whether the request names nothing but the indent, the one attribute that is structure rather than look. */
    @JsonIgnore
    public boolean isIndentOnly() {
        return indent != null
                && background == null
                && color == null
                && align == null
                && bold == null
                && italic == null
                && underline == null;
    }

    /** Whether the request names no attribute at all, in which case there is nothing to set. */
    @JsonIgnore
    public boolean isEmpty() {
        return background == null
                && color == null
                && align == null
                && bold == null
                && italic == null
                && underline == null
                && indent == null;
    }
}
