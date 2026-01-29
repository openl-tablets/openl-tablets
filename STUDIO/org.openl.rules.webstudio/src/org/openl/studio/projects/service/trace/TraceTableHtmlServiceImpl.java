package org.openl.studio.projects.service.trace;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.stereotype.Service;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.filters.ColorGridFilter;
import org.openl.rules.table.ui.filters.IColorFilter;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.ICellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.DecisionTableTraceFilterFactory;
import org.openl.rules.webstudio.web.trace.RegionsExtractor;
import org.openl.rules.webstudio.web.trace.node.ATableTracerNode;
import org.openl.rules.webstudio.web.trace.node.DTRuleTracerLeaf;
import org.openl.rules.webstudio.web.trace.node.DecisionTableTraceObject;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.trace.node.RefToTracerNodeObject;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.util.CollectionUtils;

/**
 * Default implementation of {@link TraceTableHtmlService}.
 * <p>
 * Renders traced tables as HTML fragments by applying trace filters that highlight
 * cells accessed during rule execution. The implementation reuses the legacy
 * {@code ShowTraceTableBean} logic while producing clean HTML suitable for
 * the React UI (no JSF dependencies, no inline scripts).
 * </p>
 */
@Service
public class TraceTableHtmlServiceImpl implements TraceTableHtmlService {

    /**
     * {@inheritDoc}
     */
    @Override
    public String renderTraceTableHtml(TraceHelper traceHelper,
                                       int nodeId,
                                       ProjectModel projectModel,
                                       boolean showFormulas) {
        // 1. Get trace object
        ITracerObject tto = traceHelper.getTableTracer(nodeId);
        if (tto == null) {
            throw new NotFoundException("trace.node.not.found.message");
        }

        // 2. Get table (replicates ShowTraceTableBean.getTraceTable())
        TableSyntaxNode tsn = getTableSyntaxNode(tto, projectModel);
        if (tsn == null) {
            throw new NotFoundException("trace.node.not.found.message");
        }
        IOpenLTable table = new TableSyntaxNodeAdapter(tsn);

        // 3. Create filters (replicates ShowTraceTableBean.getTraceFilters())
        IGridFilter[] filters = createTraceFilters(tto, projectModel);

        // 4. Build TableModel with filters applied
        IGridTable gridTable = table.getGridTable(null);
        int numRows = HTMLRenderer.getMaxNumRowsToDisplay(gridTable);

        TableModel tableModel = TableModel.initializeTableModel(
                gridTable,
                filters,
                numRows,
                null,  // linkBuilder
                null,  // mode
                null,  // view
                table.getMetaInfoReader(),
                false, // smartNumbers
                null   // modifiedCells
        );

        if (tableModel == null) {
            throw new NotFoundException("trace.node.not.found.message");
        }

        // 5. Render HTML (standalone version without JSF dependencies)
        return renderTableHtml(tableModel, showFormulas);
    }

    /**
     * Renders table model to HTML without JSF dependencies.
     * <p>
     * Produces clean HTML compatible with React/TypeScript UI:
     * <ul>
     *   <li>No script tags (comments are stored in data-comment attribute)</li>
     *   <li>Clean cell IDs (row_col format instead of row:col)</li>
     *   <li>Inline styles for portability</li>
     * </ul>
     * </p>
     *
     * @param tableModel   the table model to render
     * @param showFormulas whether to show formulas instead of values
     * @return HTML string representing the table
     */
    private String renderTableHtml(TableModel tableModel, boolean showFormulas) {
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"te_table\">\n");

        ICellModel[][] cells = tableModel.getCells();
        for (int row = 0; row < cells.length; row++) {
            html.append("<tr>\n");
            for (int col = 0; col < cells[row].length; col++) {
                ICellModel cell = cells[row][col];
                if (cell == null || !cell.isReal()) {
                    continue;
                }

                html.append("<td");

                // Add cell styling
                if (cell instanceof CellModel cellModel) {
                    cellModel.attributesToHtml(html, false);
                }

                // Clean cell ID (no colons)
                html.append(" id=\"cell_").append(row + 1).append("_").append(col + 1).append("\"");

                // Store comment in data attribute (React can handle this)
                if (cell.getComment() != null) {
                    html.append(" class=\"te_comment\"");
                    html.append(" data-comment=\"")
                            .append(StringEscapeUtils.escapeHtml4(cell.getComment()))
                            .append("\"");
                }

                html.append(">");
                html.append(cell.getContent(showFormulas));
                html.append("</td>\n");
            }
            html.append("</tr>\n");
        }
        html.append("</table>");

