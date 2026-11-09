package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableSignaturePart;
import org.openl.studio.projects.model.tables.RawTableValueType;

/**
 * Covers the regions of a table moved along the lines an edit inserts and removes, and fitted to the table it leaves.
 *
 * @author Yury Molchan
 */
class RegionShiftTest {

    /**
     * A decision table of three columns: the header, the titles, and five rules, the third one starting a group. The
     * first two columns are conditions, the third what the table returns.
     */
    private static final TableRegions RULES = new TableRegions(List.of(
            region(RawTableRole.SIGNATURE, 0, 0, 0, 2),
            region(RawTableRole.INPUT_TITLE, 1, 0, 1, 1),
            region(RawTableRole.OUTPUT_TITLE, 1, 2, 1, 2),
            new RawTableRegion(RawTableRole.VALUE, RawTableValueType.NUMBER, 2, 0, 6, 1),
            region(RawTableRole.OUTPUT, 2, 2, 6, 2)),
            new RawTableGroups(List.of(4), List.of()), List.of(),
            Map.of(new TableRegions.Cell(5, 0), RawTableValueType.STRING));

    private static final TableRegionLayout.Head HEAD = TableRegionLayout.Head.of(3, 0);

    /**
     * A decision table of typed values: the header, the titles, and five rules whose conditions take numbers and whose
     * return gives a text, the second rule returning a formula.
     */
    private static final TableRegions TYPED = new TableRegions(List.of(
            region(RawTableRole.SIGNATURE, 0, 0, 0, 2),
            region(RawTableRole.INPUT_TITLE, 1, 0, 1, 1),
            region(RawTableRole.OUTPUT_TITLE, 1, 2, 1, 2),
            new RawTableRegion(RawTableRole.VALUE, RawTableValueType.NUMBER, 2, 0, 6, 1),
            new RawTableRegion(RawTableRole.OUTPUT, RawTableValueType.STRING, 2, 2, 6, 2)),
            RawTableGroups.NONE, List.of(), Map.of(new TableRegions.Cell(3, 2), RawTableValueType.CODE));

    /** A Data table read by its keys: the header, the titles, and five rows of a key and a number. */
    private static final TableRegions KEYED = new TableRegions(List.of(
            region(RawTableRole.SIGNATURE, 0, 0, 0, 1),
            region(RawTableRole.OUTPUT_TITLE, 1, 0, 1, 1),
            region(RawTableRole.KEY, 2, 0, 6, 0),
            new RawTableRegion(RawTableRole.OUTPUT, RawTableValueType.NUMBER, 2, 1, 6, 1)),
            RawTableGroups.NONE, List.of(), Map.of());

    /** A Spreadsheet returning a number: the names of its steps, their values, and the step it returns. */
    private static final TableRegions STEPS = new TableRegions(List.of(
            region(RawTableRole.SIGNATURE, 0, 0, 0, 1),
            region(RawTableRole.INPUT_TITLE, 1, 0, 1, 0),
            region(RawTableRole.OUTPUT_TITLE, 1, 1, 1, 1),
            region(RawTableRole.VALUE, 2, 0, 3, 0),
            new RawTableRegion(RawTableRole.OUTPUT, RawTableValueType.NUMBER, 2, 1, 3, 1),
            region(RawTableRole.RESULT, 4, 0, 4, 0),
            new RawTableRegion(RawTableRole.RESULT, RawTableValueType.NUMBER, 4, 1, 4, 1)),
            RawTableGroups.NONE, List.of(), Map.of());

    @Test
    void stretchesTheRegionsARowIsInsertedInside() {
        var shift = RegionShift.of(RULES, 7, 3);

        shift.rowsInserted(3, 2);

        var regions = shift.fittedTo(HEAD, 9, 3, "Rules Integer rate(Integer age)");
        assertEquals(List.of(
                region(RawTableRole.SIGNATURE, 0, 0, 0, 2),
                region(RawTableRole.INPUT_TITLE, 1, 0, 1, 1),
                region(RawTableRole.OUTPUT_TITLE, 1, 2, 1, 2),
                new RawTableRegion(RawTableRole.VALUE, RawTableValueType.NUMBER, 2, 0, 8, 1),
                region(RawTableRole.OUTPUT, 2, 2, 8, 2)), regions.regions());
        assertEquals(new RawTableGroups(List.of(6), List.of()), regions.groups(), "The group moves down");
        assertEquals(Map.of(new TableRegions.Cell(7, 0), RawTableValueType.STRING), regions.types());
        assertEquals(RawTableValueType.STRING, regions.typeAt(7, 0));
    }

