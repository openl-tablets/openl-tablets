package org.openl.studio.projects.service.tables.theme;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.With;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;

/**
 * A colour a theme sets: of a font, of a fill or of a line.
 *
 * <p>A colour is one of its own, written as {@code #rrggbb}, or a theme colour of Excel made lighter or darker, as the
 * palette of Excel names it: {@code Blue, Accent 1, Lighter 60%}. The screen draws either as {@code #rrggbb}.
 *
 * <p>Written into a workbook whose theme colours are those the theme makes its colours of, the colour is the theme
 * colour itself, so Excel offers it in its palette and the colour changes with the theme of the workbook. Written into
 * any other workbook, it is {@code #rrggbb}.
 *
 * <p>A colour the theme file sets keeps the key it is set at, such as {@code spreadsheet.values.background}: the kind
 * or the base, the part and the attribute, as the file reads with its aliases and merge keys resolved. A screen can
 * draw the colour of a key its own way, such as a dark one in a dark appearance. The workbook is always written the
 * colour itself.
 *
 * <p>The key is no part of the colour: two colours are equal when they are drawn and written alike, wherever the theme
 * sets them, so parts that look alike take one style in a workbook and draw one line on the screen.
 *
 * @param rgb    the colour as {@code #rrggbb}, as the screen draws it
 * @param themed the theme colour of Excel the colour is, made lighter or darker, or {@code null} for a colour of its
 *               own
 * @param key    the key of the theme file the colour is set at, or {@code null} for a colour no theme file sets
 */
public record ThemeColour(String rgb, @Nullable ThemedColor themed, @With(AccessLevel.PACKAGE) @Nullable String key) {

    private static final Pattern RRGGBB = Pattern.compile("#[0-9a-fA-F]{6}");

    /** What starts a colour written as {@code #rrggbb}. */
    private static final String RGB_START = "#";

    /** The tint the palette of Excel names after a theme colour: lighter or darker by a whole per cent. */
    private static final Pattern TINT = Pattern.compile("(?i)(lighter|darker) (\\d{1,3})%");

    /** The largest tint in per cent: white or black. */
    private static final int WHOLE = 100;

    /** How the palette of Excel names a theme colour. */
    private static final String EXCEL_NAMING = "A theme colour of Excel is written as the palette of Excel names it, "
            + "such as Blue, Accent 1, Lighter 60%: ";

    /**
     * A colour, drawn as {@code #rrggbb}.
     *
     * @throws IllegalArgumentException when the colour is drawn as written another way
     */
    public ThemeColour {
        requireRgb(rgb);
    }

    /**
     * A colour no theme file sets at a key.
     *
     * @param rgb    the colour as {@code #rrggbb}
     * @param themed the theme colour of Excel the colour is, made lighter or darker, or {@code null} for a colour of
     *               its own
     * @throws IllegalArgumentException when the colour is drawn as written another way
     */
    ThemeColour(String rgb, @Nullable ThemedColor themed) {
        this(rgb, themed, null);
    }

    /**
     * A colour of its own.
     *
     * @param rgb the colour as {@code #rrggbb}
     * @return the colour
     * @throws IllegalArgumentException when the colour is written another way
     */
    static ThemeColour of(String rgb) {
        return new ThemeColour(rgb, null);
    }

    /** Whether another colour is drawn and written as this one, wherever a theme sets either. */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof ThemeColour colour && rgb.equals(colour.rgb) && Objects.equals(themed, colour.themed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rgb, themed);
    }

    /**
     * The colour a theme file writes as {@code #rrggbb}, or as the palette of Excel names a theme colour: the theme
     * colour, such as {@code Accent 1}, then {@code Lighter} or {@code Darker} by a whole per cent from 1 to 100. The
     * name Excel gives the colour before it, such as {@code Blue}, may be written too; it is not checked.
     *
     * @param text         the colour as the file writes it
     * @param themeColours the theme colours of Excel the theme makes its colours of, or {@code null} for none
     * @return the colour, or {@code null} for a text that writes a colour neither way
     * @throws IllegalArgumentException when the text writes a colour of its own another way than {@code #rrggbb},
     *                                  names a theme colour another way than the palette of Excel does, or names one
     *                                  of a theme without theme colours
     */
    static @Nullable ThemeColour read(String text, @Nullable ExcelThemeColours themeColours) {
        if (text.startsWith(RGB_START)) {
            return of(text);
        }
        var parts = Arrays.stream(text.split(",", -1)).map(String::strip).toList();
        var at = themeColourAt(parts);
        if (at < 0) {
            return null;
        }
        // The name Excel gives the colour may come before the theme colour, and the tint after it.
        if (at > 1 || parts.size() > at + 2) {
            throw new IllegalArgumentException(EXCEL_NAMING + text);
        }
        if (themeColours == null) {
            throw new IllegalArgumentException(
                    "A theme colour of Excel is made of the theme colours the theme writes under themeColors: " + text);
        }
        var colour = Objects.requireNonNull(ExcelThemeColour.named(parts.get(at)));
        var tint = at + 1 < parts.size() ? tintOf(parts.get(at + 1), text) : 0;
        return themeColours.colourOf(colour, tint);
    }

    /** Where the part naming a theme colour is, or {@code -1} when no part names one. */
    private static int themeColourAt(List<String> parts) {
        return IntStream.range(0, parts.size())
                .filter(at -> ExcelThemeColour.named(parts.get(at)) != null)
                .findFirst()
                .orElse(-1);
    }

    /** The tint the palette of Excel names, in per cent: above 0 lighter, below 0 darker. */
    private static int tintOf(String written, String text) {
        var tint = TINT.matcher(written);
        var percent = tint.matches() ? Integer.parseInt(tint.group(2)) : 0;
        if (percent < 1 || percent > WHOLE) {
            throw new IllegalArgumentException(EXCEL_NAMING + text);
        }
        return "lighter".equalsIgnoreCase(tint.group(1)) ? percent : -percent;
    }

    /**
     * Refuses a colour written any other way than {@code #rrggbb}, which is how the workbook and the screen both take
     * it: a shorter or a named colour would draw one colour and write another.
     *
     * @param colour the colour
     * @throws IllegalArgumentException when the colour is written another way
     */
    static void requireRgb(String colour) {
        if (!RRGGBB.matcher(colour).matches()) {
            throw new IllegalArgumentException("A colour is written as #rrggbb: " + colour);
        }
    }
}
