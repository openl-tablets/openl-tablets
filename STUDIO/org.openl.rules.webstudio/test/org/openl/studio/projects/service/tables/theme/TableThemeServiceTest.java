package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.TableThemeView;

/**
 * Covers how the table themes are found and read: their names, the base every kind of table extends, and the YAML
 * anchors, aliases and merge keys one part of a theme repeats another with.
 */
class TableThemeServiceTest {

    private static final String FIXTURES = "classpath*:test-table-themes/";

    private final TableThemeService service = new TableThemeService();

    @Test
    void offersEveryThemeOfStudioByName() {
        assertEquals(List.of(new TableThemeView("green", "Green"), new TableThemeView("standard", "Standard")),
                service.getThemes());
    }

    @Test
    void everyKindExtendsTheSkinOfTheBase() {
        var theme = service.theme("standard");
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
        assertEquals("#ddebf7", datatype.name().background());
        assertNull(theme.lookOf(theme.vocabulary()).name(), "A Vocabulary has no column of field names");
        // The parts the kinds share are written once in the base: a Rules and a Test table title what they take and
        // what they give alike, while a Spreadsheet titles its formulas as what a table gives.
        assertEquals(rules.titles(), test.titles());
        assertEquals(rules.returnTitles(), test.returnTitles());
        assertEquals(rules.returnTitles().background(), spreadsheet.titles().background());
        assertEquals(Boolean.TRUE, spreadsheet.titles().bold());
        assertEquals(rules.code(), theme.lookOf(theme.tbasic()).code());
    }

    @Test
    void aKindChangesOnlyWhatItWritesInAPartOfTheBase() {
        var theme = new TableThemeService(FIXTURES + "extended-header.yaml").theme("extended-header");
        var datatype = theme.lookOf(theme.datatype()).header();
        var vocabulary = theme.lookOf(theme.vocabulary()).header();

        assertEquals(Boolean.TRUE, datatype.name().bold(), "A kind named with nothing in it takes the base alone");
        assertEquals(Boolean.FALSE, vocabulary.name().bold(), "The Vocabulary writes its own name look");
        // The rest of the Vocabulary header is the one of the base, an alias reused for the type as well.
        assertEquals("#c6e0b4", vocabulary.style().background());
        assertEquals("#548235", vocabulary.keyword().color());
        assertEquals("#548235", vocabulary.type().color());
    }

    @Test
    void theThemesOfStudioDrawAPropertiesTableInTheGreysOfAnEnvironment() {
        for (var id : List.of("standard", "green")) {
            var theme = service.theme(id);
            var environment = theme.lookOf(theme.environment());

            assertEquals(environment, theme.lookOf(theme.properties()), id + ": a Properties table is as technical");
            assertEquals("#e7e6e6", environment.header().style().background(), id);
            assertEquals("#f2f2f2", environment.name().background(), id);
            assertEquals(theme.lookOf(theme.datatype()).header().name(), environment.header().name(),
                    id + ": it is signed as every table");
        }
    }

    @Test
    void theThemesOfStudioDrawTheTablesThatDeclareWhatADecisionTableTakesInTheLookOfARulesTable() {
        for (var id : List.of("standard", "green")) {
            var theme = service.theme(id);
            var rules = theme.lookOf(theme.rules());

            assertEquals(rules, theme.lookOf(theme.conditions()), id + ": a Conditions table");
            assertEquals(rules, theme.lookOf(theme.actions()), id + ": an Actions table");
            assertEquals(rules, theme.lookOf(theme.returns()), id + ": a Returns table");
        }
    }

    @Test
    void theThemesOfStudioDrawATBasicTableAsASpreadsheetAndAColumnMatchAsARulesTable() {
        for (var id : List.of("standard", "green")) {
            var theme = service.theme(id);
            var spreadsheet = theme.lookOf(theme.spreadsheet());
            var rules = theme.lookOf(theme.rules());
            var tbasic = theme.lookOf(theme.tbasic());
            var columnMatch = theme.lookOf(theme.columnMatch());

            // A part the Spreadsheet leaves out, such as the values of the green theme, is left out alike.
            assertEquals(Arrays.asList(spreadsheet.titles(), spreadsheet.stepTitle(), spreadsheet.values(),
                            spreadsheet.sections(), spreadsheet.result()),
                    Arrays.asList(tbasic.titles(), tbasic.stepTitle(), tbasic.values(), tbasic.sections(),
                            tbasic.result()),
                    id + ": a TBasic table looks like a Spreadsheet");
            assertEquals(rules.code(), tbasic.code(), id + ": its column ids look like the code of a Rules table");
            assertNull(tbasic.condition(), id + ": its conditions keep the look every cell starts from");
            assertEquals(List.of(rules.code(), rules.titles(), rules.returnTitles(), rules.groups()),
                    List.of(columnMatch.code(), columnMatch.titles(), columnMatch.returnTitles(), columnMatch.groups()),
                    id + ": a ColumnMatch table looks like a Rules table");
            // A line after the names it checks, and between the columns of its values.
            assertEquals(ThemeLineStyle.THIN, columnMatch.name().border().right().style(), id);
            assertEquals(ThemeLineStyle.THIN, columnMatch.values().border().right().style(), id);
            assertEquals(rules.returns().background(), columnMatch.returns().background(), id);
            assertEquals(ThemeLineStyle.THIN, columnMatch.returns().border().right().style(), id);
            assertEquals(theme.base(), theme.lookOf(theme.method()), id + ": a Method table takes the base alone");
        }
    }