    @Test
    void givesARuleInsertedUnderTheTitlesTheRolesOfTheRuleUnderIt() {
        var shift = RegionShift.of(RULES, 7, 3);

        shift.rowsInserted(2, 1);

        var regions = shift.fittedTo(HEAD, 8, 3, "Rules");
        assertEquals(RawTableRole.INPUT_TITLE, roleAt(regions, 1, 0), "The titles stay where they were");
        assertEquals(RawTableRole.VALUE, roleAt(regions, 2, 0), "The new rule");
        assertEquals(RawTableRole.OUTPUT, roleAt(regions, 2, 2));
        assertEquals(RawTableRole.VALUE, roleAt(regions, 3, 1), "The first rule moved down");
    }

    @Test
    void givesALineAddedAtTheEndTheRolesOfTheLastLine() {
        var shift = RegionShift.of(RULES, 7, 3);

        // The table grew by a row and a column the edit wrote past its end.
        var regions = shift.fittedTo(TableRegionLayout.Head.of(4, 0), 8, 4, "Rules");

        assertEquals(RawTableRole.VALUE, roleAt(regions, 7, 1));
        assertEquals(RawTableRole.OUTPUT, roleAt(regions, 7, 3), "The new column takes the roles of the last one");
        assertEquals(RawTableRole.OUTPUT_TITLE, roleAt(regions, 1, 3));
        assertEquals(RawTableRole.SIGNATURE, roleAt(regions, 0, 3), "The header reaches across the wider table");
    }

    @Test
    void takesTheRemovedRowsOutOfTheirRegions() {
        var shift = RegionShift.of(RULES, 7, 3);

        shift.rowsRemoved(3, 2);

        var regions = shift.fittedTo(HEAD, 5, 3, "Rules");
        assertEquals(new RawTableRegion(RawTableRole.VALUE, RawTableValueType.NUMBER, 2, 0, 4, 1),
                regions.regions().get(3));
        assertEquals(new RawTableGroups(List.of(3), List.of()), regions.groups(),
                "The rest of the group starts at the first row left of it");
        assertEquals(Map.of(new TableRegions.Cell(3, 0), RawTableValueType.STRING), regions.types());
    }

    @Test
    void keepsTheLineAboveAGroupWhoseFirstRuleIsRemoved() {
        var shift = RegionShift.of(RULES, 7, 3);

        shift.rowsRemoved(4, 1);

        var regions = shift.fittedTo(HEAD, 6, 3, "Rules");
        assertEquals(new RawTableGroups(List.of(4), List.of()), regions.groups(),
                "The second rule of the group starts it now");
    }

    @Test
    void dropsTheLineOfAGroupRemovedToTheEndOfTheTable() {
        var shift = RegionShift.of(RULES, 7, 3);

        shift.rowsRemoved(4, 3);

        var regions = shift.fittedTo(HEAD, 4, 3, "Rules");
        assertEquals(RawTableGroups.NONE, regions.groups(), "No rule of the group is left");
    }

    @Test
    void keepsTheLineOnTheLeftOfAGroupOfColumnsWhoseFirstColumnIsRemoved() {
        // A lookup: the titles of its vertical condition, then four columns of returned values; the two last ones
        // share a merged horizontal value.
        var lookup = new TableRegions(List.of(
                region(RawTableRole.SIGNATURE, 0, 0, 0, 4),
                region(RawTableRole.INPUT_TITLE, 1, 0, 1, 0),
                region(RawTableRole.OUTPUT_TITLE, 1, 1, 1, 4),
                region(RawTableRole.VALUE, 2, 0, 3, 0),
                region(RawTableRole.OUTPUT, 2, 1, 3, 4)), new RawTableGroups(List.of(), List.of(3)), List.of(),
                Map.of());
        var shift = RegionShift.of(lookup, 4, 5);

        shift.columnsRemoved(3, 1);

        var regions = shift.fittedTo(TableRegionLayout.Head.of(4, 0), 4, 4, "SmartLookup");
        assertEquals(new RawTableGroups(List.of(), List.of(3)), regions.groups(),
                "The last column starts the group now");
    }

