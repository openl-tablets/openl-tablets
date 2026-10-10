package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;

/**
 * Covers how the table theme is read: the base every kind of table extends, its colours, and the YAML anchors, aliases
 * and merge keys one part of a theme repeats another with.
 */
class TableThemeServiceTest {

    private final TableThemeService service = new TableThemeService();

    @Test
    void everyKindExtendsTheSkinOfTheBase() {
        var theme = service.theme();
        var datatype = theme.lookOf(theme.datatype());
        var spreadsheet = theme.lookOf(theme.spreadsheet());
        var rules = theme.lookOf(theme.rules());
        var test = theme.lookOf(theme.test());

        // The signature, the properties and the closing line come from the base; the field names are the Datatype's
        // own.
        assertNull(datatype.header().style().background(), "The base leaves the header unfilled");
        assertEquals(datatype.header(), spreadsheet.header(), "Every kind of table is signed alike");
        assertEquals(datatype.properties(), spreadsheet.properties(),
                "Every kind of table closes its properties alike");
        assertEquals(ThemeLineStyle.THIN, spreadsheet.properties().border().bottom().style());
        assertEquals(ThemeLineStyle.THIN, datatype.lastRow().border().bottom().style());
        assertEquals("Franklin Gothic Book", datatype.style().fontFamily());
        assertEquals("#ddebf7", datatype.name().background().rgb());
        assertNull(theme.lookOf(theme.vocabulary()).name(), "A Vocabulary has no column of field names");
        // The parts the kinds share are written once and repeated by aliases: a Rules and a Test table title what they
        // take and what they give alike, while a Spreadsheet titles its formulas as what a table gives.
        assertEquals(rules.titles(), test.titles());
        assertEquals(rules.returnTitles(), test.returnTitles());
        assertEquals(rules.returnTitles().background().rgb(), spreadsheet.titles().background().rgb());
        assertEquals(Boolean.TRUE, spreadsheet.titles().bold());
        assertEquals(theme.base(), theme.lookOf(theme.tbasic()), "A kind the theme writes nothing for takes the base");
    }

    @Test
    void aKindChangesOnlyWhatItWritesInAPartOfTheBase() {
        var theme = TestThemes.of("extended-header.yaml").theme();
        var datatype = theme.lookOf(theme.datatype()).header();
        var vocabulary = theme.lookOf(theme.vocabulary()).header();

        assertEquals(Boolean.TRUE, datatype.name().bold(), "A kind named with nothing in it takes the base alone");
        assertEquals(Boolean.FALSE, vocabulary.name().bold(), "The Vocabulary writes its own name look");
        // The rest of the Vocabulary header is the one of the base, an alias reused for the type as well.
        assertEquals("#c6e0b4", vocabulary.style().background().rgb());
        assertEquals("#548235", vocabulary.keyword().color().rgb());
        assertEquals("#548235", vocabulary.type().color().rgb());
    }

    @Test
    void theThemeDrawsTheKindsTheStandardDescribesNoLookForInItsGeneralFormat() {
        var theme = service.theme();
        var base = theme.base();

        // The base is the General format of the standard alone: the cell style, the signature, the properties and the
        // line that closes the table, and no part of a kind.
        assertEquals(TableTheme.Look.builder()
                .style(base.style())
                .header(base.header())
                .properties(base.properties())
                .lastRow(base.lastRow())
                .build(), base);
        for (var kind : Arrays.asList(theme.tbasic(), theme.method(), theme.run(), theme.columnMatch(),
                theme.conditions(), theme.actions(), theme.returns(), theme.environment(), theme.properties(),
                theme.constants())) {
            assertNull(kind, "The theme writes no look for a kind the standard describes none for");
        }
        assertEquals(base, theme.lookOf(theme.environment()), "Such a kind takes the General format");
    }

