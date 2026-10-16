package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableValueType;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the roles of each kind of decision table: a Rules table written either way round, a SimpleRules, a
 * SmartRules, a SimpleLookup and a SmartLookup table. It also covers the tables that declare what decision tables
 * take: a Conditions, an Actions and a Returns table, written either way round.
 */
class DecisionLayoutTest {

    private static final String SHEET = "Rules";

    /** A Rules table naming its rules, with a value of its first condition merged over the first two of them. */
    private static final String GREET = "Greet";
    private static final int GREET_ROW = 1;

    /**
     * A Rules table written the other way round, the kind, the code and the title of a column in each row, with a
     * value of its first condition merged over the first two rules.
     */
    private static final String GREET_ROUND = "GreetRound";
    private static final int GREET_ROUND_ROW = 11;

    /** A SimpleRules table whose first rule is written over two rows of the sheet. */
    private static final String GREETING = "Greeting";
    private static final int GREETING_ROW = 16;

    /** A SmartRules table naming its rules in its first column and returning a value of two fields. */
    private static final String HELLO = "Hello";
    private static final int HELLO_ROW = 23;

    /** A SimpleLookup table returning its values under one horizontal condition. */
    private static final String RATE = "Rate";
    private static final int RATE_ROW = 29;

    /** A SmartLookup table returning its values under two horizontal conditions. */
    private static final String FACTOR = "Factor";
    private static final int FACTOR_ROW = 36;

    /** A Rules table returning a type no table declares, so it does not compile. */
    private static final int BROKEN_ROW = 48;

    /**
     * A Conditions table as the Reference Guide writes it: each part in a row named in the first column, a condition
     * in each column. Its first condition takes two parameters, its inputs, its expression and its title merged over
     * both of them.
     */
    private static final int CONDITIONS_ROW = 52;

    /** An Actions table that names no part: its rows stand in the order the compiler expects them. */
    private static final int ACTIONS_ROW = 59;

    /** A Conditions table written the other way round, with each part in a column named in its first row. */
    private static final int HOURS_ROW = 66;

    /** A Returns table written with each part in a column, its titles named first. */
    private static final int RETURNS_ROW = 72;

