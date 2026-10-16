package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableSignaturePart;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the rule every layout follows: a table is laid out by what the compiler read of it, and a table the compiler
 * read none of by the parts its text writes where its kind writes them.
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

    /** A Data table of a type that does not exist, written transposed. */
    private static final int ITEMS_ROW = 47;

    /** A Test table of a rule that does not exist. */
    private static final int CHECK_ROW = 51;

    /** A Run table of a rule that does not exist, written transposed, one field taken from a Data table. */
    private static final int CALL_ROW = 57;

    /** A Rules table whose parameter is of a type that does not exist. */
    private static final int PICK_ROW = 61;

    /** A SimpleRules table whose parameter is of a type that does not exist. */
    private static final int MATCH_ROW = 70;

    /** A SimpleLookup table whose parameter is of a type that does not exist. */
    private static final int GRID_ROW = 75;

    /** A Conditions table of three parts, not the four the compiler reads. */
    private static final int PARTS_ROW = 80;

    /** A SpreadsheetResult the compiler read none of, a step and a column of it marked. */
    private static final int MARKED_ROW = 84;

    /** A Rules table of a missing type written with a rule in each column. */
    private static final int TURNED_ROW = 89;

    /** A Spreadsheet the compiler read none of that returns nothing. */
    private static final int QUIET_ROW = 93;

    /** A SpreadsheetResult the compiler read none of with a step named RETURN. */
    private static final int NAMED_ROW = 97;

    /** A Constants table of a type that does not exist. */
    private static final int LIMITS_ROW = 102;

    /** A Datatype whose parent type is not found, its columns titled in another order. */
    private static final int TITLED_ROW = 105;

    /** An Actions table of three parts, named by keywords down its first column. */
    private static final int STEPS_ROW = 109;

    /** A Datatype whose parent type is not found, written transposed, its fields of primitive types. */
    private static final int PRIMITIVES_ROW = 122;

    /** A ColumnMatch table of a missing type, a condition indented under the one above it. */
    private static final int GROUPED_ROW = 126;

    private static final int NAMES = 1;

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, CompiledReadsTest::fillSheet);
    }

    @Test
    void readsTheFieldsOfADatatypeWhoseParentIsNotFoundFromItsText() {
        var roles = Roles.of(tableAt(ORPHAN_ROW));

        assertEquals(RawTableRole.CODE, roles.roleAt(ORPHAN_ROW + 1, 1), "The type");
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(ORPHAN_ROW + 1, 2), "The name");
        assertEquals(RawTableRole.VALUE, roles.roleAt(ORPHAN_ROW + 1, 3), "The default");
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
    void readsATransposedDatatypeWhoseParentIsNotFoundByTheRowOfItsTypes() {
        var roles = Roles.of(tableAt(WIDE_ROW));

        for (var column = 1; column <= 2; column++) {
            assertEquals(RawTableRole.CODE, roles.roleAt(WIDE_ROW + 1, column), "column " + column);
            assertEquals(RawTableRole.OUTPUT, roles.roleAt(WIDE_ROW + 2, column), "column " + column);
        }
    }

    @Test
    void readsADatatypeTheCompilerBindsAsADatatypeAsADatatype() {
        var roles = Roles.of(tableAt(GENDER_ROW));

        // No space before the type: the header names a Datatype, and its column holds the type and the name of a field.
        assertEquals(RawTableRole.CODE, roles.roleAt(GENDER_ROW + 1, 1), "The type");
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(GENDER_ROW + 2, 1), "The name");
    }

    @Test
    void readsAVocabularyOfATypeThatDoesNotExistAsAVocabulary() {
        var table = tableAt(CODE_ROW);
        var roles = Roles.of(table);

        assertEquals(LayoutKind.VOCABULARY, LayoutKind.of(table));
        assertEquals(RawTableRole.VALUE, roles.roleAt(CODE_ROW + 1, 1));
        assertEquals(RawTableRole.VALUE, roles.roleAt(CODE_ROW + 2, 1));
    }

    @Test
    void readsTheStepsOfASpreadsheetTheCompilerReadNoneOfFromItsText() {
        var roles = Roles.of(tableAt(LOST_ROW));

        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(LOST_ROW + 1, 1), "The title of the steps");
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(LOST_ROW + 1, 2), "The title of a column");
        assertEquals(RawTableRole.VALUE, roles.roleAt(LOST_ROW + 2, 1), "A step");
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(LOST_ROW + 2, 2), "Its value");
        // A Spreadsheet returning a single value returns its last step.
        assertEquals(RawTableRole.RESULT, roles.roleAt(LOST_ROW + 3, 1));
        assertEquals(RawTableRole.RESULT, roles.roleAt(LOST_ROW + 3, 2));
    }

    @Test
    void returnsTheMarkedStepsAndColumnsOfASpreadsheetResultTheCompilerReadNoneOf() {
        var roles = Roles.of(tableAt(MARKED_ROW));

        assertEquals(RawTableRole.RESULT, roles.roleAt(MARKED_ROW + 1, 2), "The marked column");
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(MARKED_ROW + 1, 3));
        assertEquals(RawTableRole.VALUE, roles.roleAt(MARKED_ROW + 2, 1));
        assertEquals(RawTableRole.RESULT, roles.roleAt(MARKED_ROW + 3, 1), "The marked step");
        assertEquals(RawTableRole.RESULT, roles.roleAt(MARKED_ROW + 3, 2));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(MARKED_ROW + 3, 3), "A column not marked");
    }

    @Test
    void returnsTheStepTheCompilerReturnsNotTheLastRowOfTheSheet() {
        var roles = Roles.of(tableAt(DOUBLED_ROW));
        var total = DOUBLED_ROW + 3;

        assertEquals(RawTableRole.RESULT, roles.roleAt(total, 1), "The step the compiler returns");
        assertEquals(RawTableRole.RESULT, roles.roleAt(total, 2));
        // The row repeating its name is no step the compiler returns.
        assertEquals(RawTableRole.VALUE, roles.roleAt(total + 1, 1));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(total + 1, 2));
    }

    @Test
    void readsATBasicTableTheCompilerBuiltNoStepsOfFromItsIds() {
        var roles = Roles.of(tableAt(JUMPING_ROW));

        assertEquals(RawTableRole.CODE, roles.roleAt(JUMPING_ROW + 1, 1), "The ids");
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(JUMPING_ROW + 2, 1));
        assertEquals(RawTableRole.VALUE, roles.roleAt(JUMPING_ROW + 3, 4), "An action");
        assertEquals(RawTableRole.RESULT, roles.roleAt(JUMPING_ROW + 4, 2), "The step returning");
    }

    @Test
    void readsAColumnMatchOfAnUnknownAlgorithmAsMatch() {
        var roles = Roles.of(tableAt(FUZZY_ROW));

        assertEquals(RawTableRole.CODE, roles.roleAt(FUZZY_ROW + 1, NAMES), "The ids");
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(FUZZY_ROW + 2, NAMES));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(FUZZY_ROW + 3, NAMES), "The row of return values");
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(FUZZY_ROW + 3, 3));
        assertEquals(RawTableRole.KEY, roles.roleAt(FUZZY_ROW + 4, NAMES), "The name a condition checks");
        assertEquals(RawTableRole.VALUE, roles.roleAt(FUZZY_ROW + 4, 3));
    }

    @Test
    void readsATransposedDataTableOfAMissingTypeByItsFieldNames() {
        var roles = Roles.of(tableAt(ITEMS_ROW));

        assertEquals(RawTableRole.CODE, roles.roleAt(ITEMS_ROW + 1, 1), "A field name");
        assertEquals(RawTableRole.CODE, roles.roleAt(ITEMS_ROW + 2, 1));
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(ITEMS_ROW + 1, 2));
        assertEquals(RawTableRole.KEY, roles.roleAt(ITEMS_ROW + 1, 3), "The first field is the key");
        assertEquals(RawTableRole.VALUE, roles.roleAt(ITEMS_ROW + 2, 3));
    }

    @Test
    void readsATestOfAMissingRuleByItsFieldNames() {
        var roles = Roles.of(tableAt(CHECK_ROW));

        assertEquals(RawTableRole.CODE, roles.roleAt(CHECK_ROW + 1, 1));
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(CHECK_ROW + 2, 1));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(CHECK_ROW + 2, 2), "The title of the expected result");
        assertEquals(RawTableRole.VALUE, roles.roleAt(CHECK_ROW + 3, 1));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(CHECK_ROW + 3, 2));
    }

    @Test
    void readsTheKeysOfATransposedRunOfAMissingRuleByItsReferences() {
        var roles = Roles.of(tableAt(CALL_ROW));

        assertEquals(RawTableRole.CODE, roles.roleAt(CALL_ROW + 1, 1), "The field");
        assertEquals(RawTableRole.CODE, roles.roleAt(CALL_ROW + 1, 2), "The reference");
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(CALL_ROW + 1, 3));
        assertEquals(RawTableRole.KEY, roles.roleAt(CALL_ROW + 1, 4), "The key of a row of a Data table");
        assertEquals(RawTableRole.VALUE, roles.roleAt(CALL_ROW + 2, 4));
    }

    @Test
    void readsARulesTableOfAMissingTypeByTheKindsOfItsColumns() {
        var roles = Roles.of(tableAt(PICK_ROW));

        for (var row = PICK_ROW + 1; row <= PICK_ROW + 3; row++) {
            assertEquals(RawTableRole.CODE, roles.roleAt(row, 2), "row " + row);
        }
        assertEquals(RawTableRole.KEY, roles.roleAt(PICK_ROW + 5, 1), "The name of a rule");
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(PICK_ROW + 4, 2));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(PICK_ROW + 4, 3));
        assertEquals(RawTableRole.VALUE, roles.roleAt(PICK_ROW + 5, 2));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(PICK_ROW + 5, 3));
    }

    @Test
    void readsTheConditionsOfASimpleRulesTableOfAMissingTypeByItsParameters() {
        var roles = Roles.of(tableAt(MATCH_ROW));

        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(MATCH_ROW + 1, 1));
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(MATCH_ROW + 1, 2));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(MATCH_ROW + 1, 3));
        assertEquals(RawTableRole.VALUE, roles.roleAt(MATCH_ROW + 2, 2));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(MATCH_ROW + 2, 3));
    }

    @Test
    void readsALookupOfAMissingTypeByItsHorizontalConditions() {
        var roles = Roles.of(tableAt(GRID_ROW));

        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(GRID_ROW + 1, 1), "The vertical condition");
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(GRID_ROW + 1, 2), "A horizontal value");
        assertEquals(RawTableRole.VALUE, roles.roleAt(GRID_ROW + 2, 1));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(GRID_ROW + 2, 2), "The grid it returns");
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(GRID_ROW + 2, 3));
    }

    @Test
    void readsTheTitlesOfADeclarationTheCompilerReadNoneOfByTheirKeyword() {
        var roles = Roles.of(tableAt(PARTS_ROW));

        assertEquals(RawTableRole.KEY, roles.roleAt(PARTS_ROW + 1, 1), "A keyword");
        assertEquals(RawTableRole.CODE, roles.roleAt(PARTS_ROW + 2, 1), "The inputs");
        assertEquals(RawTableRole.CODE, roles.roleAt(PARTS_ROW + 2, 2), "The expression");
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(PARTS_ROW + 2, 3), "The title");
    }

    @Test
    void readsARulesTableOfAMissingTypeWithARuleInEachColumn() {
        var roles = Roles.of(tableAt(TURNED_ROW));

        assertEquals(RawTableRole.CODE, roles.roleAt(TURNED_ROW + 1, 1), "The kind of a condition");
        assertEquals(RawTableRole.CODE, roles.roleAt(TURNED_ROW + 2, 3), "The parameter of a return");
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(TURNED_ROW + 1, 4));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(TURNED_ROW + 2, 4));
        assertEquals(RawTableRole.VALUE, roles.roleAt(TURNED_ROW + 1, 5));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(TURNED_ROW + 2, 6));
    }

    @Test
    void returnsWhatTheTypeAndTheNamesOfASpreadsheetTheCompilerReadNoneOfTell() {
        var quiet = Roles.of(tableAt(QUIET_ROW));
        assertEquals(RawTableRole.VALUE, quiet.roleAt(QUIET_ROW + 2, 1), "A void Spreadsheet returns nothing");
        assertEquals(RawTableRole.OUTPUT, quiet.roleAt(QUIET_ROW + 2, 2));

        var named = Roles.of(tableAt(NAMED_ROW));
        assertEquals(RawTableRole.RESULT, named.roleAt(NAMED_ROW + 2, 1), "The step named RETURN");
        assertEquals(RawTableRole.RESULT, named.roleAt(NAMED_ROW + 2, 2));
        assertEquals(RawTableRole.OUTPUT, named.roleAt(NAMED_ROW + 3, 2), "The last step is not returned");
    }

    @Test
    void readsTheConstantsOfAMissingTypeByTheirPlace() {
        var roles = Roles.of(tableAt(LIMITS_ROW));

        assertEquals(RawTableRole.CODE, roles.roleAt(LIMITS_ROW + 1, 1));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(LIMITS_ROW + 1, 2), "The name of a constant");
        assertEquals(RawTableRole.VALUE, roles.roleAt(LIMITS_ROW + 1, 3));
    }

    @Test
    void readsTheColumnsOfADatatypeWhoseParentIsNotFoundByTheirTitles() {
        var roles = Roles.of(tableAt(TITLED_ROW));

        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(TITLED_ROW + 1, 1));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(TITLED_ROW + 2, 1), "The column titled Name");
        assertEquals(RawTableRole.CODE, roles.roleAt(TITLED_ROW + 2, 2), "The column titled Type");
        assertEquals(RawTableRole.VALUE, roles.roleAt(TITLED_ROW + 2, 3));
    }

    @Test
    void readsTheTitlesOfADeclarationNamingItsPartsDownItsFirstColumn() {
        var roles = Roles.of(tableAt(STEPS_ROW));

        assertEquals(RawTableRole.KEY, roles.roleAt(STEPS_ROW + 1, 1), "A keyword");
        assertEquals(RawTableRole.CODE, roles.roleAt(STEPS_ROW + 1, 2), "The inputs");
        assertEquals(RawTableRole.CODE, roles.roleAt(STEPS_ROW + 2, 2), "The expression");
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(STEPS_ROW + 3, 2), "The title");
    }

    @Test
    void readsATransposedDatatypeOfPrimitiveTypesByTheRowOfItsTypes() {
        var roles = Roles.of(tableAt(PRIMITIVES_ROW));

        assertEquals(RawTableRole.CODE, roles.roleAt(PRIMITIVES_ROW + 1, 2), "The type");
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(PRIMITIVES_ROW + 2, 2), "The name");
    }

    @Test
    void groupsTheConditionsOfAColumnMatchTheCompilerReadNoneOfByTheirIndent() {
        var roles = Roles.of(tableAt(GROUPED_ROW));

        // The first condition heads a group with the one indented under it; the condition after them starts anew.
        assertEquals(Set.of(4, 6), Set.copyOf(roles.regions().groups().rows()));
    }

    private IOpenLTable tableAt(int row) {
        return TableTestProjects.tableAt(dir, row);
    }

    /** A table of every kind the compiler reads in a way of its own, or reads none of. */
    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, ORPHAN_ROW, 1, "Datatype Orphan extends Missing");
        merge(sheet, ORPHAN_ROW, ORPHAN_ROW, 1, 3);
        TableTestProjects.row(sheet, ORPHAN_ROW + 1, 1, "String", "name", "Ann");
        TableTestProjects.row(sheet, ORPHAN_ROW + 2, 1, "Integer", "age", "7");

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
        TableTestProjects.row(sheet, LOST_ROW + 3, 1, "Total", "= $Base");

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
        // The ids are read in any case, as the compiler reads them.
        TableTestProjects.row(sheet, FUZZY_ROW + 1, NAMES, "Names", "Operation", "VALUES");
        TableTestProjects.row(sheet, FUZZY_ROW + 2, NAMES, "Attribute", "Condition", "Rating");
        TableTestProjects.row(sheet, FUZZY_ROW + 3, NAMES, "Return Values", null, "High");
        TableTestProjects.row(sheet, FUZZY_ROW + 4, NAMES, "age", "min", "40");

        TableTestProjects.row(sheet, ITEMS_ROW, 1, "Data Missing items");
        merge(sheet, ITEMS_ROW, ITEMS_ROW, 1, 4);
        TableTestProjects.row(sheet, ITEMS_ROW + 1, 1, "code", "Code", "A1", "A2");
        TableTestProjects.row(sheet, ITEMS_ROW + 2, 1, "price", "Price", "1", "2");

        TableTestProjects.row(sheet, CHECK_ROW, 1, "Test Missing MissingTest");
        merge(sheet, CHECK_ROW, CHECK_ROW, 1, 2);
        TableTestProjects.row(sheet, CHECK_ROW + 1, 1, "a", "_res_");
        TableTestProjects.row(sheet, CHECK_ROW + 2, 1, "A", "Result");
        TableTestProjects.row(sheet, CHECK_ROW + 3, 1, "1", "2");

        TableTestProjects.row(sheet, CALL_ROW, 1, "Run Missing MissingRun");
        merge(sheet, CALL_ROW, CALL_ROW, 1, 4);
        TableTestProjects.row(sheet, CALL_ROW + 1, 1, "order", ">orders", "Order", "small");
        TableTestProjects.row(sheet, CALL_ROW + 2, 1, "count", null, "Count", "3");

        TableTestProjects.row(sheet, PICK_ROW, 1, "Rules String Pick(Missing m, Integer a)");
        merge(sheet, PICK_ROW, PICK_ROW, 1, 3);
        TableTestProjects.row(sheet, PICK_ROW + 1, 1, "RULE", "C1", "RET1");
        TableTestProjects.row(sheet, PICK_ROW + 2, 1, null, "a > n", "r");
        TableTestProjects.row(sheet, PICK_ROW + 3, 1, null, "Integer n", "String r");
        TableTestProjects.row(sheet, PICK_ROW + 4, 1, "Rule", "Above", "Result");
        TableTestProjects.row(sheet, PICK_ROW + 5, 1, "R1", "1", "x");
        TableTestProjects.row(sheet, PICK_ROW + 6, 1, "R2", "2", "y");

        TableTestProjects.row(sheet, MATCH_ROW, 1, "SimpleRules String Match(Missing m, Integer a)");
        merge(sheet, MATCH_ROW, MATCH_ROW, 1, 3);
        TableTestProjects.row(sheet, MATCH_ROW + 1, 1, "M", "A", "Result");
        TableTestProjects.row(sheet, MATCH_ROW + 2, 1, "x", "1", "y");

        TableTestProjects.row(sheet, GRID_ROW, 1, "SimpleLookup Double Grid(Missing m, Integer a)");
        merge(sheet, GRID_ROW, GRID_ROW, 1, 3);
        TableTestProjects.row(sheet, GRID_ROW + 1, 1, "M", "1", "2");
        TableTestProjects.row(sheet, GRID_ROW + 2, 1, "x", "0.1", "0.2");

        TableTestProjects.row(sheet, PARTS_ROW, 1, "Conditions Parts");
        merge(sheet, PARTS_ROW, PARTS_ROW, 1, 3);
        TableTestProjects.row(sheet, PARTS_ROW + 1, 1, "Inputs", "Expression", "Title");
        TableTestProjects.row(sheet, PARTS_ROW + 2, 1, "Integer a", "a > 1", "Above");

        TableTestProjects.row(sheet, MARKED_ROW, 1, "Spreadsheet SpreadsheetResult Marked ( Missing m )");
        merge(sheet, MARKED_ROW, MARKED_ROW, 1, 3);
        TableTestProjects.row(sheet, MARKED_ROW + 1, 1, "Step", "Value*", "Note");
        TableTestProjects.row(sheet, MARKED_ROW + 2, 1, "Base", "= 1", "a");
        TableTestProjects.row(sheet, MARKED_ROW + 3, 1, "Total*", "= $Value$Base", "b");

        TableTestProjects.row(sheet, TURNED_ROW, 1, "Rules String Turned(Missing m, Integer a)");
        merge(sheet, TURNED_ROW, TURNED_ROW, 1, 6);
        TableTestProjects.row(sheet, TURNED_ROW + 1, 1, "C1", "a > n", "Integer n", "Above", "1", "2");
        TableTestProjects.row(sheet, TURNED_ROW + 2, 1, "RET1", "r", "String r", "Result", "x", "y");

        TableTestProjects.row(sheet, QUIET_ROW, 1, "Spreadsheet void Quiet ( Missing m )");
        merge(sheet, QUIET_ROW, QUIET_ROW, 1, 2);
        TableTestProjects.row(sheet, QUIET_ROW + 1, 1, "Step", "Value");
        TableTestProjects.row(sheet, QUIET_ROW + 2, 1, "Base", "= 1");

        TableTestProjects.row(sheet, NAMED_ROW, 1, "Spreadsheet SpreadsheetResult Named ( Missing m )");
        merge(sheet, NAMED_ROW, NAMED_ROW, 1, 2);
        TableTestProjects.row(sheet, NAMED_ROW + 1, 1, "Step", "Value");
        TableTestProjects.row(sheet, NAMED_ROW + 2, 1, "RETURN : Double", "= 1");
        TableTestProjects.row(sheet, NAMED_ROW + 3, 1, "Other", "= 2");

        TableTestProjects.row(sheet, LIMITS_ROW, 1, "Constants");
        merge(sheet, LIMITS_ROW, LIMITS_ROW, 1, 3);
        TableTestProjects.row(sheet, LIMITS_ROW + 1, 1, "Missing", "limit", "1");

        TableTestProjects.row(sheet, TITLED_ROW, 1, "Datatype Titled extends Missing");
        merge(sheet, TITLED_ROW, TITLED_ROW, 1, 3);
        TableTestProjects.row(sheet, TITLED_ROW + 1, 1, "Name", "Type", "Default");
        TableTestProjects.row(sheet, TITLED_ROW + 2, 1, "name", "String", "x");

        TableTestProjects.row(sheet, STEPS_ROW, 1, "Actions Steps");
        merge(sheet, STEPS_ROW, STEPS_ROW, 1, 2);
        TableTestProjects.row(sheet, STEPS_ROW + 1, 1, "Inputs", "Integer a");
        TableTestProjects.row(sheet, STEPS_ROW + 2, 1, "Expression", "a + 1");
        TableTestProjects.row(sheet, STEPS_ROW + 3, 1, "Title", "Plus");

        TableTestProjects.row(sheet, PRIMITIVES_ROW, 1, "Datatype Primitives extends Missing");
        merge(sheet, PRIMITIVES_ROW, PRIMITIVES_ROW, 1, 2);
        TableTestProjects.row(sheet, PRIMITIVES_ROW + 1, 1, "int", "double");
        TableTestProjects.row(sheet, PRIMITIVES_ROW + 2, 1, "count", "rate");

        TableTestProjects.row(sheet, GROUPED_ROW, NAMES, "ColumnMatch <MATCH> String Grade (Missing m)");
        merge(sheet, GROUPED_ROW, GROUPED_ROW, NAMES, 3);
        TableTestProjects.row(sheet, GROUPED_ROW + 1, NAMES, "names", "operation", "values");
        TableTestProjects.row(sheet, GROUPED_ROW + 2, NAMES, "Attribute", "Condition", "Grade");
        TableTestProjects.row(sheet, GROUPED_ROW + 3, NAMES, "Return Values", null, "A");
        TableTestProjects.row(sheet, GROUPED_ROW + 4, NAMES, "score", "min", "90");
        TableTestProjects.row(sheet, GROUPED_ROW + 5, NAMES, "bonus", "min", "1");
        TableTestProjects.row(sheet, GROUPED_ROW + 6, NAMES, "age", "min", "18");
        var indented = sheet.getWorkbook().createCellStyle();
        indented.setIndention((short) 1);
        sheet.getRow(GROUPED_ROW + 5).getCell(NAMES).setCellStyle(indented);
    }
}
