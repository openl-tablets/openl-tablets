package org.openl.studio.projects.model.tables;

import jakarta.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * Excel cell styling: background, font and alignment, as the workbook holds it or as a table theme draws the cell.
 *
 * <p>Every field is optional and absent when it matches the default, so a plain cell carries no style and
 * the whole object is dropped from the response. Present only when the raw table is requested with styles or with
 * a table theme, whose look names the theme as its source.
 *
 * @param background background colour as {@code #rrggbb}, absent when white (the default)
 * @param color     font colour as {@code #rrggbb}, absent when black (the default)
 * @param align     horizontal alignment, absent when left (the default)
 * @param valign    vertical alignment, absent when bottom (the default)
 * @param bold      {@code true} when the font is bold, absent otherwise
 * @param italic    {@code true} when the font is italic, absent otherwise
 * @param underline {@code true} when the font is underlined, absent otherwise
 * @param strikeout {@code true} when the font is struck out, absent otherwise
 * @param indent    left indent in Excel indent units, absent when zero
 * @param border    cell borders per side, absent when the cell has no borders
 * @param fontFamily name of the font; set by the table theme only, absent in a style read from the workbook
 * @param fontSize   size of the font in points; set by the table theme only, absent in a style read from the
 *                   workbook
 * @param source     where the style comes from: {@link RawTableStyleSource#THEME} for the look a table theme draws
 *                   the cell with, which a read naming the theme reports in place of the style of the workbook;
 *                   absent for the style the workbook holds (the default, {@link RawTableStyleSource#WORKBOOK})
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Excel cell style: background, font, alignment and borders")
public record RawTableCellStyle(
        @Parameter(description = "Background colour as #rrggbb; absent when white (the default)")
        String background,

        @Parameter(description = "Font colour as #rrggbb; absent when black (the default)")
        String color,

        @Parameter(description = "Horizontal alignment; absent for the default left alignment")
        RawTableHorizontalAlign align,

        @Parameter(description = "Vertical alignment; absent for the default bottom alignment")
        RawTableVerticalAlign valign,

        @Parameter(description = "true when the font is bold, absent otherwise")
        Boolean bold,

        @Parameter(description = "true when the font is italic, absent otherwise")
        Boolean italic,

        @Parameter(description = "true when the font is underlined, absent otherwise")
        Boolean underline,

        @Parameter(description = "true when the font is struck out, absent otherwise")
        @Nullable Boolean strikeout,

        @Parameter(description = "Left indent in Excel indent units, absent when zero")
        @Min(1)
        Integer indent,

        @Parameter(description = "Cell borders per side, absent when the cell has no borders")
        RawTableCellBorder border,

        @Parameter(description = "Name of the font; set by the table theme only")
        @Nullable String fontFamily,

        @Parameter(description = "Size of the font in points; set by the table theme only")
        @Nullable Integer fontSize,

        @Parameter(description = "Where the style comes from: theme for the look a table theme draws the cell with, "
                + "which a read naming the theme reports in place of the style of the workbook, a view only that no "
                + "edit writes; absent for the style the workbook holds (workbook, the default)")
        @Nullable RawTableStyleSource source
) {

    /** Whether every attribute is absent, so the cell carries no style at all. */
    @JsonIgnore
    public boolean isEmpty() {
        return background == null
                && color == null
                && align == null
                && valign == null
                && bold == null
                && italic == null
                && underline == null
                && strikeout == null
                && indent == null
                && border == null
                && fontFamily == null
                && fontSize == null
                && source == null;
    }
}
