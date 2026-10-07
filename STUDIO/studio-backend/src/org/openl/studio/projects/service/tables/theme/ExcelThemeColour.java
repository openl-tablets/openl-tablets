package org.openl.studio.projects.service.tables.theme;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.model.ThemesTable.ThemeElement;
import org.jspecify.annotations.Nullable;

/**
 * One of the twelve theme colours of Excel, which the colours of Excel are made of: the first and the second text and
 * background, the six accents, and the colours of a hyperlink.
 *
 * <p>Each is known by the key a theme file writes it under, by the name the palette of Excel gives it, and by the
 * number a workbook writes it by. The palette of Excel offers the first ten, and its dialog of the theme colours the
 * colours of a hyperlink too.
 */
@Getter
@RequiredArgsConstructor
public enum ExcelThemeColour {

    BACKGROUND_1("background1", "Background 1", ThemeElement.LT1),
    TEXT_1("text1", "Text 1", ThemeElement.DK1),
    BACKGROUND_2("background2", "Background 2", ThemeElement.LT2),
    TEXT_2("text2", "Text 2", ThemeElement.DK2),
    ACCENT_1("accent1", "Accent 1", ThemeElement.ACCENT1),
    ACCENT_2("accent2", "Accent 2", ThemeElement.ACCENT2),
    ACCENT_3("accent3", "Accent 3", ThemeElement.ACCENT3),
    ACCENT_4("accent4", "Accent 4", ThemeElement.ACCENT4),
    ACCENT_5("accent5", "Accent 5", ThemeElement.ACCENT5),
    ACCENT_6("accent6", "Accent 6", ThemeElement.ACCENT6),
    HYPERLINK("hyperlink", "Hyperlink", ThemeElement.HLINK),
    FOLLOWED_HYPERLINK("followedHyperlink", "Followed Hyperlink", ThemeElement.FOLHLINK);

    private static final Map<String, ExcelThemeColour> BY_KEY = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ExcelThemeColour::getKey, Function.identity()));

    private static final Map<String, ExcelThemeColour> BY_LABEL = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(colour -> lowerCase(colour.label), Function.identity()));

    /** The key a theme file writes the colour under, among its theme colours, such as {@code accent1}. */
    private final String key;

    /** The name the palette of Excel gives the colour, such as {@code Accent 1}. */
    private final String label;

    /** The colour as the theme of a workbook holds it. */
    private final ThemeElement element;

    /** The number a workbook writes the colour by. */
    int index() {
        return element.idx;
    }

    /**
     * The colour a theme file writes under a key.
     *
     * @param key the key, such as {@code accent1}
     * @return the colour, or nothing for a key no colour has
     */
    static Optional<ExcelThemeColour> keyed(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
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
