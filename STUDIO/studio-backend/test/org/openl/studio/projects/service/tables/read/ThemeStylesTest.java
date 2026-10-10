package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableStyleSource;
import org.openl.studio.projects.model.tables.RawTableThemeColor;
import org.openl.studio.projects.model.tables.RawTableThemeColorName;
import org.openl.studio.projects.service.tables.theme.ThemeBorder;
import org.openl.studio.projects.service.tables.theme.ThemeBorderLine;
import org.openl.studio.projects.service.tables.theme.ThemeColour;
import org.openl.studio.projects.service.tables.theme.ThemeHorizontalAlign;
import org.openl.studio.projects.service.tables.theme.ThemeLineStyle;
import org.openl.studio.projects.service.tables.theme.ThemeStyle;
import org.openl.studio.projects.service.tables.theme.ThemeVerticalAlign;

class ThemeStylesTest {

    /** A look that sets the alignment and the font at their defaults, as the base style of the theme does. */
    private static final ThemeStyle DEFAULTS = ThemeStyle.builder()
            .align(ThemeHorizontalAlign.LEFT)
            .valign(ThemeVerticalAlign.BOTTOM)
            .bold(false)
            .italic(false)
            .underline(false)
            .strikeout(false)
            .build();

    @Test
    void leavesOutWhatTheThemeSetsAtItsDefault() {
        var drawn = ThemeStyles.of(DEFAULTS, null);

        // The defaults are left out of what a read reports, as the style read from the workbook leaves them out.
        assertEquals(RawTableCellStyle.builder().source(RawTableStyleSource.THEME).build(), drawn);
    }

    @Test
    void keepsTheIndentOfTheCellAndNothingElseOfTheWorkbook() {
        var drawn = ThemeStyles.of(ThemeStyle.builder().build(), 2);

        // The indent sets out the structure of a table, such as the steps of a TBasic algorithm.
        assertEquals(RawTableCellStyle.builder().source(RawTableStyleSource.THEME).indent(2).build(), drawn);
    }

    @Test
    void givesAPieceOfTextTheFontOfTheThemeAlone() {
        var piece = ThemeStyles.fontOf(ThemeStyle.builder().bold(true).build());

        assertEquals(RawTableStyleSource.THEME, piece.source());
        assertEquals(Boolean.TRUE, piece.bold());
        assertNull(piece.italic());
        assertNull(piece.color(), "A piece of text keeps no colour of the workbook");
    }

    @Test
    void reportsEveryColourOfTheLookAsTheThemeColourItIs() {
        var blue = new ThemeColour(new ThemedColor(4, 600));
        var orange = new ThemeColour(new ThemedColor(5, -250));
        var look = ThemeStyle.builder()
                .background(blue)
                .color(orange)
                .border(new ThemeBorder(null, null, new ThemeBorderLine(ThemeLineStyle.THIN, blue), null))
                .build();

        var drawn = ThemeStyles.of(look, null);

        assertEquals("#b4c6e7", drawn.background());
        assertEquals(new RawTableThemeColor(RawTableThemeColorName.ACCENT1, 0.6), drawn.backgroundTheme());
        // Drawn as Office 2013 - 2022 draws Orange, Accent 2, Darker 25%.
        assertEquals("#c65911", drawn.color());
        assertEquals(new RawTableThemeColor(RawTableThemeColorName.ACCENT2, -0.25), drawn.colorTheme());
        assertEquals(new RawTableThemeColor(RawTableThemeColorName.ACCENT1, 0.6), drawn.border().bottom().colorTheme());
        assertEquals(drawn.colorTheme(), ThemeStyles.fontOf(look).colorTheme());
    }
}
