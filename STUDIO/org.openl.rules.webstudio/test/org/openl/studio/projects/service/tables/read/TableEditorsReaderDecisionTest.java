package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.studio.projects.service.tables.TableTestProjects.projectModel;
import static org.openl.studio.projects.service.tables.TableTestProjects.row;
import static org.openl.studio.projects.service.tables.TableTestProjects.table;
import static org.openl.studio.projects.service.tables.read.TableEditorsReaderTest.areaEditor;

import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.DeclaredTableEditorsView;
import org.openl.studio.projects.model.tables.TableCellEditorView;
import org.openl.studio.projects.model.tables.TableEditorsView;

/**
 * Verifies what a decision table says its rules are written with.
 *
 * <p>A decision table declares what a condition takes and what an action gives once, in its headers, and every
 * rule written under them holds it. The fixture holds one of each way a reader meets: written down, written
 * across, written as a lookup whose rules run both ways, and declared with no rule written in it at all.
 */
class TableEditorsReaderDecisionTest {

    private ProjectModel module;

    @BeforeEach
    void writeModule(@TempDir Path projectDir) throws Exception {
        module = projectModel(projectDir, "Rules", sheet -> {
            // The rules run down: the header, the column names, the expressions, the types, the titles.
            row(sheet, 1, 1, "Rules String Greeting(Integer hour)");
            row(sheet, 2, 1, "C1", "C2", "RET1");
            row(sheet, 3, 1, "min <= hour", "hour <= max", "greeting");
            row(sheet, 4, 1, "Integer min", "Integer max", "String greeting");
            row(sheet, 5, 1, "From", "To", "Greeting");
            row(sheet, 6, 1, "0", "11", "Good Morning");
            row(sheet, 7, 1, "12", "17", "Good Afternoon");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 3));

            // The same rules written the other way round, one condition to a row.
            row(sheet, 10, 1, "Rules String Sideways(Integer hour)");
            row(sheet, 11, 1, "C1", "min <= hour", "Integer min", "From", "0", "12");
            row(sheet, 12, 1, "RET1", "greeting", "String greeting", "Greeting", "Morning", "Afternoon");
            sheet.addMergedRegion(new CellRangeAddress(10, 10, 1, 6));

            // A lookup: the rules run down one condition and across another, and meet in the returns.
            row(sheet, 15, 1, "Rules Double Premium(String age, String status)");
            row(sheet, 16, 1, "C1", "HC1", "RET1");
            row(sheet, 17, 1, "age", "status");
            row(sheet, 18, 1, "String", "String");
            row(sheet, 19, 1, "Driver Age", "Married", "Single");
            row(sheet, 20, 1, "Young Driver", "700", "720");
            row(sheet, 21, 1, "Senior Driver", "300", "350");
            sheet.addMergedRegion(new CellRangeAddress(15, 15, 1, 3));

            // Declared and empty: the columns are named and their types come from the method it is written for.
            row(sheet, 24, 1, "SimpleRules Double Blank(String age, Integer years)");
            row(sheet, 25, 1, "Driver Age", "Years", "Premium");
            sheet.addMergedRegion(new CellRangeAddress(24, 24, 1, 3));

            // A lookup declared and empty: the values of the condition the rules would run across stand in it,
            // and nothing else does.
            row(sheet, 28, 1, "SimpleLookup Double BlankLookup(String age, String status)");
            row(sheet, 29, 1, "Driver Age", "Married", "Single");
            sheet.addMergedRegion(new CellRangeAddress(28, 28, 1, 3));
        });
    }

    @Test
    void saysWhatEveryRuleOfAConditionWrittenDownTheTableHolds() {
        var read = read("Greeting");

        // Both conditions hold whole numbers, from the row under their titles and for every rule after it.
        assertEquals(number(true), areaEditor(read, 5, 0));
        assertEquals(number(true), areaEditor(read, 6, 0));
        assertEquals(number(true), areaEditor(read, 5, 1));
        // A rule written under the last one holds what its column was declared with.
        assertEquals(number(true), areaEditor(read, 7, 0));
        assertNull(areaEditor(read, 5, 2), "the return holds text, which a screen writes without being told");
    }

    @Test
    void keepsAColumnOfARuleWithinTheColumnItWasDeclaredIn() {
        var read = read("Greeting");

        // The condition runs down the table, not across it: a column laid down beside it is no rule of it.
        assertNull(areaEditor(read, 5, 3));
        // Neither the types nor the titles the columns are declared in are values of the rules.
        assertNull(areaEditor(read, 3, 0));
        assertNull(areaEditor(read, 4, 0));
    }

    @Test
    void saysWhatEveryRuleOfATableWrittenTheOtherWayRoundHolds() {
        var read = read("Sideways");

        // Every condition of this table is a row of it, and its rules run across from the fourth column.
        assertEquals(number(true), areaEditor(read, 1, 4));
        assertEquals(number(true), areaEditor(read, 1, 5));
        // A rule written beside the last one holds what its row was declared with.
        assertEquals(number(true), areaEditor(read, 1, 6));
        assertNull(areaEditor(read, 1, 3), "the title the condition is shown under is no value of it");
        assertNull(areaEditor(read, 2, 4), "the return holds text");
    }

    @Test
    void saysWhatTheRulesOfALookupMeetIn() {
        var read = read("Premium");

        // The condition the rules run down holds text; the one they run across holds text too. What they meet
        // in is the return, and a return of this table is a number.
        assertEquals(number(false), areaEditor(read, 5, 1));
        assertEquals(number(false), areaEditor(read, 6, 2));
        // The cells the rules would meet in are added to by a rule written either way.
        assertEquals(number(false), areaEditor(read, 7, 1));
        assertEquals(number(false), areaEditor(read, 5, 3));
        assertNull(areaEditor(read, 5, 0), "the condition the rules run down holds text");
        assertNull(areaEditor(read, 4, 1), "the values of the condition they run across hold text");
    }

    @Test
    void saysWhatAColumnDeclaredInATableHoldingNoRulesWouldHold() {
        var read = read("Blank");

        // Nothing stands in the table to be asked about, and the types come from the method it is written for.
        assertEquals(number(true), areaEditor(read, 2, 1), "the column declared to hold whole numbers");
        assertEquals("numeric", areaEditor(read, 2, 2).editor(), "the return the method was declared with");
        assertNull(areaEditor(read, 2, 0), "the column declared to hold text");
        assertNull(areaEditor(read, 1, 1), "the title the column is shown under is no value of it");
    }

    @Test
    void saysWhatTheRulesOfALookupHoldingNoneOfThemWouldMeetIn() {
        var read = read("BlankLookup");

        // The values of the condition the rules would run across stand in the table; what they would meet in
        // begins under them and runs down and across from there.
        assertEquals("numeric", areaEditor(read, 2, 1).editor());
        assertEquals("numeric", areaEditor(read, 5, 4).editor());
        assertNull(areaEditor(read, 2, 0), "the condition the rules would run down holds text");
        assertNull(areaEditor(read, 1, 1), "a value of the condition they would run across is no return");
    }

    @Test
    void namesACellOnlyWhereItIsWrittenOtherThanTheConditionItStandsIn() {
        for (String name : List.of("Greeting", "Sideways", "Premium", "Blank", "BlankLookup")) {
            assertTrue(read(name).getCells().isEmpty(),
                    name + " says once what every rule of a condition takes");
        }
    }

    private TableEditorsView read(String name) {
        var read = new TableEditorsReader().read(table(module, name), null, null);
        assertNotNull(read);
        assertTrue(read instanceof DeclaredTableEditorsView, name + " declares what its conditions hold");
        return read;
    }

    /** The way a number is entered: within the bounds of its type, and with a point where the type takes one. */
    private static TableCellEditorView number(boolean whole) {
        // The bounds are kept apart rather than chosen in one expression: a conditional over an int and a
        // double is a double, which would quietly describe a whole number with a decimal bound.
        var described = TableCellEditorView.builder().editor("numeric").intOnly(whole);
        return (whole ? described.min(Integer.MIN_VALUE).max(Integer.MAX_VALUE)
                : described.min(-Double.MAX_VALUE).max(Double.MAX_VALUE)).build();
    }
}
