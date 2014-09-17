/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.apache.commons.collections4.CollectionUtils;
import org.openl.base.INamedThing;
import org.openl.rules.dt.trace.DTRuleTracerLeaf;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ITableTracerObject;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.filters.ColorGridFilter;
import org.openl.rules.table.ui.filters.IColorFilter;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.ui.tree.TreeCache;
import org.openl.util.tree.ITreeElement;
import org.openl.vm.trace.ITracerObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author snshor
 */
public class TraceHelper {

    private TreeCache<Integer, ITreeElement<?>> traceTreeCache = new TreeCache<Integer, ITreeElement<?>>();
    private int treeElementsNumber = 0;

    private void fillRegions(ITableTracerObject tto, List<IGridRegion> regions) {
        for (ITableTracerObject child : tto.getTableTracers()) {
            List<IGridRegion> r = child.getGridRegions();
            if (CollectionUtils.isNotEmpty(r)) {
                regions.addAll(r);
            } else if (!child.isLeaf()) {
                fillRegions(child, regions);
            }
        }
    }

    public ITableTracerObject getTableTracer(int elementId) {
        ITreeElement<?> node = traceTreeCache.getNode(elementId);
        if (node instanceof ITableTracerObject) {
            return (ITableTracerObject) node;
        }
        return null;
    }

    public String getTracerUri(int elementId) {
        ITableTracerObject tto = getTableTracer(elementId);
        if (tto != null) {
            TableSyntaxNode tsn = tto.getTableSyntaxNode();
            return tsn.getUri();
        }
        return null;
    }

    public String getTracerName(int elementId) {
        ITableTracerObject tto = getTableTracer(elementId);
        String displayName = null;

        if (tto != null) {
            if (tto instanceof ATableTracerNode) {
                displayName = tto.getDisplayName(INamedThing.REGULAR);
            } else {
                // ATableTracerLeaf
                displayName = tto.getParent().getDisplayName(INamedThing.REGULAR) + ": "
                        + tto.getDisplayName(INamedThing.REGULAR);
            }
        }

        return displayName;
    }

    public Throwable getTracerError(int elementID) {
        ITableTracerObject tracer = getTableTracer(elementID);
        Throwable error = null;

        if (tracer != null) {
            if (tracer instanceof ATableTracerNode) {
                error = ((ATableTracerNode) tracer).getError();
            }
        }

        return error;
    }

    public IGridFilter[] makeFilters(int elementId, ProjectModel model) {
        ITableTracerObject tto = getTableTracer(elementId);
        if (tto == null) {
            return null;
        }

        IColorFilter defaultColorFilter = model.getFilterHolder().makeFilter();
        if (tto instanceof DecisionTableTraceObject || tto instanceof DTRuleTracerLeaf) {
            return new DecisionTableTraceFilterFactory(tto, defaultColorFilter).createFilters();
        }

        List<IGridRegion> regions = new ArrayList<IGridRegion>();

        List<IGridRegion> r = tto.getGridRegions();
        if (CollectionUtils.isNotEmpty(r)) {
            regions.addAll(r);
        } else {
            fillRegions(tto, regions);
        }

        IGridRegion[] aRegions = new IGridRegion[regions.size()];
        aRegions = regions.toArray(aRegions);

        RegionGridSelector gridSelector = new RegionGridSelector(aRegions, true);
        ColorGridFilter colorGridFilter = new ColorGridFilter(gridSelector, defaultColorFilter);
        return new IGridFilter[]{colorGridFilter};
    }

    public void cacheTraceTree(ITreeElement<ITracerObject> tree) {
        cleanCachedTree();
        cacheTree(tree);
    }

    private void cleanCachedTree() {
        traceTreeCache.clear();
        treeElementsNumber = 0;
    }

    private void cacheTree(ITreeElement<ITracerObject> treeNode) {
        Iterable<? extends ITreeElement<ITracerObject>> children = treeNode.getChildren();
        for (ITreeElement<ITracerObject> child : children) {
            traceTreeCache.put(treeElementsNumber++, child);
            cacheTree(child);
        }
    }

    public Integer getNodeKey(ITreeElement<?> node) {
        return traceTreeCache.getKey(node);
    }

    public IOpenLTable getTraceTable(int elementId) {
        ITableTracerObject tto = getTableTracer(elementId);
        TableSyntaxNode tsn = tto.getTableSyntaxNode();
        return new TableSyntaxNodeAdapter(tsn);
    }
}
