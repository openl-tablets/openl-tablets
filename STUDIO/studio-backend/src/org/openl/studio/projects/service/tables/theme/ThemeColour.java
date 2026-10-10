package org.openl.studio.projects.service.tables.theme;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;

/**
 * A colour a theme sets: of a font, of a fill or of a line.
 *
 * <p>A colour is one of the sixty colours the palette of Excel offers for the theme of a workbook: a theme colour of
 * Excel made lighter or darker, as the palette names it, such as {@code Blue, Accent 1, Lighter 60%}.
 *
 * <p>Written into a workbook of a theme, the colour is the theme colour itself, so the table takes the colours of the
 * theme of the workbook. Written into a workbook without a theme, such as an {@code .xls} one, it is the colour Office
 * 2013 - 2022 draws it in ({@link #rgb()}).
 *
 * @param themed the theme colour of Excel the colour is, made lighter or darker
 * @param rgb    the colour as Office 2013 - 2022 draws it, as {@code #rrggbb}: the colour a workbook without a theme is
 *               written in, and the colour a screen draws it in for such a workbook
 */
public record ThemeColour(ThemedColor themed, String rgb) {

    /** The tint the palette of Excel names after a theme colour: lighter or darker by a whole per cent. */
    private static final Pattern TINT = Pattern.compile("(?i)(lighter|darker) (\\d{1,3})%");

    /** The largest tint in per cent: white or black. */
    private static final int WHOLE = 100;

    /** How many thousandths of a tint a per cent is. */
    private static final int PER_MILLE = 10;

    /** How the palette of Excel names a theme colour. */
    private static final String EXCEL_NAMING = "A colour is written as the palette of Excel names a theme colour, "
            + "such as Blue, Accent 1, Lighter 60%: ";

    /**
     * A theme colour, drawn once as Office 2013 - 2022 draws it: a table theme reports it for every cell it reaches.
     *
     * @param themed the theme colour of Excel the colour is, made lighter or darker
     */
    public ThemeColour(ThemedColor themed) {
        this(themed, "#%06x".formatted(PoiExcelHelper.toRgbValue(themed.toOfficeRgb())));
    }

    /**
     * The colour a theme file writes as the palette of Excel names a theme colour: the theme colour, such as
     * {@code Accent 1}, then {@code Lighter} or {@code Darker} by a whole per cent from 1 to 100. The name Excel gives
     * the colour before it, such as {@code Blue}, may be written too; it is not checked.
     *
     * @param text the colour as the file writes it
     * @return the colour, or {@code null} for a text that names no theme colour
     * @throws IllegalArgumentException when the text names a theme colour another way than the palette of Excel does
     */
    static @Nullable ThemeColour read(String text) {
        var parts = Arrays.stream(text.split(",", -1)).map(String::strip).toList();
        var at = themeColourAt(parts);
        if (at < 0) {
            return null;
        }
        // The name Excel gives the colour may come before the theme colour, and the tint after it.
        if (at > 1 || parts.size() > at + 2) {
            throw new IllegalArgumentException(EXCEL_NAMING + text);
        }
        var colour = Objects.requireNonNull(ExcelThemeColour.named(parts.get(at)));
        var tint = at + 1 < parts.size() ? tintOf(parts.get(at + 1), text) : 0;
        return new ThemeColour(new ThemedColor(colour.index(), tint * PER_MILLE));
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
}
