package org.openl.studio.projects.model.tables;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

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
 * are read-only.
 *
 * <p>A colour is given either as {@code #rrggbb} or as a theme colour of Excel, not both. A theme colour is written
 * into an {@code .xlsx} workbook of a theme as the theme colour itself, so the cell takes the colours of the theme of
 * the workbook. A workbook without a theme, such as an {@code .xls} one, takes the colour Office draws it in.
 *
 * @param background      background colour as {@code #rrggbb}
 * @param backgroundTheme background colour as a theme colour of Excel, made lighter or darker
 * @param color           font colour as {@code #rrggbb}
 * @param colorTheme      font colour as a theme colour of Excel, made lighter or darker
 * @param align           horizontal alignment; {@code left} puts a cell back to the default
 * @param bold            whether the font is bold
 * @param italic          whether the font is italic
 * @param underline       whether the font is underlined
 * @param indent          left indent in Excel indent units; {@code 0} takes the indent away
 * @author Vladyslav Pikus
 */
@Schema(description = """
        Styling to set on the cells: background, font and alignment. An attribute that is left out is not \
        touched, so the cells keep the styling they already carry.""")
public record RawCellStyleInput(
        @Parameter(description = "Background colour as #rrggbb.")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
        @Nullable String background,

        @Parameter(description = "Background colour as a theme colour of Excel, made lighter or darker; not given "
                + "together with background.")
        @Valid
        @Nullable RawTableThemeColor backgroundTheme,

        @Parameter(description = "Font colour as #rrggbb.")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
        @Nullable String color,

        @Parameter(description = "Font colour as a theme colour of Excel, made lighter or darker; not given together "
                + "with color.")
        @Valid
        @Nullable RawTableThemeColor colorTheme,

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

    /** Whether the request names no attribute at all, in which case there is nothing to set. */
    @JsonIgnore
    public boolean isEmpty() {
        return background == null
                && backgroundTheme == null
                && color == null
                && colorTheme == null
                && align == null
                && bold == null
                && italic == null
                && underline == null
                && indent == null;
    }

    /** Whether the request names a colour twice: both as {@code #rrggbb} and as a theme colour of Excel. */
    @JsonIgnore
    public boolean isColourTwice() {
        return (background != null && backgroundTheme != null) || (color != null && colorTheme != null);
    }
}
