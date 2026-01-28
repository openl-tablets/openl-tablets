package org.openl.studio.projects.service.trace;

import java.util.ArrayList;
import java.util.List;

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
 * Service for rendering traced tables as HTML fragments.
 */
@Service
public class TraceTableHtmlService {

    /**
     * Renders the traced table as an HTML fragment.
     *
     * @param traceHelper  the trace helper containing cached trace objects
     * @param nodeId       the trace node ID
     * @param projectModel the project model for filter holder access
     * @param showFormulas whether to show formulas instead of values
     * @return HTML string representing the traced table
     */
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

        // 5. Render HTML
        return new HTMLRenderer.TableRenderer(tableModel).render(showFormulas);
    }

    /**
     * Gets the TableSyntaxNode for a trace object.
     * Replicates ShowTraceTableBean.getTableSyntaxNode() logic.
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
     * Replicates ShowTraceTableBean.getTraceFilters() logic.
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

        IGridRegion[] aRegions = regions.toArray(new IGridRegion[0]);
        RegionGridSelector gridSelector = new RegionGridSelector(aRegions, true);
        ColorGridFilter colorGridFilter = new ColorGridFilter(gridSelector, defaultColorFilter);
        return new IGridFilter[]{colorGridFilter};
    }

    /**
     * Recursively fills regions from trace object children.
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
