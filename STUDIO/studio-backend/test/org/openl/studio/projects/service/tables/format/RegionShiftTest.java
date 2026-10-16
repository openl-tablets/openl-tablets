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
        assertEquals(RawTableGroups.NONE, regions.groups(), "The row starting the group is gone");
        assertEquals(Map.of(new TableRegions.Cell(3, 0), RawTableValueType.STRING), regions.types());
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