    @Test
    void dropsARegionWhoseRowsAreAllRemoved() {
        var shift = RegionShift.of(RULES, 7, 3);

        shift.rowsRemoved(1, 1);

        var regions = shift.fittedTo(HEAD, 6, 3, "Rules");
        assertEquals(List.of(RawTableRole.SIGNATURE, RawTableRole.VALUE, RawTableRole.OUTPUT),
                regions.regions().stream().map(RawTableRegion::role).toList());
        assertEquals(RawTableRole.VALUE, roleAt(regions, 1, 0), "The rules move up under the header");
    }

    @Test
    void movesTheColumnsAsTheRows() {
        var shift = RegionShift.of(RULES, 7, 3);

        shift.columnsInserted(0, 1);

        var regions = shift.fittedTo(TableRegionLayout.Head.of(4, 0), 7, 4, "Rules");
        assertEquals(RawTableRole.INPUT_TITLE, roleAt(regions, 1, 0), "The new first column repeats the one it moved");
        assertEquals(RawTableRole.VALUE, roleAt(regions, 6, 0));
        assertEquals(RawTableRole.INPUT_TITLE, roleAt(regions, 1, 1));
        assertEquals(RawTableRole.OUTPUT_TITLE, roleAt(regions, 1, 3));
        assertEquals(Map.of(new TableRegions.Cell(5, 1), RawTableValueType.STRING), regions.types(),
                "The type of a cell moves with its column");

        shift.columnsRemoved(3, 1);

        var narrowed = shift.fittedTo(HEAD, 7, 3, "Rules");
        assertEquals(List.of(RawTableRole.SIGNATURE, RawTableRole.INPUT_TITLE, RawTableRole.VALUE,
                        RawTableRole.INPUT_TITLE, RawTableRole.VALUE),
                narrowed.regions().stream().map(RawTableRegion::role).toList(), "The return is gone");
    }

    @Test
    void givesAColumnInsertedBeforeTheReturnTheRolesOfTheConditionBeforeIt() {
        var shift = RegionShift.of(RULES, 7, 3);

        shift.columnsInserted(2, 1);

        var regions = shift.fittedTo(TableRegionLayout.Head.of(4, 0), 7, 4, "Rules");
        assertEquals(RawTableRole.INPUT_TITLE, roleAt(regions, 1, 2));
        assertEquals(RawTableRole.VALUE, roleAt(regions, 4, 2));
        assertEquals(RawTableRole.OUTPUT, roleAt(regions, 4, 3), "The return moved right");
    }

    @Test
    void laysTheHeaderAndThePropertiesOutAgainAndMovesTheBody() {
        var shift = RegionShift.of(RULES, 7, 3);

        // Two properties were written under the header.
        var regions = shift.fittedTo(TableRegionLayout.Head.of(3, 2), 9, 3, "Rules Integer rate(Integer age)");

        assertEquals(RawTableRole.PROPERTIES, roleAt(regions, 1, 0));
        assertEquals(RawTableRole.PROPERTIES, roleAt(regions, 2, 2));
        assertEquals(RawTableRole.INPUT_TITLE, roleAt(regions, 3, 0), "The titles under the properties");
        assertEquals(RawTableRole.OUTPUT, roleAt(regions, 8, 2));
        assertEquals(new RawTableGroups(List.of(6), List.of()), regions.groups());
        assertEquals("rate", nameOf(regions, "Rules Integer rate(Integer age)"), "The header is read again");
        assertTrue(shift.bodyMoved());
    }

    @Test
    void tellsTheLinesTheEditsTouchedInTheTableTheyLeave() {
        var shift = RegionShift.of(RULES, 7, 3);

        shift.rowsInserted(3, 2);
        shift.rowsRemoved(6, 1);
        shift.columnsInserted(3, 1);

        shift.fittedTo(TableRegionLayout.Head.of(4, 0), 8, 4, "Rules");
        assertEquals(Set.of(2, 3, 4, 5, 6), shift.touchedRows(),
                "The rows inserted, the rows on either side of them, and the rows on either side of the one removed");
        assertEquals(Set.of(2, 3), shift.touchedColumns(), "The column added at the end, and the column before it");
        assertFalse(shift.bodyMoved());
    }

