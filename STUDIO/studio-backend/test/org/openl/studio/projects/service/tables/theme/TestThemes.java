package org.openl.studio.projects.service.tables.theme;

import org.springframework.core.io.ClassPathResource;

/**
 * The table themes the tests read besides the shipped one.
 *
 * <p>The shipped theme gives the kinds the formatting standard of OpenL tables describes no look for the General
 * format alone. The layouts of those kinds are tested with a theme that gives each of them a look of its own.
 */
public final class TestThemes {

    private TestThemes() {
    }

    /**
     * The theme of a file of the test themes.
     *
     * @param file the name of the file in {@code test-table-themes}, such as {@code every-kind.yaml}
     * @return the theme
     * @throws IllegalStateException when the file is not a theme
     */
    public static TableThemeService of(String file) {
        return new TableThemeService(new ClassPathResource("test-table-themes/" + file));
    }

    /**
     * A theme that gives a look of its own to every kind of table the shipped themes leave to the General format: the
     * parts the kinds share are written once in its base, and every kind extends it, so the looks tell the places of
     * every layout apart.
     *
     * @return the theme
     */
    public static TableThemeService everyKind() {
        return of("every-kind.yaml");
    }
}
