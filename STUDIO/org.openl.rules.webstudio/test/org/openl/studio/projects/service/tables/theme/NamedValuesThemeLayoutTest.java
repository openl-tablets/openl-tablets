package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.TableThemeView;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the look a theme gives the tables that name a value in each row: an Environment, a Properties and a
 * Constants table, written either way round.
 */
class NamedValuesThemeLayoutTest {

    private static final String SHEET = "Settings";
    private static final String THEME = "default";

    /** The looks the default theme gives the places of these tables. */
    private static final String WHITE = "#ffffff";
    private static final String MUTED = "#808080";
    private static final String GREY_HEADER = "#e7e6e6";
    private static final String SETTING = "#f2f2f2";
    private static final String LIGHT_LINE = "#d9d9d9";
    private static final String CONSTANT_NAME = "#ddebf7";

    /** An Environment table importing two packages, the setting merged over both. */
    private static final int ENVIRONMENT_ROW = 1;

    /** A Properties table of a category of tables. */
    private static final int PROPERTIES_ROW = 6;

    /** A Constants table, a constant in each row. */
    private static final int CONSTANTS_ROW = 11;

    /** A Constants table written transposed, a constant in each column. */
    private static final int TRANSPOSED_ROW = 16;

    private final TableThemeService service = new TableThemeService();

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, NamedValuesThemeLayoutTest::fillSheet);
    }

    @Test
    void offersEveryThemeForAnEnvironmentAPropertiesAndAConstantsTable() {
        for (var row : List.of(ENVIRONMENT_ROW, PROPERTIES_ROW, CONSTANTS_ROW, TRANSPOSED_ROW)) {
            assertEquals(List.of("default", "green"),
                    service.getThemes(tableAt(row)).stream().map(TableThemeView::id).toList(), "row " + row);
        }
    }

    @Test
    void drawsAnEnvironmentTableInGreys() {
        var layout = layoutAt(ENVIRONMENT_ROW);

        assertEquals(GREY_HEADER, layout.at(ENVIRONMENT_ROW, 1).style().background());
        // The setting is merged over both of its values: every cell of it takes the look of the setting.
        for (var row = ENVIRONMENT_ROW + 1; row <= ENVIRONMENT_ROW + 2; row++) {
            assertEquals(SETTING, layout.at(row, 1).style().background());
            assertEquals(new ThemeBorderLine(ThemeLineStyle.THIN, LIGHT_LINE), right(layout.at(row, 1)));
            assertEquals(WHITE, layout.at(row, 2).style().background());
        }
        // A light line runs under each value, and the line closing the table under the last one.
        assertEquals(new ThemeBorderLine(ThemeLineStyle.THIN, LIGHT_LINE), bottom(layout.at(ENVIRONMENT_ROW + 1, 2)));
        assertEquals(new ThemeBorderLine(ThemeLineStyle.THIN, null), bottom(layout.at(ENVIRONMENT_ROW + 2, 2)));
    }

    @Test
    void drawsAPropertiesTableAsAnEnvironmentAndSignsItAsEveryTable() {
        var layout = layoutAt(PROPERTIES_ROW);

        assertEquals(GREY_HEADER, layout.at(PROPERTIES_ROW, 1).style().background());
        assertEquals(SETTING, layout.at(PROPERTIES_ROW + 1, 1).style().background());
        assertEquals(WHITE, layout.at(PROPERTIES_ROW + 2, 2).style().background());
        // The keyword is muted and the name of the table bold, as in the header of every table.
        var runs = layout.at(PROPERTIES_ROW, 1).runs("Properties Catalogue");
        assertEquals(MUTED, runs.getFirst().style().color());
        assertEquals(Boolean.TRUE, runs.getLast().style().bold());
    }

    @Test
    void givesTheTypesTheNamesAndTheValuesOfAConstantsTableTheirLooks() {
        var layout = layoutAt(CONSTANTS_ROW);

        for (var row = CONSTANTS_ROW + 1; row <= CONSTANTS_ROW + 2; row++) {
            assertEquals(MUTED, layout.at(row, 1).style().color());
            assertEquals(CONSTANT_NAME, layout.at(row, 2).style().background());
            assertEquals(WHITE, layout.at(row, 3).style().background());
        }
        assertEquals(WHITE, layout.at(CONSTANTS_ROW, 1).style().background(), "A Constants table is not grey");
        assertEquals(ThemeLineStyle.NONE, bottom(layout.at(CONSTANTS_ROW + 1, 3)).style());
        assertEquals(ThemeLineStyle.THIN, bottom(layout.at(CONSTANTS_ROW + 2, 3)).style(), "The last row closes it");
    }

    @Test
    void givesATransposedConstantsTableTheLooksAsItIsCompiled() {
        var layout = layoutAt(TRANSPOSED_ROW);

        // A constant in each column: its type, then its name, then its value.
        for (var column = 1; column <= 2; column++) {
            assertEquals(MUTED, layout.at(TRANSPOSED_ROW + 1, column).style().color());
            assertEquals(CONSTANT_NAME, layout.at(TRANSPOSED_ROW + 2, column).style().background());
            assertEquals(WHITE, layout.at(TRANSPOSED_ROW + 3, column).style().background());
        }
        assertEquals(ThemeLineStyle.THIN, bottom(layout.at(TRANSPOSED_ROW + 3, 2)).style(), "The last row closes it");
    }

    @Test
    void writesTheLookOfAnEnvironmentTableIntoTheWorkbook() {
        service.writer(THEME).writeAll(List.of(tableAt(ENVIRONMENT_ROW)), Map.of());

        var written = read(ENVIRONMENT_ROW);
        assertEquals("Environment", written.getFirst().getFirst().value(), "The theme changes no text");
        assertEquals(GREY_HEADER, written.getFirst().getFirst().style().background());
        assertEquals(SETTING, written.get(1).getFirst().style().background());
        assertEquals(LIGHT_LINE, written.get(1).get(1).style().border().bottom().color());
    }

    /** The table whose header stands in the given row: an Environment table has no name to find it by. */
    private IOpenLTable tableAt(int row) {
        return TableTestProjects.tableAt(dir, row);
    }

    private ThemedTable layoutAt(int row) {
        return service.layoutOf(tableAt(row), THEME);
    }

    /** The cells of a table as the workbook now holds them, with their styles. */
    private List<List<RawTableCell>> read(int row) {
        return TableTestProjects.styledSource(tableAt(row));
    }

    private static ThemeBorderLine bottom(ThemedTable.ThemedCell cell) {
        return cell.style().border().bottom();
    }

    private static ThemeBorderLine right(ThemedTable.ThemedCell cell) {
        return cell.style().border().right();
    }

    /** An Environment, a Properties and a Constants table, and a Constants table written transposed. */
    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, ENVIRONMENT_ROW, 1, "Environment");
        merge(sheet, ENVIRONMENT_ROW, ENVIRONMENT_ROW, 1, 2);
        TableTestProjects.row(sheet, ENVIRONMENT_ROW + 1, 1, "import", "java.util");
        TableTestProjects.row(sheet, ENVIRONMENT_ROW + 2, 1, null, "java.time");
        merge(sheet, ENVIRONMENT_ROW + 1, ENVIRONMENT_ROW + 2, 1, 1);

        TableTestProjects.row(sheet, PROPERTIES_ROW, 1, "Properties Catalogue");
        merge(sheet, PROPERTIES_ROW, PROPERTIES_ROW, 1, 2);
        TableTestProjects.row(sheet, PROPERTIES_ROW + 1, 1, "scope", "Category");
        TableTestProjects.row(sheet, PROPERTIES_ROW + 2, 1, "category", "Catalogue");

        TableTestProjects.row(sheet, CONSTANTS_ROW, 1, "Constants Limits");
        merge(sheet, CONSTANTS_ROW, CONSTANTS_ROW, 1, 3);
        TableTestProjects.row(sheet, CONSTANTS_ROW + 1, 1, "Integer", "DEFAULT_AGE", "43");
        TableTestProjects.row(sheet, CONSTANTS_ROW + 2, 1, "Double", "MAX_RATE", "0.7");

        TableTestProjects.row(sheet, TRANSPOSED_ROW, 1, "Constants Bounds");
        merge(sheet, TRANSPOSED_ROW, TRANSPOSED_ROW, 1, 2);
        TableTestProjects.row(sheet, TRANSPOSED_ROW + 1, 1, "Integer", "Double");
        TableTestProjects.row(sheet, TRANSPOSED_ROW + 2, 1, "MIN_AGE", "MIN_RATE");
        TableTestProjects.row(sheet, TRANSPOSED_ROW + 3, 1, "18", "0.1");
    }
}
