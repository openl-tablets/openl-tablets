package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.hierarchy.ProjectionProperty;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.tree.DiffTreeNode;
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
import org.openl.util.FileUtils;
import org.richfaces.component.UITree;
import org.richfaces.event.TreeSelectionChangeEvent;

public abstract class AbstractDiffController {
    // TODO remove?
    private boolean showEqualElements = false;

    private TreeNode richDiffTree;
    private TreeNode selectedNode;
    private List<File> tempFiles = new ArrayList<>();

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
                    if (projType.equalsIgnoreCase(XlsProjectionType.TABLE.name())) {
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
            return projection.getTable();
        }
        return null;
    }

    public IGridFilter getFilter1() {
        return getFilter(0);
    }

    public IGridFilter getFilter2() {
        return getFilter(1);
    }

    public IGridFilter getFilter(int i) {
        XlsProjection projection = projectionTable(i);
        if (projection != null) {
            IOpenLTable table = projection.getTable();
            List<ICell> diffCells = projection.getDiffCells();
            if (diffCells != null) {
                return makeFilter(table.getGridTable(), diffCells);
            }
        }
        return null;
    }

    private IGridFilter makeFilter(IGridTable table, List<ICell> selectedCells) {
        List<IGridRegion> regions = new ArrayList<>();
        for (ICell cell : selectedCells) {
            IGridRegion region = table.getSubtable(cell.getColumn(), cell.getRow(), 1, 1).getRegion();
            regions.add(region);
        }
        if (regions.isEmpty()) {
            return null;
        }
        IGridRegion[] aRegions = regions.toArray(new IGridRegion[0]);
        return new ColorGridFilter(new RegionGridSelector(aRegions, true),
            WebStudioUtils.getWebStudio().getModel().getFilterHolder().makeFilter());
    }

    public void setDiffTree(DiffTreeNode diffTree) {
        if (diffTree == null) {
            richDiffTree = null;
        } else {
            // for a sake of auto increment counter only
            AtomicInteger idGenerator = new AtomicInteger();

            richDiffTree = new TreeNode(diffTree);

            rebuild(idGenerator, richDiffTree);
        }
        // reset selection
        selectedNode = null;
    }

    private void rebuild(AtomicInteger idGenerator, TreeNode parent) {
        for (DiffTreeNode d : parent.getDiffTreeNode().getChildren()) {
            List<PropertyNode> propertyNodes = getPropertyNodes(d);

            TreeNode node = new TreeNode(d, propertyNodes.isEmpty() && !hasChildren(d));

            if (shouldSkipNode(node)) {
                continue;
            }

            // Skip empty sheets
            if (node.getType().equals(XlsProjectionType.SHEET.name()) && node.isLeaf()) {
                continue;
            }

            parent.addChild(String.valueOf(idGenerator.getAndIncrement()), node);

            // props
            for (PropertyNode propertyNode : propertyNodes) {
                node.addChild(String.valueOf(idGenerator.getAndIncrement()), propertyNode);
            }

            rebuild(idGenerator, node);
        }
    }

    private boolean hasChildren(DiffTreeNode diff) {
        for (DiffTreeNode node : diff.getChildren()) {
            if (!shouldSkipNode(new TreeNode(node))) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldSkipNode(TreeNode node) {
        String type = node.getType();
        if (type.equals(XlsProjectionType.GRID.name())) {
            // don't show tree deeper
            return true;
            // TODO implement XlsProjectionDiffer and you don't need that check
        }

        // skip equal elements
        return type
            .equals(XlsProjectionType.TABLE.name()) && !showEqualElements && isEqualElements(node.getDiffTreeNode());
    }

    private List<PropertyNode> getPropertyNodes(DiffTreeNode d) {
        List<PropertyNode> propertyNodes = new ArrayList<>();

        Projection p1 = d.getElement(0).getProjection();
        Projection p2 = d.getElement(1).getProjection();
        if (p1 != null && p2 != null && !isEqualElements(d)) {
            for (ProjectionProperty pp1 : p1.getProperties()) {
                ProjectionProperty pp2 = p2.getProperty(pp1.getName());

                if (pp2 != null) {
                    Object v1 = pp1.getRawValue();
                    Object v2 = pp2.getRawValue();
                    if (v1 != null && v2 != null) {
                        if (!v1.equals(v2)) {
                            if (pp1.getName().equals("name")) {
                                v1 = "";
                            }
                            String s = pp1.getName() + ": " + v1 + " -> " + v2;

                            PropertyNode np = new PropertyNode(d, s);
                            propertyNodes.add(np);
                        }
                    }
                }
            }
        }
        return propertyNodes;
    }

    private boolean isEqualElements(DiffTreeNode d) {
        return d.getElement(1).isSelfEqual();
    }

    public TreeNode getRichDiffTree() {
        return richDiffTree;
    }

    public void processSelection(TreeSelectionChangeEvent event) {
        List<Object> selection = new ArrayList<>(event.getNewSelection());
        Object currentSelectionKey = selection.get(0);
        UITree tree = (UITree) event.getSource();

        Object storedKey = tree.getRowKey();
        tree.setRowKey(currentSelectionKey);
        selectedNode = (TreeNode) tree.getRowData();
        tree.setRowKey(storedKey);
    }

    protected void addTempFile(File tempFile) {
        if (tempFile != null) {
            tempFiles.add(tempFile);
        }
    }

    protected void deleteTempFiles() {
        for (File file : tempFiles) {
            FileUtils.deleteQuietly(file);
        }

        tempFiles.clear();
    }
}
