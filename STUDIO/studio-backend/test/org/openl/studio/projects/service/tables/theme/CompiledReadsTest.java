package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the rule every layout follows: a table is themed by what the compiler read of it, never by a structure the
 * theme works out from the text, and a table the compiler read none of takes the look every cell starts from for its
 * body.
 */
class CompiledReadsTest {

    private static final String SHEET = "Compiled";

    /** A theme that gives every kind of table a look of its own, so the base tells apart from any look. */
    private static final TableThemeService LOOKS = TestThemes.everyKind();

    /** The looks that theme gives. */
    private static final String WHITE = "#ffffff";
    private static final String BLACK = "#000000";
    private static final String MUTED = "#808080";
    private static final String LIGHT_BLUE = "#ddebf7";

    /** A Datatype whose parent type is not found: the compiler reads none of its fields. */
    private static final int ORPHAN_ROW = 1;

    /** A Datatype written transposed whose parent type is not found. */
    private static final int WIDE_ROW = 6;

    /**
     * A Datatype written as a Vocabulary with no space before the type of its values: the compiler binds a Datatype of
     * one field written transposed, its type and its name in a column.
     */
    private static final int GENDER_ROW = 11;

    /** A Vocabulary of a type that does not exist: the compiler binds nothing. */
    private static final int CODE_ROW = 16;

    /** A Spreadsheet whose header names a parameter by its type alone: the compiler reads none of its steps. */
    private static final int LOST_ROW = 21;

    /** A Spreadsheet whose last row repeats the name of the step above it: the compiler reads no step there. */
    private static final int DOUBLED_ROW = 26;

    /** A TBasic table running an operation that does not exist: the compiler builds none of its steps. */
    private static final int JUMPING_ROW = 33;

    /** A ColumnMatch table of an algorithm that does not exist. */
    private static final int FUZZY_ROW = 40;

