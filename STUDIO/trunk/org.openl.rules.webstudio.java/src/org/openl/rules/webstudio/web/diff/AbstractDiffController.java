package org.openl.rules.webstudio.web.diff;

import java.util.ArrayList;
import java.util.List;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;

import org.openl.rules.table.ITable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.filters.ColorGridFilter;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.hierarchy.ProjectionProperty;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls.XlsProjectionType;
import org.openl.rules.diff.util.DiffHelper;

public abstract class AbstractDiffController {
    private String currentNodeId;
    private DiffTreeNode diffTree;
    private boolean showEqualElements = false;

    public abstract String compare();

    public DiffTreeNode[] getDiffTreeNodes() {
        DiffTreeNode tree = DiffHelper.getDiffNodeById(getDiffTree(), getCurrentNodeId());
        if (tree != null) {
            return tree.getChildren();
        }
        return new DiffTreeNode[0];
    }

    public String getCurrentNodeId() {
        currentNodeId = FacesUtils.getRequestParameter("id");
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public boolean isShowEqualElements() {
        return showEqualElements;
    }

    public void setShowEqualElements(boolean showEqualElements) {
        this.showEqualElements = showEqualElements;
    }

    public ITable getTable1() {
        return getTable(0);
    }

    public ITable getTable2() {
        return getTable(1);
    }

    public ITable getTable(int i) {
        DiffTreeNode tree = DiffHelper.getDiffNodeById(getDiffTree(), getCurrentNodeId());
        if (tree != null) {
            DiffElement[] elems = tree.getElements();
            if (elems.length > i) {
                Projection projection = elems[i].getProjection();
                if (projection != null) {
                    String projType = projection.getType();
                    if (projType.equalsIgnoreCase((XlsProjectionType.TABLE.name()))) {
                        ProjectionProperty[] props = projection.getProperties();
                        return (ITable) DiffHelper.getPropValue(props, "grid");
                    }
                }
            }
        }
        return null;
    }

    public IGridFilter getFilter2() {
        return getFilter(1);
    }

    public IGridFilter getFilter(int i) {
        DiffTreeNode tree = DiffHelper.getDiffNodeById(getDiffTree(), getCurrentNodeId());
        if (tree != null) {
            DiffElement[] elems = tree.getElements();
            if (elems.length > i) {
                Projection projection = elems[i].getProjection();
                if (projection != null) {
                    String projType = projection.getType();
                    if (projType.equalsIgnoreCase((XlsProjectionType.TABLE.name()))) {
                        ProjectionProperty[] props = projection.getProperties();
                        ITable table = (ITable) DiffHelper.getPropValue(props, "grid");
                        List<DiffTreeNode> cells = DiffHelper.getDiffNodesByType(tree, XlsProjectionType.CELL.name());
                        List<ICell> diffCells = new ArrayList<ICell>();
                        for (DiffTreeNode cellNode : cells) {
                            DiffElement cellElem = cellNode.getElements()[i];
                            if (!cellElem.isSelfEqual()) {
                                ProjectionProperty[] cellProps = cellElem.getProjection().getProperties();
                                ICell cell = (ICell) DiffHelper.getPropValue(cellProps, "cell");
                                diffCells.add(cell);
                            }
                        }
                        return makeFilter(table.getGridTable(), diffCells);
                    }
                }
            }
        }
        return null;
    }

    private IGridFilter makeFilter(IGridTable table, List<ICell> selectedCells) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        for (ICell cell : selectedCells) {
            IGridRegion region = table.getRegion(cell.getColumn(), cell.getRow(), 1, 1)
                .getGridTable().getRegion();
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
    }

    public DiffTreeNode getDiffTree() {
        return diffTree;
    }

}
