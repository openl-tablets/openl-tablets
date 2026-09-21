package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * A table written as a header alone, the way an author leaves it while writing one, has no body to shape a
 * reader by: the readers shaped by the body decline it, and it is read as the grid it is.
 */
class HeaderOnlyTableReadTest {

    @TempDir
    Path tempDir;

    /** How many tables have been written, so that each gets a module of its own. */
    private int written;

    @Test
    void aHeaderAloneIsNoBody() throws Exception {
        assertFalse(OpenLTableUtils.hasBody(headerOnly("Spreadsheet ")));
        assertTrue(OpenLTableUtils.hasBody(table("Spreadsheet String Full()", new String[]{"Step1", "\"a\""})));
    }

    @Test
    void theBodyShapedReadersDeclineIt() throws Exception {
        var spreadsheet = headerOnly("Spreadsheet ");
        assertFalse(new SpreadsheetTableReader().supports(spreadsheet));
        assertFalse(new SimpleSpreadsheetReader().supports(spreadsheet));
        assertFalse(new SimpleRulesTableReader().supports(headerOnly("SimpleRules String Price(String make)")));
        assertFalse(new SmartRulesTableReader().supports(headerOnly("SmartRules String Price(String make)")));
        assertFalse(new LookupTableReader().supports(headerOnly("SmartLookup String Price(String make)")));
        assertFalse(new LookupTableReader().supports(headerOnly("SimpleLookup String Price(String make)")));
    }

    @Test
    void aTitleRowAloneHoldsNoRulesYet() throws Exception {
        // The next state an author leaves a table in: the titles of its columns, and no rows under them yet.
        var rules = new SmartRulesTableReader()
                .read(table("SmartRules String Price(String make)", new String[]{"make", "Price"}));
        var lookup = new LookupTableReader()
                .read(table("SmartLookup String Price(String make)", new String[]{"make", "Price"}));

        assertEquals(List.of("make", "Price"), rules.headers.stream().map(header -> header.title).toList());
        assertEquals(List.of(), rules.rules);
        assertEquals(List.of("make", "Price"), lookup.headers.stream().map(header -> header.title).toList());
        assertEquals(List.of(), lookup.rows);
    }

    @Test
    void theListingKeepsTheShapeTheHeaderDeclares() throws Exception {
        var summary = new SummaryTableReader().read(headerOnly("SimpleRules String Price(String make)"));

        // The listing says what the header declares; which reader answers the table is decided by its body.
        assertEquals("SimpleRules", summary.tableType);
    }

    @Test
    void theGridStillReadsIt() throws Exception {
        // The header is the whole grid, read as the cell holds it — its trailing space trimmed with the rest.
        assertEquals(List.of(List.of("Spreadsheet")), TableTestProjects.rawSource(headerOnly("Spreadsheet ")));
    }

    private IOpenLTable headerOnly(String header) throws Exception {
        return table(header);
    }

    /** The one table of a module whose grid holds the given rows, the header first. */
    private IOpenLTable table(String header, String[]... rows) throws Exception {
        var grid = ArrayUtils.addFirst(rows, new String[]{header});
        var name = "table" + ++written;
        return TableTestProjects.onlyTable(TableTestProjects.writeProject(tempDir.resolve(name), name, "Rules", grid));
    }
}