    @Test
    void keepsTheCellsATableReturnsAsTheLinesMove() {
        var marked = new TableRegions(List.of(
                region(RawTableRole.SIGNATURE, 0, 0, 0, 1),
                region(RawTableRole.KEY, 1, 0, 1, 0),
                new RawTableRegion(RawTableRole.OUTPUT, null, 1, 1, 1, 1, true),
                region(RawTableRole.VALUE, 2, 0, 2, 0),
                region(RawTableRole.OUTPUT, 2, 1, 2, 1)), RawTableGroups.NONE, List.of(), Map.of());
        var shift = RegionShift.of(marked, 3, 2);

        shift.rowsInserted(1, 1);
        shift.columnsInserted(1, 1);

        var regions = shift.fittedTo(TableRegionLayout.Head.of(3, 0), 4, 3, "Spreadsheet");
        assertTrue(returnedAt(regions, 2, 2), "The cell returned moved down and right");
        assertFalse(returnedAt(regions, 3, 2), "A cell not returned");
    }

    @Test
    void neverReturnsAStepOrAColumnAppendedAfterTheMarkedOnes() {
        // A SpreadsheetResult whose last step and last column are marked: the cell under both is returned.
        var marked = new TableRegions(List.of(
                region(RawTableRole.SIGNATURE, 0, 0, 0, 1),
                region(RawTableRole.VALUE, 1, 0, 1, 0),
                region(RawTableRole.OUTPUT, 1, 1, 1, 1),
                region(RawTableRole.KEY, 2, 0, 2, 0),
                new RawTableRegion(RawTableRole.OUTPUT, null, 2, 1, 2, 1, true)), RawTableGroups.NONE, List.of(),
                Map.of());
        var shift = RegionShift.of(marked, 3, 2);

        shift.rowsInserted(3, 1);
        shift.columnsInserted(2, 1);

        var regions = shift.fittedTo(TableRegionLayout.Head.of(3, 0), 4, 3, "Spreadsheet");
        assertTrue(returnedAt(regions, 2, 1), "The marked cell stays returned");
        assertFalse(returnedAt(regions, 3, 1), "A step appended after the marked one");
        assertEquals(RawTableRole.OUTPUT, roleAt(regions, 3, 1));
        assertFalse(returnedAt(regions, 2, 2), "A column appended after the marked one");
        assertEquals(RawTableRole.OUTPUT, roleAt(regions, 2, 2));
    }

    @Test
    void neverReturnsAStepInsertedBetweenTwoMarkedOnes() {
        // A SpreadsheetResult marking two adjacent steps: their values are one returned region.
        var marked = new TableRegions(List.of(
                region(RawTableRole.SIGNATURE, 0, 0, 0, 1),
                region(RawTableRole.KEY, 1, 0, 2, 0),
                new RawTableRegion(RawTableRole.OUTPUT, null, 1, 1, 2, 1, true),
                region(RawTableRole.VALUE, 3, 0, 3, 0),
                region(RawTableRole.OUTPUT, 3, 1, 3, 1)), RawTableGroups.NONE, List.of(), Map.of());
        var shift = RegionShift.of(marked, 4, 2);

        shift.rowsInserted(2, 1);

        var regions = shift.fittedTo(TableRegionLayout.Head.of(2, 0), 5, 2, "Spreadsheet");
        assertTrue(returnedAt(regions, 1, 1), "The first marked step");
        assertFalse(returnedAt(regions, 2, 1), "The step inserted between them");
        assertEquals(RawTableRole.OUTPUT, roleAt(regions, 2, 1), "takes their role alone");
        assertTrue(returnedAt(regions, 3, 1), "The second marked step");
    }

