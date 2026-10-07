package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableHorizontalAlign;
import org.openl.studio.projects.model.tables.RawTableStyleSource;
import org.openl.studio.projects.model.tables.RawTableVerticalAlign;
import org.openl.studio.projects.service.tables.theme.ThemeHorizontalAlign;
import org.openl.studio.projects.service.tables.theme.ThemeStyle;
import org.openl.studio.projects.service.tables.theme.ThemeVerticalAlign;

class ThemeStylesTest {

    /** A cell of a workbook centred, aligned to the top, bold and italic. */
    private static final RawTableCellStyle WORKBOOK = RawTableCellStyle.builder()
            .align(RawTableHorizontalAlign.CENTER)
            .valign(RawTableVerticalAlign.TOP)
            .bold(true)
            .italic(true)
            .underline(true)
            .strikeout(true)
            .build();

    /** A look that sets every one of those attributes at its default, as the base style of the Standard theme does. */
    private static final ThemeStyle DEFAULTS = ThemeStyle.builder()
            .align(ThemeHorizontalAlign.LEFT)
            .valign(ThemeVerticalAlign.BOTTOM)
            .bold(false)
            .italic(false)
            .underline(false)
            .strikeout(false)
            .build();

    @Test
    void takesAwayTheAlignmentAndTheFontOfTheWorkbookWhereTheThemeSetsThemAtTheirDefault() {
        var drawn = ThemeStyles.over(WORKBOOK, DEFAULTS);

        // The defaults are left out of what a read reports, as the style read from the workbook leaves them out.
        assertNull(drawn.align());
        assertNull(drawn.valign());
        assertNull(drawn.bold());
        assertNull(drawn.italic());
        assertNull(drawn.underline());
        assertNull(drawn.strikeout());
    }

    @Test
    void keepsWhatTheWorkbookGivesWhereTheThemeSetsNothing() {
        var drawn = ThemeStyles.over(WORKBOOK, ThemeStyle.builder().build());

        // The style is the theme's all the same: a read reports it in place of the style of the workbook.
        assertEquals(WORKBOOK.toBuilder().source(RawTableStyleSource.THEME).build(), drawn);
    }

    @Test
    void takesTheFontFlagsOfTheCellAwayWhereThePieceTurnsThemOff() {
        var piece = ThemeStyles.fontOf(WORKBOOK, DEFAULTS);

        assertEquals(RawTableStyleSource.THEME, piece.source());
        assertNull(piece.bold());
        assertNull(piece.italic());
        assertNull(piece.underline());
        assertNull(piece.strikeout());
    }
}
