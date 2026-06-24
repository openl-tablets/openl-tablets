package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;

/**
 * Regression test for EPBDS-16151.
 * <p>
 * The traced table HTML is rendered by the backend and injected into the React UI as real HTML.
 * Cell content reaching {@link TraceTableHtmlServiceImpl} is already HTML-safe: data characters are
 * escaped once in {@code TableViewer.buildCell()}. The renderer used to escape it a second time and
 * then restore only {@code &nbsp;} and {@code <br>}. That double-encoded every other special
 * character (for example {@code <} became {@code &amp;lt;}), which the browser showed literally as
 * {@code &lt;} instead of {@code <}.
 * </p>
 */
class TraceTableHtmlServiceImplTest {

    private static final String PROJECT = "test-resources/org/openl/studio/projects/service/trace/EPBDS-16151";

    @Test
    @DisplayName("Traced table cell content keeps single HTML escaping for the React UI")
    void specialCharactersInTracedTableAreNotDoubleEscaped() throws Exception {
        String html = renderAllTables();

        // The cell value contains <, >, " which escapeHtml4 turns into the single entities below.
        // The React UI renders this HTML, so the entities must stay single-escaped to show the real
        // characters. A redundant second escape produces "&amp;lt;" etc. (shown literally as "&lt;").
        assertFalse(html.contains("&amp;lt;"), () -> "'<' must not be double-escaped: " + html);
        assertFalse(html.contains("&amp;gt;"), () -> "'>' must not be double-escaped: " + html);
        assertFalse(html.contains("&amp;quot;"), () -> "'\"' must not be double-escaped: " + html);
        assertFalse(html.contains("&amp;amp;"), () -> "'&' must not be double-escaped: " + html);

        // The cell really contains <, > and "; they must survive as single HTML entities.
        assertTrue(html.contains("&lt;"), () -> "'<' must be present as a single HTML entity: " + html);
        assertTrue(html.contains("&gt;"), () -> "'>' must be present as a single HTML entity: " + html);
        assertTrue(html.contains("&quot;"), () -> "'\"' must be present as a single HTML entity: " + html);
        // Characters without an HTML4 entity must pass through verbatim.
        assertTrue(html.contains("№"), () -> "'№' must be rendered verbatim: " + html);
    }

    /**
     * Renders every parsed table the same way {@link TraceTableHtmlServiceImpl} renders a traced
     * table (trace filters only add cell highlighting, not content, so they are omitted here).
     */
    private static String renderAllTables() throws Exception {
        ProjectModel projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(ProjectResolver.getInstance()
                .resolve(Path.of(PROJECT))
                .getModules()
                .getFirst());

        var service = new TraceTableHtmlServiceImpl();
        var html = new StringBuilder();
        for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
            IOpenLTable table = new TableSyntaxNodeAdapter(tsn);
            IGridTable gridTable = table.getGridTable(null);
            int numRows = HTMLRenderer.getMaxNumRowsToDisplay(gridTable);
            TableModel tableModel = TableModel.initializeTableModel(
                    gridTable, null, numRows, null, null, null, table.getMetaInfoReader(), false, null);
            if (tableModel != null) {
                html.append(service.renderTableHtml(tableModel, false));
            }
        }
        assertNotNull(html.toString());
        return html.toString();
    }
}
