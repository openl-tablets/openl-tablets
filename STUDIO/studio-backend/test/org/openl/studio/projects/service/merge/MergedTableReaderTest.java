package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;

import org.openl.rules.xls.merge.diff.CellConflict;
import org.openl.rules.xls.merge.diff.CellPart;
import org.openl.rules.xls.merge.diff.CellSource;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.TableCells;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.service.tables.read.RawTableCells;

class MergedTableReaderTest {

    @Test
    void readsAConflictPastTheRowsShownOnItsOwn() {
        // A table of 1,000 rows taken from your version, with a conflicting cell in its second and its last row.
        var place = CellRangeAddress.valueOf("A1:B1000");
        var cells = new HashMap<CellAddress, CellSource>();
        for (var address : place) {
            cells.put(address, new CellSource(address, null, Set.of()));
        }
        var table = new TableCells(place, cells, List.of(), List.of(conflict("B2"), conflict("B1000")), List.of());
        var areas = new ArrayList<String>();

        var merged = MergedTableReader.read(table, 200, (revision, area) -> {
            areas.add(revision + " " + area.formatAsString());
            return cellsOf(revision, area);
        });

        assertEquals(Set.of("OURS A1:B200", "BASE B2", "THEIRS B2", "OURS B1000", "BASE B1000", "THEIRS B1000"),
                Set.copyOf(areas));
        assertEquals(6, areas.size());
        assertEquals(200, merged.cells().size());
        var far = merged.conflicts().getLast();
        assertEquals("B1000", far.cell());
        assertEquals("OURS B1000", far.ours().value());
        assertEquals("THEIRS B1000", far.theirs().value());
    }

    private static CellConflict conflict(String cell) {
        var address = new CellAddress(cell);
        return new CellConflict(address,
                address,
                address,
                address,
                new CellSource(address, null, Set.of()),
                new CellSource(address, address, Set.of(CellPart.VALUE)));
    }

    /** The cells of an area of a version, each holding the name of the version and its address. */
    private static RawTableCells cellsOf(Resolution revision, CellRangeAddress area) {
        var rows = new ArrayList<List<RawTableCell>>();
        for (var row = area.getFirstRow(); row <= area.getLastRow(); row++) {
            var line = new ArrayList<RawTableCell>();
            for (var column = area.getFirstColumn(); column <= area.getLastColumn(); column++) {
                var address = new CellAddress(row, column).formatAsString();
                line.add(RawTableCell.builder().cell(address).value(revision + " " + address).build());
            }
            rows.add(line);
        }
        return new RawTableCells(rows, List.of());
    }
}
