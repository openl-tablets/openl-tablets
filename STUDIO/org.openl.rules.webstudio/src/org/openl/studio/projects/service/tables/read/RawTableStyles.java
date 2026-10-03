package org.openl.studio.projects.service.tables.read;

import java.util.HexFormat;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.tableeditor.model.ui.BorderStyle;
import org.openl.studio.projects.model.tables.RawTableBorderLineStyle;
import org.openl.studio.projects.model.tables.RawTableCellBorderSide;
import org.openl.studio.projects.model.tables.RawTableCellStyle;

/**
 * Turns the colours, fonts and border lines of a cell into the style the Tables API reports.
 *
 * <p>A style read from the workbook and the look a table theme draws over it are both reported through here, so a
 * theme is drawn on the screen the way the workbook it is written into is drawn.
 *
 * <p>An attribute at its default is left out: a white fill, a black font or line, and a font flag that is off.
 */
public final class RawTableStyles {

    /** The fill of a cell nothing fills. */
    public static final String WHITE = "#ffffff";

    /** The colour of a text or a line that names none. */
    public static final String BLACK = "#000000";

    /** Writes a colour in lower case, as the API reports it. */
    private static final HexFormat HEX = HexFormat.of();

    private RawTableStyles() {
    }

    /**
     * A colour in the form {@code #rrggbb}.
     *
     * @param rgb        the red, green and blue of the colour, or {@code null} for none
     * @param defaultHex the colour reported as no colour at all
     * @return the colour, or {@code null} when it is missing or is the default colour
     */
    public static @Nullable String hex(short @Nullable [] rgb, String defaultHex) {
        if (rgb == null || rgb.length < 3) {
            return null;
        }
        // Each component is written as the byte it holds, so a negative short never takes more than two digits.
        var hex = "#" + HEX.formatHex(new byte[]{(byte) rgb[0], (byte) rgb[1], (byte) rgb[2]});
        return hex.equals(defaultHex) ? null : hex;
    }

    /**
     * Lays the attributes of a font into a style.
     *
     * @param style the style to fill
     * @param font  the font
     * @return the style, for the attributes that follow
     */
    static RawTableCellStyle.RawTableCellStyleBuilder font(RawTableCellStyle.RawTableCellStyleBuilder style,
                                                           ICellFont font) {
        return style.color(hex(font.getFontColor(), BLACK))
                .bold(flag(font.isBold()))
                .italic(flag(font.isItalic()))
                .underline(flag(font.isUnderlined()))
                .strikeout(flag(font.isStrikeout()));
    }

    /**
     * One side of a cell border.
     *
     * @param border the line the side is drawn with, or {@code null} for none
     * @return the side, or {@code null} when the side has no border
     */
    public static @Nullable RawTableCellBorderSide borderSide(@Nullable BorderStyle border) {
        if (border == null || border == BorderStyle.NONE || border.getWidth() == 0) {
            return null;
        }
        var style = switch (border.getStyle() == null ? "solid" : border.getStyle()) {
            case "dashed" -> RawTableBorderLineStyle.DASHED;
            case "dotted" -> RawTableBorderLineStyle.DOTTED;
            case "double" -> RawTableBorderLineStyle.DOUBLE;
            default -> RawTableBorderLineStyle.SOLID;
        };
        return RawTableCellBorderSide.builder()
                .style(style)
                .width(border.getWidth())
                .color(hex(border.getRgb(), BLACK))
                .build();
    }

    /** {@link Boolean#TRUE} for a flag that is on, {@code null} for one that is off. */
    private static @Nullable Boolean flag(boolean on) {
        return on ? Boolean.TRUE : null;
    }
}