    @Test
    void theThemesOfStudioSignEveryKindOfTableAlike() {
        for (var id : List.of("standard", "green")) {
            var theme = service.theme(id);
            var datatype = theme.lookOf(theme.datatype()).header();
            var spreadsheet = theme.lookOf(theme.spreadsheet()).header();

            assertEquals(datatype, theme.lookOf(theme.vocabulary()).header(), id + ": a Vocabulary is a Datatype");
            assertEquals(List.of(datatype.keyword(), datatype.name(), datatype.type(), datatype.parameters()),
                    List.of(spreadsheet.keyword(), spreadsheet.name(), spreadsheet.type(), spreadsheet.parameters()),
                    id + ": every piece of the signature is the one of the base");
            assertEquals(datatype.style().withBackground(null), spreadsheet.style(),
                    id + ": at most the fill of the header is a Datatype's own");
            assertNull(spreadsheet.style().background(), id);
        }
        var green = service.theme("green");
        assertEquals("#c6e0b4", green.lookOf(green.datatype()).header().style().background(),
                "The green theme fills the header of a Datatype");
    }

    @Test
    void aKindTheThemeWritesNothingForTakesTheBaseAlone() {
        var theme = new TableThemeService(FIXTURES + "datatype-extension.yaml").theme("datatype-extension");

        assertEquals(theme.base(), theme.lookOf(theme.vocabulary()), "A Vocabulary takes the base alone");
        assertEquals(theme.base(), theme.lookOf(theme.spreadsheet()), "So does a Spreadsheet");
        // The Datatype also merges the base with the YAML merge key, which extends the base by what it already is.
        var datatype = theme.lookOf(theme.datatype());
        assertEquals(Boolean.TRUE, datatype.style().italic(), "The Datatype extends the base");
        assertEquals(new ThemeBorderLine(ThemeLineStyle.MEDIUM, "#ff0000"), datatype.lastRow().border().bottom());
        assertEquals("#fff2cc", datatype.name().background());
    }

    @Test
    void offersTheThemesItCanReadAndLeavesOutTheOnesItRefuses() {
        // Next to the themes it reads lie the ones it refuses; Studio starts with the ones it can offer.
        assertEquals(List.of(new TableThemeView("datatype-extension", "Datatype Extension"),
                        new TableThemeView("decision-kinds", "Decision Kinds"),
                        new TableThemeView("extended-header", "Extended Header"),
                        new TableThemeView("named-colours", "Named Colours"),
                        new TableThemeView("tbasic-condition", "TBasic Condition")),
                new TableThemeService(FIXTURES + "*.yaml").getThemes());
    }

    @Test
    void readsAColourByTheNameTheThemeGivesIt() {
        var theme = new TableThemeService(FIXTURES + "named-colours.yaml").theme("named-colours");
        var datatype = theme.lookOf(theme.datatype());

        assertEquals("#1f4e78", datatype.header().keyword().color(), "The colour of a font");
        assertEquals("#ddebf7", datatype.name().background(), "The colour of a fill");
        assertEquals(new ThemeBorderLine(ThemeLineStyle.THIN, "#1f4e78"), datatype.lastRow().border().bottom(),
                "The colour of a line");
        assertEquals("#000000", datatype.name().color(), "A colour written as #rrggbb");
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
        assertTrue(refused.getMessage().contains("declares no name"));
    }

    @Test
    void refusesAThemeNamingAnAttributeItDoesNotKnow() {
        var refused = refusalOf(fixture("unknown-attribute"));
        assertTrue(refused.getMessage().contains("unknown-attribute.yaml"));
        assertTrue(refused.getCause().getMessage().contains("backgroundColour"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short-colour", "unnamed-colour"})
    void refusesAThemeWritingAColourAnyOtherWayThanRrggbb(String theme) {
        // A colour written another way would be drawn as one colour and written into the workbook as another, whether
        // a part sets it or the theme gives it a name.
        var refused = refusalOf(fixture(theme));
        assertTrue(refused.getCause().getMessage().contains("#rrggbb"), refused.getCause().getMessage());
    }

    @Test
    void refusesAThemeWithoutAName() {
        var refused = refusalOf(fixture("nameless"));
        assertTrue(refused.getMessage().contains("declares no name"));
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
    void refusesAThemeItDoesNotHave() {
        var refused = assertThrows(BadRequestException.class, () -> service.writer("purple"));
        assertEquals("openl.error.400.table.theme.unknown.message", refused.getErrorCode());
    }

    /** Why a theme file is not offered, as reading it tells. */
    private static IllegalStateException refusalOf(Resource file) {
        return assertThrows(IllegalStateException.class, () -> TableThemeService.read(file));
    }

    private static ClassPathResource fixture(String id) {
        return new ClassPathResource("test-table-themes/" + id + ".yaml");
    }
}
