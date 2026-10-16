package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableValueType;

/**
 * Covers the tables a save left formatted: known while their workbook file stays as that save wrote it, in the same
 * regions.
 *
 * @author Yury Molchan
 */
class FormattedTablesTest {

    private static final TableRegions RULES = new TableRegions(List.of(
            new RawTableRegion(RawTableRole.SIGNATURE, null, 0, 0, 0, 1),
            new RawTableRegion(RawTableRole.VALUE, RawTableValueType.NUMBER, 1, 0, 3, 1)),
            new RawTableGroups(List.of(2), List.of()), List.of(), Map.of());

    @TempDir
    Path dir;

    @Test
    void knowsATableFormattedInTheSameRegionsWhateverTheTypesOfItsValues() throws IOException {
        var position = positionIn(Files.writeString(dir.resolve("Rules.xlsx"), "saved"));

        FormattedTables.formatted(position, RULES);

        var typed = new TableRegions(RULES.regions(), RULES.groups(), RULES.signature(),
                Map.of(new TableRegions.Cell(2, 1), RawTableValueType.STRING));
        assertTrue(FormattedTables.isFormatted(position, typed), "The look of a cell does not follow its value");
        var ungrouped = new TableRegions(RULES.regions(), RawTableGroups.NONE, RULES.signature(), Map.of());
        assertFalse(FormattedTables.isFormatted(position, ungrouped), "Other regions");
    }

    @Test
    void forgetsATableOnceItsWorkbookIsWrittenAgain() throws IOException {
        var file = Files.writeString(dir.resolve("Rules.xlsx"), "saved");
        var position = positionIn(file);
        FormattedTables.formatted(position, RULES);

        Files.writeString(file, "written by another tool");

        assertFalse(FormattedTables.isFormatted(position, RULES));
    }

    @Test
    void knowsNoTableReadFromNoWorkbookFile() {
        var grid = mock(IGridTable.class);
        var sheet = mock(IGrid.class);
        when(grid.getGrid()).thenReturn(sheet);

        assertNull(FormattedTables.positionOf(grid));
        FormattedTables.formatted(null, RULES);
        assertFalse(FormattedTables.isFormatted(null, RULES));
    }

    private static FormattedTables.Position positionIn(Path file) {
        return new FormattedTables.Position(file.toFile(), "Rules", 0, 0);
    }
}
