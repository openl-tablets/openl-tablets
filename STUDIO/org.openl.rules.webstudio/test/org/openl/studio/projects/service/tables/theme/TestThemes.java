package org.openl.studio.projects.service.tables.theme;

/**
 * The table themes the tests read besides the shipped ones.
 *
 * <p>The shipped themes give the kinds the formatting standard of OpenL tables describes no look for the General
 * format alone. The layouts of those kinds are tested with a theme that gives each of them a look of its own.
 */
public final class TestThemes {

    /** The identifier of the theme that gives the kinds of table in the General format a look of their own. */
    public static final String EVERY_KIND = "every-kind";

    private TestThemes() {
    }

    /**
     * A theme that gives a look of its own to every kind of table the shipped themes leave to the General format: the
     * parts the kinds share are written once in its base, and every kind extends it, so the looks tell the places of
     * every layout apart.
     *
     * @return the themes, the one of {@link #EVERY_KIND} alone
     */
    public static TableThemeService everyKind() {
        return new TableThemeService("classpath*:test-table-themes/" + EVERY_KIND + ".yaml");
    }
}
