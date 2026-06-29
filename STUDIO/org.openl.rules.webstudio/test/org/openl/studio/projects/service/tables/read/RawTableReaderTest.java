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

        RawTableView capped = new RawTableReader().read(table, 1);

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
        RawTableView wide = new RawTableReader().read(table, full.source.size() + 10);
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
        RawTableView styled = new RawTableReader().read(table, RawTableReader.ALL_ROWS, true);
        assertEquals(plain.source.size(), styled.source.size());
        assertTrue(styled.source.stream().flatMap(List::stream).anyMatch(c -> c.style() != null),
                "a styled read must attach at least one cell style");
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
