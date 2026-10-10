package org.openl.studio.projects.service.tables.theme;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.model.ThemesTable.ThemeElement;
import org.jspecify.annotations.Nullable;

/**
 * One of the ten theme colours of Excel the palette of Excel offers: the first and the second background and text, and
 * the six accents.
 *
 * <p>Each is known by the name the palette of Excel gives it and by the number a workbook writes it by.
 */
@RequiredArgsConstructor
public enum ExcelThemeColour {

    BACKGROUND_1("Background 1", ThemeElement.LT1),
    TEXT_1("Text 1", ThemeElement.DK1),
    BACKGROUND_2("Background 2", ThemeElement.LT2),
    TEXT_2("Text 2", ThemeElement.DK2),
    ACCENT_1("Accent 1", ThemeElement.ACCENT1),
    ACCENT_2("Accent 2", ThemeElement.ACCENT2),
    ACCENT_3("Accent 3", ThemeElement.ACCENT3),
    ACCENT_4("Accent 4", ThemeElement.ACCENT4),
    ACCENT_5("Accent 5", ThemeElement.ACCENT5),
    ACCENT_6("Accent 6", ThemeElement.ACCENT6);

    private static final Map<String, ExcelThemeColour> BY_LABEL = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(colour -> lowerCase(colour.label), Function.identity()));

    /** The name the palette of Excel gives the colour, such as {@code Accent 1}. */
    private final String label;

    /** The colour as the theme of a workbook holds it. */
    private final ThemeElement element;

    /** The number a workbook writes the colour by. */
    int index() {
        return element.idx;
    }

    /**
     * The colour the palette of Excel gives a name, written in any letter case.
     *
     * @param name the name, such as {@code Accent 1}
     * @return the colour, or {@code null} for a name no colour has
     */
    static @Nullable ExcelThemeColour named(String name) {
        return BY_LABEL.get(lowerCase(name));
    }

    private static String lowerCase(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