    @Test
    void neverRepeatsAMarkedStepForAStepInsertedAfterIt() {
        // A SpreadsheetResult: a step marked *, its name a key and its value returned, and a step not marked.
        var marked = new TableRegions(List.of(
                region(RawTableRole.SIGNATURE, 0, 0, 0, 1),
                region(RawTableRole.KEY, 1, 0, 1, 0),
                new RawTableRegion(RawTableRole.OUTPUT, null, 1, 1, 1, 1, true),
                region(RawTableRole.VALUE, 2, 0, 2, 0),
                region(RawTableRole.OUTPUT, 2, 1, 2, 1)), RawTableGroups.NONE, List.of(), Map.of());
        var shift = RegionShift.of(marked, 3, 2);

        shift.rowsInserted(2, 1);

        var regions = shift.fittedTo(TableRegionLayout.Head.of(2, 0), 4, 2, "Spreadsheet");
        assertEquals(RawTableRole.VALUE, roleAt(regions, 2, 0), "The new step takes the roles of the step under it");
        assertFalse(returnedAt(regions, 2, 1), "The new step is not marked");
        assertTrue(returnedAt(regions, 1, 1), "The marked step stays marked");
    }

    private static boolean returnedAt(TableRegions regions, int row, int column) {
        return regions.regions().stream()
                .filter(region -> region.holds(row, column))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No region holds " + row + ", " + column))
                .returned();
    }

    @Test
    void neverRepeatsASectionOrTheResultForARowInsertedAfterIt() {
        // A TBasic body: a step, a step that returns, and a step.
        var tbasic = new TableRegions(List.of(
                region(RawTableRole.SIGNATURE, 0, 0, 0, 1),
                region(RawTableRole.VALUE, 1, 0, 1, 1),
                region(RawTableRole.RESULT, 2, 0, 2, 1),
                region(RawTableRole.VALUE, 3, 0, 3, 1)), RawTableGroups.NONE, List.of(), Map.of());
        var shift = RegionShift.of(tbasic, 4, 2);

        shift.rowsInserted(3, 1);

        var regions = shift.fittedTo(TableRegionLayout.Head.of(2, 0), 5, 2, "TBasic");
        assertEquals(RawTableRole.RESULT, roleAt(regions, 2, 0));
        assertEquals(RawTableRole.VALUE, roleAt(regions, 3, 0), "The new step takes the roles of the step under it");
    }

    @Test
    void keepsTheRolesOfAnEditOfValuesOfATypeAlone() {
        var shift = RegionShift.of(TYPED, 7, 3);

        shift.cellWritten(2, 0);
        shift.cellWritten(4, 2);
        shift.rowsInserted(4, 2);
        shift.rowsInserted(9, 1);
        shift.rowsRemoved(4, 2);

        assertFalse(shift.structureEdited(), "Values of a type change no role");
    }

    @Test
    void tellsAWriteOfATitleOrOfCodeMayChangeTheRoles() {
        var title = RegionShift.of(TYPED, 7, 3);
        title.cellWritten(1, 1);
        assertTrue(title.structureEdited(), "A title");

        var signature = RegionShift.of(TYPED, 7, 3);
        signature.cellWritten(0, 0);
        assertTrue(signature.structureEdited(), "The header");

        var formula = RegionShift.of(TYPED, 7, 3);
        formula.cellWritten(3, 2);
        assertTrue(formula.structureEdited(), "A value of code, such as the operation of a step of a TBasic table");
    }

    @Test
    void tellsAWriteOfTheNameOfAStepMayChangeTheRoles() {
        var name = RegionShift.of(STEPS, 5, 2);
        name.cellWritten(3, 0);
        assertTrue(name.structureEdited(), "A name, which a mark makes a step returned");

        var value = RegionShift.of(STEPS, 5, 2);
        value.cellWritten(3, 1);
        assertFalse(value.structureEdited(), "The value of a step");
    }