    private static final int NAMES = 1;

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, CompiledReadsTest::fillSheet);
    }

    @Test
    void givesADatatypeWhoseParentIsNotFoundTheBaseAlone() {
        var layout = layoutAt(ORPHAN_ROW);

        // Read, the name would be filled and the type muted.
        assertEquals(WHITE, layout.at(ORPHAN_ROW + 1, 2).style().background().rgb());
        assertEquals(BLACK, layout.at(ORPHAN_ROW + 1, 1).style().color().rgb());
        var header = "Datatype Orphan extends Missing";
        var name = layout.at(ORPHAN_ROW, 1).runs(header).stream()
                .filter(run -> "Orphan".equals(header.substring(run.start(), run.end())))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.TRUE, name.style().bold(), "The header keeps the theme");
    }

    @Test
    void givesATransposedDatatypeWhoseParentIsNotFoundTheBaseAlone() {
        var layout = layoutAt(WIDE_ROW);

        for (var column = 1; column <= 2; column++) {
            assertEquals(BLACK, layout.at(WIDE_ROW + 1, column).style().color().rgb(), "column " + column);
            assertEquals(WHITE, layout.at(WIDE_ROW + 2, column).style().background().rgb(), "column " + column);
        }
    }

    @Test
    void readsADatatypeTheCompilerBindsAsADatatypeAsADatatype() {
        var layout = layoutAt(GENDER_ROW);

        // No space before the type: the header names a Datatype, and its column holds the type and the name of a field.
        assertEquals(MUTED, layout.at(GENDER_ROW + 1, 1).style().color().rgb(), "The type");
        assertEquals(LIGHT_BLUE, layout.at(GENDER_ROW + 2, 1).style().background().rgb(), "The name");
    }

    @Test
    void givesAVocabularyOfATypeThatDoesNotExistTheBaseAlone() {
        var standard = new TableThemeService();

        var layout = standard.layoutOf(tableAt(CODE_ROW));

        // The shipped theme centres the values of a Vocabulary; the compiler read none of these.
        assertEquals(ThemeHorizontalAlign.LEFT, layout.at(CODE_ROW + 1, 1).style().align());
    }

    @Test
    void givesASpreadsheetTheCompilerReadNoneOfTheBaseAlone() {
        var layout = layoutAt(LOST_ROW);

        assertEquals(WHITE, layout.at(LOST_ROW + 1, 2).style().background().rgb(), "The titles are not blue");
        assertEquals(WHITE, layout.at(LOST_ROW + 2, 2).style().background().rgb(), "The values are not filled");
    }

    @Test
    void returnsTheStepTheCompilerReturnsNotTheLastRowOfTheSheet() {
        var layout = layoutAt(DOUBLED_ROW);
        var total = DOUBLED_ROW + 3;

        // The step the compiler returns: its name is bold and lines set its row apart.
        assertEquals(Boolean.TRUE, layout.at(total, 1).style().bold());
        assertEquals(ThemeLineStyle.THIN, layout.at(total, 2).style().border().top().style());
        // The row repeating its name is no step: neither the result, nor marked for it.
        assertEquals(Boolean.FALSE, layout.at(total + 1, 1).style().bold());
        assertEquals(ThemeLineStyle.NONE, layout.at(total + 1, 2).style().border().top().style());
    }

    @Test
    void givesATBasicTableTheCompilerBuiltNoStepsOfTheBaseAlone() {
        var layout = layoutAt(JUMPING_ROW);

        assertEquals(BLACK, layout.at(JUMPING_ROW + 1, 1).style().color().rgb(), "The ids are not code");
        assertEquals(WHITE, layout.at(JUMPING_ROW + 3, 4).style().background().rgb(), "The actions are not filled");
    }

    @Test
    void givesAColumnMatchTableOfAnAlgorithmThatDoesNotExistTheBaseAlone() {
        var layout = layoutAt(FUZZY_ROW);

        assertEquals(BLACK, layout.at(FUZZY_ROW + 1, NAMES).style().color().rgb(), "The ids are not code");
        assertEquals(WHITE, layout.at(FUZZY_ROW + 2, NAMES).style().background().rgb(), "The titles are not grey");
    }

    private IOpenLTable tableAt(int row) {
        return TableTestProjects.tableAt(dir, row);
    }

    private ThemedTable layoutAt(int row) {
        return LOOKS.layoutOf(tableAt(row));
    }

    /** A table of every kind the compiler reads in a way of its own, or reads none of. */
    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, ORPHAN_ROW, 1, "Datatype Orphan extends Missing");
        merge(sheet, ORPHAN_ROW, ORPHAN_ROW, 1, 2);
        TableTestProjects.row(sheet, ORPHAN_ROW + 1, 1, "String", "name");
        TableTestProjects.row(sheet, ORPHAN_ROW + 2, 1, "Integer", "age");

        TableTestProjects.row(sheet, WIDE_ROW, 1, "Datatype Wide extends Missing");
        merge(sheet, WIDE_ROW, WIDE_ROW, 1, 2);
        TableTestProjects.row(sheet, WIDE_ROW + 1, 1, "String", "Integer");
        TableTestProjects.row(sheet, WIDE_ROW + 2, 1, "name", "age");

        TableTestProjects.row(sheet, GENDER_ROW, 1, "Datatype Gender<String>");
        TableTestProjects.row(sheet, GENDER_ROW + 1, 1, "Male");
        TableTestProjects.row(sheet, GENDER_ROW + 2, 1, "Female");

        TableTestProjects.row(sheet, CODE_ROW, 1, "Datatype Code <Strng>");
        TableTestProjects.row(sheet, CODE_ROW + 1, 1, "A");
        TableTestProjects.row(sheet, CODE_ROW + 2, 1, "B");

        TableTestProjects.row(sheet, LOST_ROW, 1, "Spreadsheet Double Lost ( Integer )");
        merge(sheet, LOST_ROW, LOST_ROW, 1, 2);
        TableTestProjects.row(sheet, LOST_ROW + 1, 1, "Step", "Formula");
        TableTestProjects.row(sheet, LOST_ROW + 2, 1, "Base", "= 1");

        TableTestProjects.row(sheet, DOUBLED_ROW, 1, "Spreadsheet Double Doubled ( )");
        merge(sheet, DOUBLED_ROW, DOUBLED_ROW, 1, 2);
        TableTestProjects.row(sheet, DOUBLED_ROW + 1, 1, "Step", "Formula");
        TableTestProjects.row(sheet, DOUBLED_ROW + 2, 1, "Base", "= 1");
        TableTestProjects.row(sheet, DOUBLED_ROW + 3, 1, "Total*", "= $Base");
        TableTestProjects.row(sheet, DOUBLED_ROW + 4, 1, "Total*", "= 2");

        TableTestProjects.row(sheet, JUMPING_ROW, 1, "TBasic Integer Jumping ( Integer n )");
        merge(sheet, JUMPING_ROW, JUMPING_ROW, 1, 4);
        TableTestProjects.row(sheet, JUMPING_ROW + 1, 1, "Label", "Operation", "Condition", "Action");
        TableTestProjects.row(sheet, JUMPING_ROW + 2, 1, "Name", "Operation", "Condition", "Action");
        TableTestProjects.row(sheet, JUMPING_ROW + 3, 1, null, "JUMP", null, "1");
        TableTestProjects.row(sheet, JUMPING_ROW + 4, 1, null, "RETURN", "n", null);

        TableTestProjects.row(sheet, FUZZY_ROW, NAMES, "ColumnMatch <FUZZY> String Fuzzy (Integer age)");
        merge(sheet, FUZZY_ROW, FUZZY_ROW, NAMES, 3);
        TableTestProjects.row(sheet, FUZZY_ROW + 1, NAMES, "names", "operation", "values");
        TableTestProjects.row(sheet, FUZZY_ROW + 2, NAMES, "Attribute", "Condition", "Rating");
        TableTestProjects.row(sheet, FUZZY_ROW + 3, NAMES, "Return Values", null, "High");
        TableTestProjects.row(sheet, FUZZY_ROW + 4, NAMES, "age", "min", "40");

    }
}
