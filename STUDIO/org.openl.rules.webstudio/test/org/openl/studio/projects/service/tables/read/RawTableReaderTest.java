package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableView;

/**
 * Confirms the raw reader caps rows at {@code maxRows} and reports the full count when truncated, while the
 * default path returns the whole table unchanged (so the shared Tables API stays non-regressive).
 */
class RawTableReaderTest {

    private static final String PROJECT = "test/rules/EPBDS-16160";

    @Test
    void capsRowsAndReportsTotalWhenTruncated() throws Exception {
        IOpenLTable table = multiRowTable();
        int fullHeight = new RawTableReader().read(table).source.size();

        RawTableView capped = new RawTableReader().read(table, null, 1, false);

        assertEquals(1, capped.source.size(), "the result must be capped to maxRows");
        assertNotNull(capped.totalRows, "a truncated read must report the full row count");
        assertEquals(fullHeight, capped.totalRows);
    }

    @Test
    void returnsTheWholeTableByDefault() throws Exception {
        IOpenLTable table = multiRowTable();

        RawTableView full = new RawTableReader().read(table);
        assertNull(full.totalRows, "a full read carries no truncation marker");

        // A cap at or above the height is non-regressive: same rows, no marker.
        RawTableView wide = new RawTableReader().read(table, null, full.source.size() + 10, false);
        assertEquals(full.source.size(), wide.source.size());
        assertNull(wide.totalRows);
    }

    @Test
    void attachesExcelStylesOnlyWhenRequested() throws Exception {
        IOpenLTable table = multiRowTable();

        // Default read carries no styles, so the shared API stays plain (e.g. for MCP).
        RawTableView plain = new RawTableReader().read(table);
        assertTrue(plain.source.stream().flatMap(List::stream).allMatch(c -> c.style() == null),
                "the default read must carry no cell styles");

        // With styles requested, the shape is unchanged and Excel formatting is attached.
        RawTableView styled = new RawTableReader().read(table, null, null, true);
        assertEquals(plain.source.size(), styled.source.size());
        assertTrue(styled.source.stream().flatMap(List::stream).anyMatch(c -> c.style() != null),
                "a styled read must attach at least one cell style");
    }

    @Test
    void readsAWindowFromStartRowKeepingAbsoluteAddresses() throws Exception {
        IOpenLTable table = multiRowTable();
        var reader = new RawTableReader();

        List<List<RawTableCell>> full = reader.read(table).source;
        int fullHeight = full.size();

        // Slice from a plain data row, so no merged region is cut at the boundary, and confirm the window
        // lines up one-to-one with the whole-table read while keeping absolute cell addresses.
        int startRow = firstPlainRow(full);
        RawTableView window = reader.read(table, startRow, null, false);

        assertEquals(fullHeight - startRow, window.source.size(), "the window must skip the rows before startRow");
        assertEquals(cellAddresses(full.get(startRow)), cellAddresses(window.source.getFirst()),
                "a sliced cell must keep the address it has in the whole table");
        assertNotNull(window.totalRows, "a windowed read must report the full row count");
        assertEquals(fullHeight, window.totalRows);
    }

    @Test
    void capsTheWindowToMaxRowsFromStartRow() throws Exception {
        IOpenLTable table = multiRowTable();
        var reader = new RawTableReader();
        List<List<RawTableCell>> full = reader.read(table).source;

        // startRow and maxRows compose into a bounded slice taken from the middle of the table.
        int startRow = firstPlainRow(full);
        RawTableView oneRow = reader.read(table, startRow, 1, false);

        assertEquals(1, oneRow.source.size(), "the window must be capped to maxRows counted from startRow");
        assertEquals(cellAddresses(full.get(startRow)), cellAddresses(oneRow.source.getFirst()));
        assertEquals(full.size(), oneRow.totalRows);
    }

    @Test
    void returnsAnEmptyWindowWhenStartRowIsPastTheEnd() throws Exception {
        IOpenLTable table = multiRowTable();
        var reader = new RawTableReader();
        int fullHeight = reader.read(table).source.size();

        RawTableView beyond = reader.read(table, fullHeight + 5, null, false);

        assertTrue(beyond.source.isEmpty(), "an offset past the last row yields an empty matrix");
        assertEquals(fullHeight, beyond.totalRows, "the empty window still reports the full row count");
    }

    /** The A1 addresses of a matrix row, so a slice can be checked to keep absolute cell addresses. */
    private static List<String> cellAddresses(List<RawTableCell> row) {
        return row.stream().map(RawTableCell::cell).toList();
    }

    /** The first plain data row (every cell a simple 1x1 origin), so slicing there cuts no merged region. */
    private static int firstPlainRow(List<List<RawTableCell>> matrix) {
        for (int r = 1; r < matrix.size(); r++) {
            if (matrix.get(r).stream().allMatch(c -> c.cell() != null && c.colspan() == null && c.rowspan() == null)) {
                return r;
            }
        }
        throw new IllegalStateException("no plain data row to slice at");
    }

    /** The first table in the test project with more than one row, so the cap is observable. */
    private static IOpenLTable multiRowTable() throws Exception {
        var modules = ProjectResolver.getInstance().resolve(Path.of(PROJECT)).getModules();
        var projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(modules.getFirst());
        var reader = new RawTableReader();
        for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
            IOpenLTable table = new TableSyntaxNodeAdapter(tsn);
            if (table.getGridTable(IXlsTableNames.VIEW_DEVELOPER) != null && reader.read(table).source.size() > 1) {
                return table;
            }
        }
        throw new IllegalStateException("no multi-row table found in " + PROJECT);
    }
}