    @Test
    void theThemeSignsEveryKindOfTableAlike() {
        var theme = service.theme();
        var datatype = theme.lookOf(theme.datatype()).header();
        var spreadsheet = theme.lookOf(theme.spreadsheet()).header();

        assertEquals(datatype, theme.lookOf(theme.vocabulary()).header(), "A Vocabulary is a Datatype");
        assertEquals(List.of(datatype.keyword(), datatype.name(), datatype.type(), datatype.parameters()),
                List.of(spreadsheet.keyword(), spreadsheet.name(), spreadsheet.type(), spreadsheet.parameters()),
                "Every piece of the signature is the one of the base");
        assertEquals(datatype.style(), spreadsheet.style());
        assertNull(spreadsheet.style().background());
    }

    @Test
    void aKindTheThemeWritesNothingForTakesTheBaseAlone() {
        var theme = TestThemes.of("datatype-extension.yaml").theme();

        assertEquals(theme.base(), theme.lookOf(theme.vocabulary()), "A Vocabulary takes the base alone");
        assertEquals(theme.base(), theme.lookOf(theme.spreadsheet()), "So does a Spreadsheet");
        // The Datatype also merges the base with the YAML merge key, which extends the base by what it already is.
        var datatype = theme.lookOf(theme.datatype());
        assertEquals(Boolean.TRUE, datatype.style().italic(), "The Datatype extends the base");
        assertEquals(new ThemeBorderLine(ThemeLineStyle.MEDIUM, new ThemeColour(new ThemedColor(5, -250))),
                datatype.lastRow().border().bottom());
        assertEquals("#fff2cc", datatype.name().background().rgb());
    }

    @Test
    void readsAColourByTheNameTheThemeGivesIt() {
        var theme = TestThemes.of("named-colours.yaml").theme();
        var datatype = theme.lookOf(theme.datatype());

        assertEquals("#1f4e78", datatype.header().keyword().color().rgb(), "The colour of a font");
        assertEquals("#ddebf7", datatype.name().background().rgb(), "The colour of a fill");
        assertEquals(new ThemeBorderLine(ThemeLineStyle.THIN, new ThemeColour(new ThemedColor(8, -500))),
                datatype.lastRow().border().bottom(), "The colour of a line");
        assertEquals("#000000", datatype.name().color().rgb(), "A colour a part names itself");
    }

    @Test
    void readsAColourAsThePaletteOfExcelNamesAThemeColour() {
        var theme = TestThemes.of("excel-theme-colours.yaml").theme();
        var datatype = theme.lookOf(theme.datatype());

        // Drawn as Office 2013 - 2022 draws the theme colour made lighter or darker, and written as that theme colour.
        assertEquals(new ThemeColour(new ThemedColor(4, 600)), datatype.name().background(),
                "Blue, Accent 1, Lighter 60%");
        assertEquals("#b4c6e7", datatype.name().background().rgb());
        assertEquals(new ThemeColour(new ThemedColor(0, -500)), datatype.header().keyword().color(),
                "White, Background 1, Darker 50%");
        assertEquals("#808080", datatype.header().keyword().color().rgb());
        assertEquals(new ThemeColour(new ThemedColor(1, 0)), datatype.name().color(),
                "A theme colour named without the name Excel gives it, in another letter case");
        assertEquals(new ThemeBorderLine(ThemeLineStyle.THIN, new ThemeColour(new ThemedColor(9, -250))),
                datatype.lastRow().border().bottom(), "The colour of a line");
        assertEquals(new ThemeColour(new ThemedColor(8, 800)), datatype.values().background(),
                "A theme colour a part names itself");
    }

    @Test
    void theThemeMakesItsColoursOfTheThemeColoursTheStandardNames() {
        var theme = service.theme();
        var style = theme.base().style();

        assertEquals(new ThemeColour(new ThemedColor(1, 0)), style.color(), "Black, Text 1");
        assertEquals(new ThemeColour(new ThemedColor(0, 0)), style.background(), "White, Background 1");
        assertEquals(new ThemeColour(new ThemedColor(8, 800)), theme.lookOf(theme.datatype()).name().background(),
                "Blue, Accent 5, Lighter 80%");
    }

