package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableSignaturePart;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the rule every layout follows: a table is laid out by what the compiler read of it, never by a structure
 * worked out from the text, and a table the compiler read none of holds values in its body.
 */
class CompiledReadsTest {

    private static final String SHEET = "Compiled";

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
    void readsTheBodyOfADatatypeWhoseParentIsNotFoundAsValues() {
        var roles = Roles.of(tableAt(ORPHAN_ROW));

        // Read, the type would be code and the name a key.
        assertEquals(RawTableRole.VALUE, roles.roleAt(ORPHAN_ROW + 1, 1));
        assertEquals(RawTableRole.VALUE, roles.roleAt(ORPHAN_ROW + 1, 2));
        // The header is split into its pieces as the header of every table is.
        var header = "Datatype Orphan extends Missing";
        var from = 0;
        var name = "";
        for (var piece : roles.regions().signature()) {
            if (piece.part() == RawTableSignaturePart.NAME) {
                name = header.substring(from, piece.to()).trim();
            }
            from = piece.to();
        }
        assertEquals("Orphan", name);
    }

    @Test
    void readsTheBodyOfATransposedDatatypeWhoseParentIsNotFoundAsValues() {
        var roles = Roles.of(tableAt(WIDE_ROW));

        for (var column = 1; column <= 2; column++) {
            assertEquals(RawTableRole.VALUE, roles.roleAt(WIDE_ROW + 1, column), "column " + column);
            assertEquals(RawTableRole.VALUE, roles.roleAt(WIDE_ROW + 2, column), "column " + column);
        }
    }

    @Test
    void readsADatatypeTheCompilerBindsAsADatatypeAsADatatype() {
        var roles = Roles.of(tableAt(GENDER_ROW));

        // No space before the type: the header names a Datatype, and its column holds the type and the name of a field.
        assertEquals(RawTableRole.CODE, roles.roleAt(GENDER_ROW + 1, 1), "The type");
        assertEquals(RawTableRole.KEY, roles.roleAt(GENDER_ROW + 2, 1), "The name");
    }

    @Test
    void readsTheBodyOfAVocabularyOfATypeThatDoesNotExistAsValues() {
        var roles = Roles.of(tableAt(CODE_ROW));

        assertEquals(RawTableRole.VALUE, roles.roleAt(CODE_ROW + 1, 1));
        assertEquals(RawTableRole.VALUE, roles.roleAt(CODE_ROW + 2, 1));
    }

    @Test
    void readsTheBodyOfASpreadsheetTheCompilerReadNoneOfAsValues() {
        var roles = Roles.of(tableAt(LOST_ROW));

        assertEquals(RawTableRole.VALUE, roles.roleAt(LOST_ROW + 1, 2), "The titles are no titles");
        assertEquals(RawTableRole.VALUE, roles.roleAt(LOST_ROW + 2, 2), "The values are not what it gives");
    }

    @Test
    void returnsTheStepTheCompilerReturnsNotTheLastRowOfTheSheet() {
        var roles = Roles.of(tableAt(DOUBLED_ROW));
        var total = DOUBLED_ROW + 3;

        assertEquals(RawTableRole.RESULT, roles.roleAt(total, 1), "The step the compiler returns");
        assertEquals(RawTableRole.RESULT, roles.roleAt(total, 2));
        // The row repeating its name is no step the compiler returns.
        assertEquals(RawTableRole.KEY, roles.roleAt(total + 1, 1));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(total + 1, 2));
    }

    @Test
    void readsTheBodyOfATBasicTableTheCompilerBuiltNoStepsOfAsValues() {
        var roles = Roles.of(tableAt(JUMPING_ROW));

        assertEquals(RawTableRole.VALUE, roles.roleAt(JUMPING_ROW + 1, 1), "The ids are not code");
        assertEquals(RawTableRole.VALUE, roles.roleAt(JUMPING_ROW + 3, 4), "The actions are plain values");
    }

    @Test
    void readsTheBodyOfATableTheCompilerReadNoneOfAsValues() {
        for (var row : List.of(ORPHAN_ROW, LOST_ROW, FUZZY_ROW)) {
            var roles = Roles.of(tableAt(row));
            assertEquals(RawTableRole.SIGNATURE, roles.roleAt(row, 1));
            assertEquals(RawTableRole.VALUE, roles.roleAt(row + 2, 1), "Row " + row);
        }
        var fuzzy = Roles.of(tableAt(FUZZY_ROW));
        assertEquals(RawTableRole.VALUE, fuzzy.roleAt(FUZZY_ROW + 1, NAMES), "The ids are not code");
    }

    private IOpenLTable tableAt(int row) {
        return TableTestProjects.tableAt(dir, row);
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