    @Test
    void tellsTheLowestRowAnEditThatMayChangeTheRolesTouched() {
        var title = RegionShift.of(TYPED, 7, 3);
        title.cellWritten(1, 2);
        assertTrue(title.structureEditedFrom(1), "A title written");
        assertFalse(title.structureEditedFrom(2), "No row under it");
        title.rowsInserted(0, 3);
        assertTrue(title.structureEditedFrom(4), "The title moved down with the rows inserted above it");
        title.rowsRemoved(0, 2);
        assertTrue(title.structureEditedFrom(2), "And up with the rows removed above it");
        assertFalse(title.structureEditedFrom(3), "No row under it");

        var name = RegionShift.of(STEPS, 5, 2);
        name.cellWritten(3, 0);
        assertTrue(name.structureEdited(), "The name of a step written may change the roles");
        assertFalse(name.structureEditedFrom(0), "A name is no title: the kinds a probe serves have none in the body");

        var key = RegionShift.of(KEYED, 7, 2);
        key.cellWritten(5, 0);
        key.rowsInserted(4, 1);
        assertTrue(key.structureEdited(), "A key written may change the roles");
        assertFalse(key.structureEditedFrom(0), "A key names a line: the first rows tell any role it changes");
        assertFalse(RegionShift.of(KEYED, 7, 2).structureEdited(), "Nothing edited");

        var values = RegionShift.of(KEYED, 7, 2);
        values.rowsInserted(4, 2);
        values.rowsRemoved(2, 1);
        assertFalse(values.structureEdited(), "Lines of keys and values of a type change no role");
    }

    @Test
    void tellsTheSameRolesWhateverTheTypesAndTheCutOfTheRegions() {
        var cut = new TableRegions(List.of(
                region(RawTableRole.SIGNATURE, 0, 0, 0, 2),
                region(RawTableRole.INPUT_TITLE, 1, 0, 1, 1),
                region(RawTableRole.OUTPUT_TITLE, 1, 2, 1, 2),
                new RawTableRegion(RawTableRole.VALUE, RawTableValueType.STRING, 2, 0, 3, 1),
                new RawTableRegion(RawTableRole.VALUE, RawTableValueType.NUMBER, 4, 0, 6, 1),
                new RawTableRegion(RawTableRole.OUTPUT, null, 2, 2, 6, 2)),
                TYPED.groups(), List.of(), Map.of());
        assertTrue(TableRegionLayout.sameRoles(TYPED, cut), "Other types and another cut of the same roles");

        var returning = new TableRegions(List.of(
                region(RawTableRole.SIGNATURE, 0, 0, 0, 2),
                region(RawTableRole.INPUT_TITLE, 1, 0, 1, 0),
                region(RawTableRole.OUTPUT_TITLE, 1, 1, 1, 2),
                new RawTableRegion(RawTableRole.VALUE, RawTableValueType.NUMBER, 2, 0, 6, 0),
                region(RawTableRole.OUTPUT, 2, 1, 6, 2)),
                TYPED.groups(), List.of(), Map.of());
        assertFalse(TableRegionLayout.sameRoles(TYPED, returning), "A condition made a return");
    }

    @Test
    void tellsALineOfMoreThanValuesInsertedOrRemovedMayChangeTheRoles() {
        var removed = RegionShift.of(STEPS, 5, 2);
        removed.rowsRemoved(4, 1);
        assertTrue(removed.structureEdited(), "The step returned is removed");

        var inserted = RegionShift.of(TYPED, 7, 3);
        inserted.rowsInserted(3, 1);
        assertTrue(inserted.structureEdited(), "A rule inserted above the second one, whose return is code");

        var column = RegionShift.of(TYPED, 7, 3);
        column.columnsInserted(2, 1);
        assertTrue(column.structureEdited(), "A column");

        var removedColumn = RegionShift.of(TYPED, 7, 3);
        removedColumn.columnsRemoved(1, 1);
        assertTrue(removedColumn.structureEdited(), "A column removed");
    }

    private static RawTableRole roleAt(TableRegions regions, int row, int column) {
        return regions.regions().stream()
                .filter(region -> region.holds(row, column))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No region holds " + row + ", " + column))
                .role();
    }

    private static String nameOf(TableRegions regions, String header) {
        var from = 0;
        for (var piece : regions.signature()) {
            if (piece.part() == RawTableSignaturePart.NAME) {
                return header.substring(from, piece.to()).trim();
            }
            from = piece.to();
        }
        return "";
    }

    private static RawTableRegion region(RawTableRole role, int top, int left, int bottom, int right) {
        return new RawTableRegion(role, null, top, left, bottom, right);
    }
}