    @ParameterizedTest
    @CsvSource({
            "misnamed-tint, as the palette of Excel names a theme colour",
            "overnamed-theme-colour, as the palette of Excel names a theme colour",
            "unknown-theme-colour, as the palette of Excel names a theme colour"})
    void refusesAThemeColourOfExcelWrittenAnotherWay(String theme, String reason) {
        var refused = refusalOf(fixture(theme));
        var message = refused.getCause().getMessage();
        assertTrue(message.contains(reason), message);
    }

    @Test
    void refusesAThemeNamingAColourItGivesNoName() {
        var refused = refusalOf(fixture("named-border-colour"));
        var reason = refused.getCause().getMessage();
        assertTrue(reason.contains("colors: red"), reason);
    }

    @Test
    void refusesAThemeSettingAColourByAListOfNames() {
        var refused = refusalOf(fixture("listed-colour"));
        assertTrue(refused.getMessage().contains("listed-colour.yaml"));
    }

    @Test
    void refusesAnEmptyThemeFile() {
        var refused = refusalOf(fixture("empty"));
        assertTrue(refused.getMessage().contains("is empty"));
    }

    @Test
    void refusesAThemeNamingAnAttributeItDoesNotKnow() {
        var refused = refusalOf(fixture("unknown-attribute"));
        assertTrue(refused.getMessage().contains("unknown-attribute.yaml"));
        assertTrue(refused.getCause().getMessage().contains("backgroundColour"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short-colour", "unnamed-colour"})
    void refusesAThemeWritingAColourAnyOtherWayThanAThemeColourOfExcel(String theme) {
        // A colour of its own would be written into a workbook of a theme as no theme colour, and the table would not
        // take the colours of the theme, whether a part sets it or the theme gives it a name.
        var refused = refusalOf(fixture(theme));
        assertTrue(refused.getCause().getMessage().contains("as the palette of Excel names a theme colour"),
                refused.getCause().getMessage());
    }

    @Test
    void refusesAThemeWritingAFontSizeThatIsNotAWholeNumber() {
        // Cut down to a whole number, the size would be drawn and written other than the file writes it.
        var refused = refusalOf(fixture("fractional-size"));
        assertTrue(refused.getMessage().contains("fractional-size.yaml"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"zero-size", "huge-size"})
    void refusesAThemeWritingAFontSizeExcelHasNot(String theme) {
        // Excel sizes a font from 1 to 409 points: another size would be written into a workbook Excel refuses.
        var refused = refusalOf(fixture(theme));
        assertTrue(refused.getCause().getMessage().contains("from 1 to 409"), refused.getCause().getMessage());
    }

    @Test
    void refusesAThemeWritingAKeyTwice() {
        var refused = refusalOf(fixture("twice"));
        assertTrue(refused.getMessage().contains("twice.yaml"));
    }

    @Test
    void refusesToStartWithAThemeItCannotRead() {
        var file = fixture("twice");

        var refused = assertThrows(IllegalStateException.class, () -> new TableThemeService(file));
        assertTrue(refused.getMessage().contains("twice.yaml"));
    }

    @Test
    void drawsATableTheThemeCannotBeLaidOutOverPlain() {
        var broken = mock(IOpenLTable.class);
        when(broken.getGridTable()).thenThrow(new IllegalStateException("The grid of the table cannot be read"));

        // Every table a screen shows is drawn with the theme: a failure leaves this one plain, not unreadable.
        assertNull(service.layoutOf(broken));
    }

    /** Why a theme file is refused, as reading it tells. */
    private static IllegalStateException refusalOf(Resource file) {
        return assertThrows(IllegalStateException.class, () -> TableThemeService.read(file));
    }

    private static ClassPathResource fixture(String id) {
        return new ClassPathResource("test-table-themes/" + id + ".yaml");
    }
}