    /** A Conditions table no declaration is read from: its three columns are no parts either way round. */
    private static final int UNDECLARED_ROW = 77;

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, DecisionLayoutTest::fillSheet);
    }

    @Test
    void readsTheRolesOfARulesTable() {
        var roles = Roles.of(table(GREET));

        assertEquals(RawTableRole.SIGNATURE, roles.roleAt(GREET_ROW, 3));
        for (var row = GREET_ROW + 1; row <= GREET_ROW + 3; row++) {
            assertEquals(RawTableRole.CODE, roles.roleAt(row, 2), "The kind, the expression and the parameters");
        }
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(GREET_ROW + 4, 1), "The title of the rule names");
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(GREET_ROW + 4, 2));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(GREET_ROW + 4, 4));
        assertEquals(RawTableRole.KEY, roles.roleAt(GREET_ROW + 5, 1), "The RULE column names the rules");
        assertEquals(RawTableRole.VALUE, roles.roleAt(GREET_ROW + 6, 3));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(GREET_ROW + 7, 4));
        assertEquals(RawTableValueType.NUMBER, roles.typeAt(GREET_ROW + 5, 3), "An Integer limit");
        assertEquals(RawTableValueType.STRING, roles.typeAt(GREET_ROW + 5, 2));
        assertNull(roles.typeAt(GREET_ROW + 5, 1), "A name holds no value of a type");
        // The rules sharing the merged Weekday make a group: the line after it is the only one, the first rule
        // standing right under the titles.
        assertEquals(new RawTableGroups(List.of(7), List.of()), roles.regions().groups());
    }

    @Test
    void readsTheRolesOfARulesTableWrittenTheOtherWayRound() {
        var roles = Roles.of(table(GREET_ROUND));

        assertEquals(RawTableRole.CODE, roles.roleAt(GREET_ROUND_ROW + 1, 1));
        assertEquals(RawTableRole.CODE, roles.roleAt(GREET_ROUND_ROW + 3, 3));
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(GREET_ROUND_ROW + 1, 4));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(GREET_ROUND_ROW + 3, 4));
        assertEquals(RawTableRole.VALUE, roles.roleAt(GREET_ROUND_ROW + 2, 6));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(GREET_ROUND_ROW + 3, 7));
        // A rule is a column: the merged Weekday groups the first two, so the column after them starts a group.
        assertEquals(new RawTableGroups(List.of(), List.of(6)), roles.regions().groups());
    }

    @Test
    void readsTheRolesOfASimpleRulesTableWithARuleOverTwoRows() {
        var roles = Roles.of(table(GREETING));

        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(GREETING_ROW + 1, 1));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(GREETING_ROW + 1, 2));
        assertEquals(RawTableRole.VALUE, roles.roleAt(GREETING_ROW + 3, 1), "A rule merged over two rows");
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(GREETING_ROW + 4, 2));
        // A rule written over two rows, with every value of it merged over both, is one rule, not a group.
        assertEquals(RawTableGroups.NONE, roles.regions().groups());
    }

    @Test
    void readsTheRolesOfASmartRulesTableNamingItsRules() {
        var roles = Roles.of(table(HELLO));

        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(HELLO_ROW + 1, 1));
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(HELLO_ROW + 1, 2));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(HELLO_ROW + 1, 3));
        assertEquals(RawTableRole.KEY, roles.roleAt(HELLO_ROW + 2, 1), "The rule names");
        assertEquals(RawTableRole.VALUE, roles.roleAt(HELLO_ROW + 3, 2));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(HELLO_ROW + 2, 3));
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(HELLO_ROW + 2, 4));
        assertEquals(RawTableValueType.NUMBER, roles.typeAt(HELLO_ROW + 2, 4), "The rank of the reply");
        assertEquals(RawTableGroups.NONE, roles.regions().groups());
    }

    @Test
    void readsTheRolesOfALookup() {
        var rate = Roles.of(table(RATE));

        assertEquals(RawTableRole.INPUT_TITLE, rate.roleAt(RATE_ROW + 1, 1), "A vertical title");
        assertEquals(RawTableRole.OUTPUT_TITLE, rate.roleAt(RATE_ROW + 1, 3), "A horizontal value");
        assertEquals(RawTableRole.VALUE, rate.roleAt(RATE_ROW + 3, 2), "A vertical value");
        assertEquals(RawTableRole.OUTPUT, rate.roleAt(RATE_ROW + 4, 4), "The return grid");
        assertEquals(RawTableValueType.NUMBER, rate.typeAt(RATE_ROW + 4, 4));
        assertEquals(new RawTableGroups(List.of(4), List.of()), rate.regions().groups());

        var factor = Roles.of(table(FACTOR));
        assertEquals(RawTableRole.OUTPUT_TITLE, factor.roleAt(FACTOR_ROW + 1, 2), "A merged horizontal value");
        assertEquals(RawTableRole.OUTPUT_TITLE, factor.roleAt(FACTOR_ROW + 2, 5), "A horizontal value of the second row");
        assertEquals(RawTableRole.OUTPUT, factor.roleAt(FACTOR_ROW + 3, 5));
        // Each row of the horizontal values is a region of its own, so a line sets it apart from the row above.
        var horizontals = factor.regions().regions().stream()
                .filter(region -> region.role() == RawTableRole.OUTPUT_TITLE)
                .toList();
        assertTrue(horizontals.stream().allMatch(region -> region.top() == region.bottom()), horizontals::toString);
        assertEquals(2, horizontals.stream().map(RawTableRegion::top).distinct().count(), horizontals::toString);
        // The columns under the merged 2 start a group of the horizontal values.
        assertEquals(new RawTableGroups(List.of(), List.of(3)), factor.regions().groups());
    }

    @Test
    void readsADecisionTableThatDidNotCompileByTheKindsOfItsColumns() {
        var roles = Roles.of(tableAt(BROKEN_ROW));

        assertEquals(RawTableRole.SIGNATURE, roles.roleAt(BROKEN_ROW, 1));
        assertEquals(RawTableRole.CODE, roles.roleAt(BROKEN_ROW + 1, 1), "The kind of a column");
        assertEquals(RawTableRole.CODE, roles.roleAt(BROKEN_ROW + 2, 2), "An expression");
    }

    @Test
    void readsTheRolesOfTheDeclarationsOfConditionsActionsAndReturns() {
        var conditions = Roles.of(tableAt(CONDITIONS_ROW));
        assertEquals(RawTableRole.KEY, conditions.roleAt(CONDITIONS_ROW + 1, 1), "A keyword names a part");
        assertEquals(RawTableRole.KEY, conditions.roleAt(CONDITIONS_ROW + 4, 1));
        assertEquals(RawTableRole.CODE, conditions.roleAt(CONDITIONS_ROW + 2, 2));
        assertEquals(RawTableRole.CODE, conditions.roleAt(CONDITIONS_ROW + 3, 4));
        assertEquals(RawTableRole.INPUT_TITLE, conditions.roleAt(CONDITIONS_ROW + 4, 2));

        var actions = Roles.of(tableAt(ACTIONS_ROW));
        assertEquals(RawTableRole.CODE, actions.roleAt(ACTIONS_ROW + 1, 1), "No keyword: the inputs are code");
        assertEquals(RawTableRole.OUTPUT_TITLE, actions.roleAt(ACTIONS_ROW + 4, 2));

        var hours = Roles.of(tableAt(HOURS_ROW));
        assertEquals(RawTableRole.KEY, hours.roleAt(HOURS_ROW + 1, 2), "A keyword over a part in a column");
        assertEquals(RawTableRole.CODE, hours.roleAt(HOURS_ROW + 2, 2));
        assertEquals(RawTableRole.INPUT_TITLE, hours.roleAt(HOURS_ROW + 3, 4));

        var returns = Roles.of(tableAt(RETURNS_ROW));
        assertEquals(RawTableRole.KEY, returns.roleAt(RETURNS_ROW + 1, 1));
        assertEquals(RawTableRole.OUTPUT_TITLE, returns.roleAt(RETURNS_ROW + 2, 1));
        assertEquals(RawTableRole.CODE, returns.roleAt(RETURNS_ROW + 2, 3));

        var undeclared = Roles.of(tableAt(UNDECLARED_ROW));
        // No declaration is read: the parts stand in columns, and the last of them holds the titles.
        assertEquals(RawTableRole.CODE, undeclared.roleAt(UNDECLARED_ROW + 1, 2));
        assertEquals(RawTableRole.INPUT_TITLE, undeclared.roleAt(UNDECLARED_ROW + 1, 3));
    }

    private IOpenLTable table(String name) {
        return TableTestProjects.table(TableTestProjects.projectModel(dir), name);
    }

    /** The table whose header stands in the given row: a table that did not compile has no name to find it by. */
    private IOpenLTable tableAt(int row) {
        return TableTestProjects.tableAt(dir, row);
    }

    /**
     * A Rules table written each way round, a SimpleRules, a SmartRules, a SimpleLookup and a SmartLookup table, the
     * Datatype the SmartRules returns, a Rules table that does not compile, and the tables that declare what decision
     * tables take.
     */
    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, GREET_ROW, 1, "Rules String " + GREET + " ( String day, Integer hour )");
        merge(sheet, GREET_ROW, GREET_ROW, 1, 4);
        TableTestProjects.row(sheet, GREET_ROW + 1, 1, "RULE", "C1", "C2", "RET1");
        TableTestProjects.row(sheet, GREET_ROW + 2, 1, null, "day == dayName", "hour < limit", "greeting");
        TableTestProjects.row(sheet, GREET_ROW + 3, 1, null, "String dayName", "Integer limit", "String greeting");
        TableTestProjects.row(sheet, GREET_ROW + 4, 1, "Rule", "Day", "Before", "Greeting");
        TableTestProjects.row(sheet, GREET_ROW + 5, 1, "R1", "Weekday", "12", "Good Morning");
        TableTestProjects.row(sheet, GREET_ROW + 6, 1, "R2", null, "24", "Good Evening");
        merge(sheet, GREET_ROW + 5, GREET_ROW + 6, 2, 2);
        TableTestProjects.row(sheet, GREET_ROW + 7, 1, "R3", "Weekend", "24", "Rest");

        TableTestProjects.row(sheet, GREET_ROUND_ROW, 1,
                "Rules String " + GREET_ROUND + " ( String day, Integer hour )");
        merge(sheet, GREET_ROUND_ROW, GREET_ROUND_ROW, 1, 7);
        TableTestProjects.row(sheet, GREET_ROUND_ROW + 1, 1, "C1", "day == dayName", "String dayName", "Day",
                "Weekday", null, "Weekend");
        merge(sheet, GREET_ROUND_ROW + 1, GREET_ROUND_ROW + 1, 5, 6);
        TableTestProjects.row(sheet, GREET_ROUND_ROW + 2, 1, "C2", "hour < limit", "Integer limit", "Before", "12",
                "24", "24");
        TableTestProjects.row(sheet, GREET_ROUND_ROW + 3, 1, "RET1", "greeting", "String greeting", "Greeting",
                "Good Morning", "Good Evening", "Rest");

        TableTestProjects.row(sheet, GREETING_ROW, 1, "SimpleRules String " + GREETING + " ( Integer hour )");
        merge(sheet, GREETING_ROW, GREETING_ROW, 1, 2);
        TableTestProjects.row(sheet, GREETING_ROW + 1, 1, "Hour", "Greeting");
        TableTestProjects.row(sheet, GREETING_ROW + 2, 1, "< 12", "Good Morning");
        TableTestProjects.row(sheet, GREETING_ROW + 3, 1, null, null);
        merge(sheet, GREETING_ROW + 2, GREETING_ROW + 3, 1, 1);
        merge(sheet, GREETING_ROW + 2, GREETING_ROW + 3, 2, 2);
        TableTestProjects.row(sheet, GREETING_ROW + 4, 1, ">= 12", "Good Day");

        TableTestProjects.row(sheet, HELLO_ROW, 1, "SmartRules Reply " + HELLO + " ( Integer hour )");
        merge(sheet, HELLO_ROW, HELLO_ROW, 1, 4);
        TableTestProjects.row(sheet, HELLO_ROW + 1, 1, "Rule", "Hour", "Text", "Rank");
        TableTestProjects.row(sheet, HELLO_ROW + 2, 1, "Morning", "< 12", "Good Morning", "1");
        TableTestProjects.row(sheet, HELLO_ROW + 3, 1, "Day", ">= 12", "Good Day", "2");

        TableTestProjects.row(sheet, RATE_ROW, 1,
                "SimpleLookup Double " + RATE + " ( String band, String day, Integer year )");
        merge(sheet, RATE_ROW, RATE_ROW, 1, 4);
        TableTestProjects.row(sheet, RATE_ROW + 1, 1, "Band", "Day", "1", "2");
        TableTestProjects.row(sheet, RATE_ROW + 2, 1, "A", "Weekday", "1.1", "1.2");
        TableTestProjects.row(sheet, RATE_ROW + 3, 1, null, "Weekend", "1.3", "1.4");
        merge(sheet, RATE_ROW + 2, RATE_ROW + 3, 1, 1);
        TableTestProjects.row(sheet, RATE_ROW + 4, 1, "B", "Weekday", "2.1", "2.2");

        TableTestProjects.row(sheet, FACTOR_ROW, 1,
                "SmartLookup Double " + FACTOR + " ( String band, Integer year, String day )");
        merge(sheet, FACTOR_ROW, FACTOR_ROW, 1, 5);
        TableTestProjects.row(sheet, FACTOR_ROW + 1, 1, "Band", "1", null, "2", null);
        TableTestProjects.row(sheet, FACTOR_ROW + 2, 1, null, "Weekday", "Weekend", "Weekday", "Weekend");
        merge(sheet, FACTOR_ROW + 1, FACTOR_ROW + 2, 1, 1);
        merge(sheet, FACTOR_ROW + 1, FACTOR_ROW + 1, 2, 3);
        merge(sheet, FACTOR_ROW + 1, FACTOR_ROW + 1, 4, 5);
        TableTestProjects.row(sheet, FACTOR_ROW + 3, 1, "A", "1.1", "1.2", "1.3", "1.4");
        TableTestProjects.row(sheet, FACTOR_ROW + 4, 1, "B", "2.1", "2.2", "2.3", "2.4");

        TableTestProjects.row(sheet, 43, 1, "Datatype Reply");
        TableTestProjects.row(sheet, 44, 1, "String", "text");
        TableTestProjects.row(sheet, 45, 1, "Integer", "rank");

        TableTestProjects.row(sheet, BROKEN_ROW, 1, "Rules Nothing Broken ( Integer hour )");
        merge(sheet, BROKEN_ROW, BROKEN_ROW, 1, 2);
        TableTestProjects.row(sheet, BROKEN_ROW + 1, 1, "C1", "RET1");
        TableTestProjects.row(sheet, BROKEN_ROW + 2, 1, "hour < 12", "greeting");

        fillDeclarations(sheet);
    }

    /**
     * A Conditions table with a condition in each column, an Actions table naming no part, a Conditions and a Returns
     * table with each part in a column, and a Conditions table no declaration is read from.
     */
    private static void fillDeclarations(Sheet sheet) {
        TableTestProjects.row(sheet, CONDITIONS_ROW, 1, "Conditions AgeBand");
        merge(sheet, CONDITIONS_ROW, CONDITIONS_ROW, 1, 4);
        TableTestProjects.row(sheet, CONDITIONS_ROW + 1, 1, "Inputs", "Integer age", null, "Integer age");
        merge(sheet, CONDITIONS_ROW + 1, CONDITIONS_ROW + 1, 2, 3);
        TableTestProjects.row(sheet, CONDITIONS_ROW + 2, 1, "Expression", "age >= minAge && age <= maxAge", null,
                "age < limit");
        merge(sheet, CONDITIONS_ROW + 2, CONDITIONS_ROW + 2, 2, 3);
        TableTestProjects.row(sheet, CONDITIONS_ROW + 3, 1, "Parameter", "Integer minAge", "Integer maxAge",
                "Integer limit");
        TableTestProjects.row(sheet, CONDITIONS_ROW + 4, 1, "Title", "Age Band", null, "Under Limit");
        merge(sheet, CONDITIONS_ROW + 4, CONDITIONS_ROW + 4, 2, 3);

        TableTestProjects.row(sheet, ACTIONS_ROW, 1, "Actions Shifts");
        merge(sheet, ACTIONS_ROW, ACTIONS_ROW, 1, 2);
        TableTestProjects.row(sheet, ACTIONS_ROW + 1, 1, "Integer hour", "Integer hour");
        TableTestProjects.row(sheet, ACTIONS_ROW + 2, 1, "hour + shift", "hour - back");
        TableTestProjects.row(sheet, ACTIONS_ROW + 3, 1, "Integer shift", "Integer back");
        TableTestProjects.row(sheet, ACTIONS_ROW + 4, 1, "Shift Later", "Shift Earlier");

        TableTestProjects.row(sheet, HOURS_ROW, 1, "Conditions Hours");
        merge(sheet, HOURS_ROW, HOURS_ROW, 1, 4);
        TableTestProjects.row(sheet, HOURS_ROW + 1, 1, "Inputs", "Expression", "Parameter", "Title");
        TableTestProjects.row(sheet, HOURS_ROW + 2, 1, "Integer hour", "hour < limit", "Integer limit", "Before");
        TableTestProjects.row(sheet, HOURS_ROW + 3, 1, "Integer hour", "hour >= start", "Integer start", "From");

        TableTestProjects.row(sheet, RETURNS_ROW, 1, "Returns Rates");
        merge(sheet, RETURNS_ROW, RETURNS_ROW, 1, 4);
        TableTestProjects.row(sheet, RETURNS_ROW + 1, 1, "Title", "Inputs", "Expression", "Parameter");
        TableTestProjects.row(sheet, RETURNS_ROW + 2, 1, "Base Rate", "Double rate", "rate * factor",
                "Double factor");

        TableTestProjects.row(sheet, UNDECLARED_ROW, 1, "Conditions Undeclared");
        merge(sheet, UNDECLARED_ROW, UNDECLARED_ROW, 1, 3);
        TableTestProjects.row(sheet, UNDECLARED_ROW + 1, 1, "Integer age", "age > min", "Integer min");
    }
}
