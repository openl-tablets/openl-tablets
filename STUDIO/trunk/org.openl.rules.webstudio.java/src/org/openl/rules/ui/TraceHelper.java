/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ITable;
import org.openl.rules.table.ITableTracerObject;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.filters.ColorGridFilter;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.ui.tree.TreeCache;
import org.openl.types.IOpenMethodHeader;
import org.openl.util.tree.ITreeElement;
import org.openl.vm.ITracerObject;
import org.openl.vm.Tracer;

/**
 * @author snshor
 * 
 */
public class TraceHelper {

    private TreeCache<Integer, ITreeElement<?>> traceTreeCache = new TreeCache<Integer, ITreeElement<?>>();
    private int treeElementsNumber = 0;

    private void fillRegions(ITableTracerObject tto, List<IGridRegion> regions) {
        for (ITableTracerObject child : tto.getTableTracers()) {
            IGridRegion r = child.getGridRegion();
            if (r != null) {
                regions.add(r);
            } else if (!child.isLeaf()) {
                fillRegions(child, regions);
            }
        }
    }

    public String getProjectNodeUri(int id, ProjectModel model) {
        ITracerObject tracer = (ITracerObject) traceTreeCache.getNode(id);
        if (!(tracer instanceof ITableTracerObject)) {
            return null;
        }
        ITableTracerObject tableTracer = (ITableTracerObject) tracer;
        return tableTracer.getTableSyntaxNode().getUri();
    }

    public TableInfo getTableInfo(int elementID) {
        ITracerObject tt = (ITracerObject) traceTreeCache.getNode(elementID);

        if (tt == null) {
            return null;
        }

        if (!(tt instanceof ITableTracerObject)) {
            return null;
        }

        ITableTracerObject tto = (ITableTracerObject) tt;
        TableSyntaxNode tsn = tto.getTableSyntaxNode();
        IGridTable gt = tsn.getTable().getGridTable();

        String displayName = null;

        if (tto instanceof ATableTracerNode) {
            displayName = tto.getDisplayName(INamedThing.REGULAR);
        } else {
            // ATableTracerLeaf
            displayName = tto.getParent().getDisplayName(INamedThing.REGULAR) + ": "
                    + tto.getDisplayName(INamedThing.REGULAR);
        }

        return new TableInfo(gt, displayName, false);
    }

    IGridFilter makeFilter(ITableTracerObject tto, ProjectModel model) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();

        IGridRegion r = tto.getGridRegion();
        if (r != null) {
            regions.add(r);
        } else {
            fillRegions(tto, regions);
        }

        IGridRegion[] aRegions = new IGridRegion[regions.size()];
        aRegions = regions.toArray(aRegions);

        return new ColorGridFilter(new RegionGridSelector(aRegions, true), model.getFilterHolder().makeFilter());
    }

    public String printTraceMethod(IOpenMethodHeader header, Object[] params, StringBuffer buf, ProjectModel model) {
        buf.append(header.getName()).append('(');
        ObjectViewer viewer = new ObjectViewer(model);

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                buf.append(",");
            }
            buf.append(viewer.displayResult(params[i]));
        }

        buf.append(')');
        return buf.toString();
    }

    public ITreeElement<?> getTraceTree(Tracer tracer) {
        ITreeElement<?> tree = tracer.getRoot();
        cleanCachedTree();
        cacheTree(tree);
        return tree;
    }

    private void cleanCachedTree() {
        traceTreeCache.clear();
        treeElementsNumber = 0;
    }

    private void cacheTree(ITreeElement<?> treeNode) {
        for (Iterator<?> iterator = treeNode.getChildren(); iterator.hasNext();) {
            ITreeElement<?> child = (ITreeElement<?>) iterator.next();
            traceTreeCache.put(treeElementsNumber++, child);
            cacheTree(child);
        }
    }

    public int getNodeKey(ITreeElement<?> node) {
        return traceTreeCache.getKey(node);
    }

    public String showTrace(int id, ProjectModel model, String view) {
        ITracerObject tt = (ITracerObject) traceTreeCache.getNode(id);

        if (tt == null) {
            return "ERROR ID = " + id;
        }

        if (!(tt instanceof ITableTracerObject)) {
            return "----";
        }

        ITableTracerObject tto = (ITableTracerObject) tt;
        TableSyntaxNode tsn = tto.getTableSyntaxNode();

        ITable table = new TableSyntaxNodeAdapter(tsn);
        view = model.getTableView(view);
        IGridTable gt = new TableEditorModel(table, view, false).getUpdatedTable();

        TableModel tableModel = ProjectModel.buildModel(gt, new IGridFilter[] { makeFilter(tto, model) });
        // TODO: Show formulas in trace
        return new HTMLRenderer.TableRenderer(tableModel).render(false);
    }
}
