package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Excel cell styling read from the workbook: background, font and alignment.
 *
 * <p>Every field is optional and absent when it matches the default, so a plain cell carries no style and
 * the whole object is dropped from the response. Present only when the raw table is requested with styles.
 *
 * @param background background colour as {@code #rrggbb}, absent when white (the default)
 * @param color     font colour as {@code #rrggbb}, absent when black (the default)
 * @param align     horizontal alignment: {@code left}, {@code center} or {@code right}
 * @param valign    vertical alignment: {@code top}, {@code middle} or {@code bottom}
 * @param bold      {@code true} when the font is bold, absent otherwise
 * @param italic    {@code true} when the font is italic, absent otherwise
 * @param underline {@code true} when the font is underlined, absent otherwise
 * @param indent    left indent in Excel indent units, absent when zero
 * @param border    cell borders per side, absent when the cell has no borders
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Excel cell style: background, font, alignment and borders")
public record RawTableCellStyle(
        String background,
        String color,
        String align,
        String valign,
        Boolean bold,
        Boolean italic,
        Boolean underline,
        Integer indent,
        RawTableCellBorder border
) {
}
