package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.TableThemeView;

/**
 * Covers how the table themes are found and read: their names, and the YAML anchors, aliases and merge keys one part
 * of a theme extends or repeats another with.
 */
class TableThemeServiceTest {

    private static final String FIXTURES = "classpath*:test-table-themes/";

    private final TableThemeService service = new TableThemeService();

    @Test
    void offersEveryThemeOfStudioByName() {
        assertEquals(List.of(new TableThemeView("default", "Default"), new TableThemeView("green", "Green")),
                service.getThemes());
    }

    @Test
    void aKindExtendsTheBaseThroughTheMergeKey() {
        var theme = service.theme("default");
        var datatype = theme.lookOf(false);

        // The header and the closing line come from the base, the field names are the Datatype's own.
        assertEquals("#b4c6e7", datatype.header().style().background());
        assertEquals(ThemeLineStyle.THIN, datatype.lastRow().border().bottom().style());
        assertEquals("Franklin Gothic Book", datatype.style().fontFamily());
        assertEquals("#ddebf7", datatype.name().background());
        assertNull(theme.lookOf(true).name(), "A Vocabulary has no column of field names");
    }

    @Test
    void aKindReplacesWhatItWritesItselfAndExtendsANestedPartThroughItsAnchor() {
        var green = service.theme("green");
        var datatype = green.lookOf(false).header();
        var vocabulary = green.lookOf(true).header();

        assertEquals(Boolean.TRUE, datatype.name().bold());
        assertEquals(Boolean.FALSE, vocabulary.name().bold(), "The Vocabulary writes its own name look");
        // The rest of the Vocabulary header is the one the base anchors, an alias reused for the type as well.
        assertEquals("#c6e0b4", vocabulary.style().background());
        assertEquals("#548235", vocabulary.type().color());
        assertEquals("#548235", vocabulary.style().border().top().color());
    }

    @Test
    void aThemeStylesOnlyTheKindsItNamesALookFor() {
        var theme = new TableThemeService(FIXTURES + "datatype-only.yaml").theme("datatype-only");

        assertNull(theme.lookOf(true), "The base styles nothing by itself: a kind without a look is left as it is");
        var datatype = theme.lookOf(false);
        assertEquals(Boolean.TRUE, datatype.style().italic(), "The Datatype extends the base");
        assertEquals(new ThemeBorderLine(ThemeLineStyle.MEDIUM, "#ff0000"), datatype.lastRow().border().bottom());
        assertEquals("#fff2cc", datatype.name().background());
    }

    @Test
    void offersTheThemesItCanReadAndLeavesOutTheOnesItRefuses() {
        // Next to the theme it reads lie three it refuses; Studio starts with the one it can offer.
        assertEquals(List.of(new TableThemeView("datatype-only", "Datatype Only")),
                new TableThemeService(FIXTURES + "*.yaml").getThemes());
    }

    @Test
    void refusesAThemeNamingAnAttributeItDoesNotKnow() {
        var refused = refusalOf(fixture("unknown-attribute"));
        assertTrue(refused.getMessage().contains("unknown-attribute.yaml"));
        assertTrue(refused.getCause().getMessage().contains("backgroundColour"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short-colour", "named-border-colour"})
    void refusesAThemeWritingAColourAnyOtherWayThanRrggbb(String theme) {
        // A colour written another way would be drawn as one colour and written into the workbook as another.
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
