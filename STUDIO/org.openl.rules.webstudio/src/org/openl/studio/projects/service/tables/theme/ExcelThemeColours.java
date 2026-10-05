package org.openl.studio.projects.service.tables.theme;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.util.StringUtils;

/**
 * The theme colours of Excel a theme makes its colours of, and the name Excel shows them by.
 *
 * <p>The theme file writes them under {@code themeColors}: the name, and each of the twelve colours as
 * {@code #rrggbb} under its key, such as {@code accent1}. A colour the theme names as the palette of Excel names it,
 * such as {@code Blue, Accent 1, Lighter 60%}, is made of these.
 *
 * <p>A workbook whose theme colours are these is written every such colour as the theme colour it is, so Excel offers
 * it in its palette. Any other workbook is written the colour as {@code #rrggbb}: its theme is never changed, for
 * Apache POI has no way to set the theme of a workbook.
 *
 * @param name    the name Excel shows the theme colours by, such as {@code Office 2013 - 2022}
 * @param colours each theme colour as {@code #rrggbb}
 */
public record ExcelThemeColours(String name, Map<ExcelThemeColour, String> colours) {

    /** The key the file writes the name of the theme colours under. */
    private static final String NAME_KEY = "name";

    /** How many thousandths of a tint a per cent is. */
    private static final int PER_MILLE = 10;

    /**
     * The theme colours as the theme file writes them.
     *
     * @param written the name and the colours by their keys
     * @return the theme colours
     * @throws IllegalArgumentException when the file declares no name, leaves a colour out, names one Excel has not,
     *                                  or writes a colour another way than {@code #rrggbb}
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static ExcelThemeColours of(Map<String, String> written) {
        var name = written.get(NAME_KEY);
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("The theme colors declare no name");
        }
        var colours = new EnumMap<ExcelThemeColour, String>(ExcelThemeColour.class);
        written.forEach((key, rgb) -> {
            if (!NAME_KEY.equals(key)) {
                colours.put(colourOf(key), ThemeColour.of(Optional.ofNullable(rgb).orElse("")).rgb());
            }
        });
        for (var colour : ExcelThemeColour.values()) {
            if (!colours.containsKey(colour)) {
                throw new IllegalArgumentException("The theme colors give no " + colour.getKey());
            }
        }
        return new ExcelThemeColours(name, Collections.unmodifiableMap(colours));
    }

    private static ExcelThemeColour colourOf(String key) {
        return ExcelThemeColour.keyed(key)
                .orElseThrow(() -> new IllegalArgumentException("The theme colors of Excel have no colour " + key));
    }

    /**
     * Whether the theme colours of a workbook are these: each of the twelve, whatever name the workbook gives them.
     * An {@code .xls} workbook and an {@code .xlsx} one without a theme, such as one a program wrote rather than Excel,
     * have none.
     *
     * @param workbook the workbook
     * @return {@code true} when every theme colour of the workbook is the one of these
     */
    boolean areThoseOf(Workbook workbook) {
        var themes = workbook instanceof XSSFWorkbook xssf ? xssf.getStylesSource().getTheme() : null;
        return themes != null && Arrays.stream(ExcelThemeColour.values())
                .allMatch(colour -> Arrays.equals(PoiExcelHelper.toRgb(themes.getThemeColor(colour.index())),
                        PoiExcelHelper.toRgb(colours.get(colour))));
    }

    /**
     * A theme colour made lighter or darker, as the palette of Excel names it.
     *
     * @param colour the theme colour
     * @param tint   how much lighter, above 0, or darker, below 0, in per cent
     * @return the colour, drawn as Excel draws it
     */
    ThemeColour colourOf(ExcelThemeColour colour, int tint) {
        var themed = new ThemedColor(colour.index(), tint * PER_MILLE);
        var drawn = PoiExcelHelper.applyTint(PoiExcelHelper.toRgb(colours.get(colour)), themed.writtenTint());
        return new ThemeColour("#%06x".formatted(PoiExcelHelper.toRgbValue(drawn)), themed);
    }
}
