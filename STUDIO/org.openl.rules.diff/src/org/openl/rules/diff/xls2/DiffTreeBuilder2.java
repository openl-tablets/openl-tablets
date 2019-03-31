package org.openl.rules.diff.xls2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.diff.hierarchy.AbstractProperty;
import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.tree.DiffElementImpl;
import org.openl.rules.diff.tree.DiffTreeBuilderImpl;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.tree.DiffTreeNodeImpl;
import org.openl.rules.diff.xls.XlsProjection;
import org.openl.rules.diff.xls.XlsProjectionType;
import org.openl.rules.table.IGridTable;

public class DiffTreeBuilder2 extends DiffTreeBuilderImpl {
    // @Override
    public DiffTreeNode compare(Projection[] projections) {
        throw new UnsupportedOperationException("Use compare()!");
    }

    // @Override
    protected void buildTree(DiffTreeNodeImpl root, Projection[] projections) {
        throw new UnsupportedOperationException("It uses own builder! Must not use inherited one!");
    }

    protected DiffTreeNodeImpl newDiffTreeNode() {
        DiffTreeNodeImpl n = new DiffTreeNodeImpl();
        DiffElement[] a = new DiffElementImpl[2];
        n.setElements(a);
        n.setChildren(new ArrayList<DiffTreeNode>());

        return n;
    }

    private Map<String, List<DiffPair>> sheetMap = new LinkedHashMap<>();

    public void add(DiffPair diff) {
        String sheetName = (diff.getTable1() != null) ? diff.getTable1().getSheetName()
                                                      : diff.getTable2().getSheetName();
        List<DiffPair> list = sheetMap.computeIfAbsent(sheetName, e -> new ArrayList<>());
        list.add(diff);
    }

    protected void buildTree(DiffTreeNodeImpl root) {
        root.getElements()[0] = new DiffElementImpl(new XlsProjection("book1", XlsProjectionType.BOOK));
        root.getElements()[1] = new DiffElementImpl(new XlsProjection("book2", XlsProjectionType.BOOK));

        for (Map.Entry<String, List<DiffPair>> entry : sheetMap.entrySet()) {
            // Create Sheet Node
            String sheetName = entry.getKey();
            List<DiffPair> diffs = entry.getValue();

            int n1 = 0;
            int n2 = 0;
            for (DiffPair r : diffs) {
                if (r.getTable1() != null)
                    n1++;
                if (r.getTable2() != null)
                    n2++;
            }

            DiffTreeNodeImpl sheetNode = newDiffTreeNode();
            root.getChildren().add(sheetNode);

            XlsProjection p1 = null;
            XlsProjection p2 = null;

            if (n1 > 0 && n2 > 0) {
                // in both v1 & v2
                p1 = new XlsProjection(sheetName, XlsProjectionType.SHEET);
                p2 = new XlsProjection(sheetName, XlsProjectionType.SHEET);
            } else if (n1 > 0) {
                // REMOVED in v2
                p1 = new XlsProjection(sheetName, XlsProjectionType.SHEET);
            } else {
                // n2 > 0 -- ADDED in v2
                p2 = new XlsProjection(sheetName, XlsProjectionType.SHEET);
            }

            sheetNode.getElements()[0] = new DiffElementImpl(p1);
            sheetNode.getElements()[1] = new DiffElementImpl(p2);

            // Create Table Nodes in a Sheet
            for (DiffPair r : diffs) {
                DiffTreeNodeImpl tableNode = newDiffTreeNode();
                sheetNode.getChildren().add(tableNode);

                XlsProjection tp1 = null;
                XlsProjection tp2 = null;

                XlsTable t1 = r.getTable1();
                XlsTable t2 = r.getTable2();

                if (t1 != null) {
                    tp1 = new XlsProjection(t1.getTableName(), XlsProjectionType.TABLE);
                    tp1.setData(t1.getTable());
                    fillProps(tp1, t1);

                    tp1.setDiffCells(r.getDiffCells1());
                }
                if (r.getTable2() != null) {
                    tp2 = new XlsProjection(t2.getTableName(), XlsProjectionType.TABLE);
                    tp2.setData(t2.getTable());
                    fillProps(tp2, t2);

                    tp2.setDiffCells(r.getDiffCells2());
                }

                tableNode.getElements()[0] = new DiffElementImpl(tp1);
                tableNode.getElements()[1] = new DiffElementImpl(tp2);
            }
        }
    }

    public void fillProps(XlsProjection projection, XlsTable xlsTable) {
        projection.addProperty(new AbstractProperty("name", xlsTable.getTableName()));
        projection.addProperty(new AbstractProperty("location", xlsTable.getLocation().getStart().toString()));
        IGridTable grid = xlsTable.getTable().getGridTable();
        projection.addProperty(new AbstractProperty("size", grid.getWidth() + "x" + grid.getHeight()));
    }

    public DiffTreeNode compare() {
        DiffTreeNodeImpl root = newDiffTreeNode();
        buildTree(root);
        diffTree(root);
        return root;
    }
}
