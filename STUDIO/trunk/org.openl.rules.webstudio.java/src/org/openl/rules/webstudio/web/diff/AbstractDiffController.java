package org.openl.rules.webstudio.web.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.util.DiffHelper;
import org.openl.rules.diff.xls.XlsProjection;
import org.openl.rules.diff.xls.XlsProjectionType;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.filters.ColorGridFilter;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.richfaces.component.UITree;
import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

public abstract class AbstractDiffController {
    private DiffTreeNode diffTree;
    private boolean showEqualElements = false;

    public abstract String compare();

    public boolean isShowEqualElements() {
        return showEqualElements;
    }

    public void setShowEqualElements(boolean showEqualElements) {
        this.showEqualElements = showEqualElements;
    }

    public IOpenLTable getTable1() {
        return getTable(0);
    }

    public IOpenLTable getTable2() {
        return getTable(1);
    }

    private DiffTreeNode getCurrentDiffTreeNode() {
        DiffTreeNode tree = null;
        if (selectedNode != null) {
            tree = selectedNode.getDiffTreeNode();
        }
        return tree;
    }

    protected XlsProjection projectionTable(int i) {
        DiffTreeNode tree = getCurrentDiffTreeNode();
        if (tree != null) {
            DiffElement[] elems = tree.getElements();
            if (elems.length > i) {
                Projection projection = elems[i].getProjection();
                if (projection != null) {
                    String projType = projection.getType();
                    if (projType.equalsIgnoreCase((XlsProjectionType.TABLE.name()))) {
                        return (XlsProjection) projection;
                    }
                }
            }
        }
        return null;
    }

    public IOpenLTable getTable(int i) {
        XlsProjection projection = projectionTable(i);
        if (projection != null) {
            return (IOpenLTable) projection.getData();
        }
        return null;
    }

    public IGridFilter getFilter2() {
        return getFilter(1);
    }

    public IGridFilter getFilter(int i) {
        XlsProjection projection = projectionTable(i);
        if (projection != null) {
            IOpenLTable table = (IOpenLTable) projection.getData();
            List<DiffTreeNode> cells = DiffHelper.getDiffNodesByType(getCurrentDiffTreeNode(), XlsProjectionType.CELL.name());
            List<ICell> diffCells = new ArrayList<ICell>();
            for (DiffTreeNode cellNode : cells) {
                DiffElement cellElem = cellNode.getElements()[i];
                if (!cellElem.isSelfEqual()) {
                    XlsProjection p = (XlsProjection) cellElem.getProjection();
                    if (p != null) {
                        ICell cell = (ICell) p.getData();
                        diffCells.add(cell);
                    }
                }
            }
            return makeFilter(table.getGridTable(), diffCells);
        }
        return null;
    }

    private IGridFilter makeFilter(IGridTable table, List<ICell> selectedCells) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        for (ICell cell : selectedCells) {
            IGridRegion region = table.getSubtable(cell.getColumn(), cell.getRow(), 1, 1).getRegion();
            regions.add(region);
        }
        if (regions.isEmpty()) {
            return null;
        }
        IGridRegion[] aRegions = regions.toArray(new IGridRegion[regions.size()]);
        return new ColorGridFilter(new RegionGridSelector(aRegions, true),
                WebStudioUtils.getWebStudio().getModel().getFilterHolder().makeFilter());
    }

    public void setDiffTree(DiffTreeNode diffTree) {
        this.diffTree = diffTree;

        if (diffTree == null) {
            richDiffTree = null;
        } else {
            // for a sake of auto increment counter only
            AtomicInteger idGenerator = new AtomicInteger();

            richDiffTree = new TreeNodeImpl<UiTreeData>();
            richDiffTree.setData(new UiTreeData(diffTree));

            rebuild(idGenerator, richDiffTree, diffTree);
        }
        // TODO do we need diffTree?
    }

    private void rebuild(AtomicInteger idGenerator, TreeNode<UiTreeData> parent, DiffTreeNode diff) {
        for (DiffTreeNode d : diff.getChildren()) {
            UiTreeData ui = new UiTreeData(d);

            String type = ui.getType();
            if (type.equals(XlsProjectionType.GRID.name())) {
                // don't show tree deeper
                continue;
                // TODO implement XlsProjectionDiffer and you don't need that check
            }

            TreeNode<UiTreeData> n = new TreeNodeImpl<UiTreeData>();
            n.setData(ui);
            parent.addChild(idGenerator.getAndIncrement(), n);

            rebuild(idGenerator, n, d);
        }
    }

    public DiffTreeNode getDiffTree() {
        return diffTree;
    }

    private TreeNode<UiTreeData> richDiffTree;

    public TreeNode<UiTreeData> getRichDiffTree() {
        return richDiffTree;
    }

    protected UiTreeData selectedNode;

    public void processSelection(NodeSelectedEvent event) {
        UITree tree = (UITree) event.getComponent();

        try {
            selectedNode = (UiTreeData) tree.getRowData();
        } catch (IllegalStateException ex) {
            // If nothing selected in tree then invalidate selection. 
            selectedNode = null;
        }
    }
}
