package org.openl.studio.projects.service.tables.theme;

import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.With;
import org.jspecify.annotations.Nullable;

/**
 * The look a theme gives a cell, or a piece of its text.
 *
 * <p>Every attribute is optional. An attribute the theme leaves out keeps what the cell has in the workbook. A
 * cell therefore keeps its number format, its wrapping and any attribute the theme says nothing about.
 *
 * @param fontFamily the name of the font
 * @param fontSize   the size of the font in points
 * @param bold       whether the font is bold
 * @param italic     whether the font is italic
 * @param underline  whether the font is underlined
 * @param strikeout  whether the font is struck out
 * @param color      the colour of the font as {@code #rrggbb}
 * @param background the colour the cell is filled with as {@code #rrggbb}
 * @param align      how the text lines up across the cell
 * @param valign     how the text lines up from top to bottom
 * @param border     the borders of the cell
 */
@Builder
@With(AccessLevel.PACKAGE)
public record ThemeStyle(@Nullable String fontFamily,
                         @Nullable Integer fontSize,
                         @Nullable Boolean bold,
                         @Nullable Boolean italic,
                         @Nullable Boolean underline,
                         @Nullable Boolean strikeout,
                         @Nullable String color,
                         @Nullable String background,
                         @Nullable ThemeHorizontalAlign align,
                         @Nullable ThemeVerticalAlign valign,
                         @Nullable ThemeBorder border) {

    private static final Pattern COLOUR = Pattern.compile("#[0-9a-fA-F]{6}");

    /** The smallest and the largest font Excel writes, in points. */
    private static final int SMALLEST_FONT = 1;
    private static final int LARGEST_FONT = 409;

    /**
     * A look, its colours written as {@code #rrggbb} and its font sized as Excel sizes one.
     *
     * @throws IllegalArgumentException when a colour is written another way, or the font is of a size Excel has not
     */
    public ThemeStyle {
        requireColour(color);
        requireColour(background);
        requireFontSize(fontSize);
    }

    /** A look that changes nothing. */
    static final ThemeStyle NONE = ThemeStyle.builder().build();

    /**
     * Lays another look over this one: an attribute the other names wins, and borders are laid side by side.
     *
     * @param over the look laid on top, or {@code null} for none
     * @return the look both give together
     */
    ThemeStyle with(@Nullable ThemeStyle over) {
        if (over == null) {
            return this;
        }
        return ThemeStyle.builder()
                .fontFamily(pick(over.fontFamily, fontFamily))
                .fontSize(pick(over.fontSize, fontSize))
                .bold(pick(over.bold, bold))
                .italic(pick(over.italic, italic))
                .underline(pick(over.underline, underline))
                .strikeout(pick(over.strikeout, strikeout))
                .color(pick(over.color, color))
                .background(pick(over.background, background))
                .align(pick(over.align, align))
                .valign(pick(over.valign, valign))
                .border(border == null ? over.border : border.with(over.border))
                .build();
    }

    /**
     * This look for a cell of a block it goes round: its lines above and below where the cell reaches the edge of the
     * block, and the lines of the look inside the block elsewhere.
     *
     * @param inside the look of a cell within the block
     * @param first  whether the cell reaches the first line of the block
     * @param last   whether the cell reaches the last line of the block
     * @return the look the cell is drawn with
     */
    ThemeStyle atEdges(ThemeStyle inside, boolean first, boolean last) {
        var around = border == null ? ThemeBorder.KEEP : border;
        return withBorder(around.atEdges(inside.border == null ? ThemeBorder.KEEP : inside.border, first, last));
    }

    /** Whether the look sets anything about the font. */
    boolean hasFont() {
        return fontFamily != null || fontSize != null || bold != null || italic != null || underline != null
                || strikeout != null || color != null;
    }

    /**
     * Refuses a colour written any other way than {@code #rrggbb}, which is how the workbook and the screen both take
     * it: a shorter or a named colour would draw one colour and write another.
     *
     * @param colour the colour, or {@code null} for none
     * @throws IllegalArgumentException when the colour is written another way
     */
    static void requireColour(@Nullable String colour) {
        if (colour != null && !COLOUR.matcher(colour).matches()) {
            throw new IllegalArgumentException("A colour is written as #rrggbb: " + colour);
        }
    }

    /**
     * Refuses a font of a size Excel has not: it sizes a font from 1 to 409 points, and a workbook written with
     * another size is one Excel refuses to open.
     *
     * @param size the size in points, or {@code null} for none
     * @throws IllegalArgumentException when Excel has no font of the size
     */
    private static void requireFontSize(@Nullable Integer size) {
        if (size != null && (size < SMALLEST_FONT || size > LARGEST_FONT)) {
            throw new IllegalArgumentException("A font size is a whole number of points from 1 to 409: " + size);
        }
    }

    private static <T> @Nullable T pick(@Nullable T over, @Nullable T under) {
        return over != null ? over : under;
    }
}