        // Large table warning
        if (tableModel.getNumRowsToDisplay() > -1) {
            html.append("<div class=\"te_bigtable_mes\">")
                    .append("<div class=\"te_bigtable_mes_header\">The table is displayed partially (the first ")
                    .append(tableModel.getNumRowsToDisplay())
                    .append(" rows).</div>")
                    .append("<div>To view the full table, use 'Open In Excel'.</div>")
                    .append("</div>");
        }

        return html.toString();
    }

    /**
     * Gets the TableSyntaxNode for a trace object.
     * <p>
     * Replicates ShowTraceTableBean.getTableSyntaxNode() logic to find the
     * syntax node associated with a trace object. Handles both direct table
     * nodes and reference nodes.
     * </p>
     *
     * @param tto          the trace object
     * @param projectModel the project model for URI-based lookup
     * @return the table syntax node, or null if not found
     */
    private TableSyntaxNode getTableSyntaxNode(ITracerObject tto, ProjectModel projectModel) {
        TableSyntaxNode syntaxNode = null;

        if (tto instanceof ATableTracerNode tableNode) {
            if (tableNode.getTraceObject() != null) {
                syntaxNode = tableNode.getTraceObject().getSyntaxNode();
            }
        } else if (tto instanceof RefToTracerNodeObject refNode) {
            return getTableSyntaxNode(refNode.getOriginalTracerNode(), projectModel);
        }

        if (syntaxNode == null) {
            // Default approach for TBasic nodes or if traced object does not have syntax node
            String uri = tto.getUri();
            return projectModel.findNode(uri);
        }

        return syntaxNode;
    }

    /**
     * Creates trace filters for visualization.
     * <p>
     * Replicates ShowTraceTableBean.getTraceFilters() logic to create filters
     * that highlight cells accessed during trace execution. Handles special
     * cases for Decision Tables which require specialized filter factories.
     * </p>
     *
     * @param tto          the trace object
     * @param projectModel the project model for filter holder access
     * @return array of grid filters for cell highlighting
     */
    private IGridFilter[] createTraceFilters(ITracerObject tto, ProjectModel projectModel) {
        IColorFilter defaultColorFilter = projectModel.getFilterHolder().makeFilter();

        // Decision Table special handling
        if (tto instanceof DecisionTableTraceObject || tto instanceof DTRuleTracerLeaf) {
            return new DecisionTableTraceFilterFactory(tto, defaultColorFilter).createFilters();
        }

        // Generic approach: collect regions and create ColorGridFilter
        List<IGridRegion> regions = new ArrayList<>();
        List<IGridRegion> r = RegionsExtractor.getGridRegions(tto);

        if (CollectionUtils.isNotEmpty(r)) {
            regions.addAll(r);
        } else {
            fillRegions(tto, regions);
        }

        if (regions.isEmpty()) {
            return new IGridFilter[0];
        }

        IGridRegion[] aRegions = regions.toArray(IGridRegion.EMPTY_REGION);
        RegionGridSelector gridSelector = new RegionGridSelector(aRegions, true);
        ColorGridFilter colorGridFilter = new ColorGridFilter(gridSelector, defaultColorFilter);
        return new IGridFilter[]{colorGridFilter};
    }

    /**
     * Recursively fills regions from trace object children.
     *
     * @param tto     the trace object to traverse
     * @param regions list to collect grid regions into
     */
    private void fillRegions(ITracerObject tto, List<IGridRegion> regions) {
        for (ITracerObject child : tto.getChildren()) {
            List<IGridRegion> r = RegionsExtractor.getGridRegions(child);
            if (CollectionUtils.isNotEmpty(r)) {
                regions.addAll(r);
            } else if (!child.isLeaf()) {
                fillRegions(child, regions);
            }
        }
    }
}
