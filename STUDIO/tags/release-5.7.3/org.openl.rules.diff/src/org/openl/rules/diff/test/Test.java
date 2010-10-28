package org.openl.rules.diff.test;

import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.tree.DiffStatus;
import org.openl.rules.diff.hierarchy.Projection;

import static java.lang.System.out;
import java.lang.reflect.Array;

public class Test {
    public static void main(String[] args) {
        Test t = new Test();
        t.test();
    }

    void test() {
        AbstractProjection p1 = new AbstractProjection("p1", "project");
        p1.addProperty(new AbstractProperty("prop.str", String.class, "some-string"));
        p1.addProperty(new AbstractProperty("prop.date", Long.class, System.currentTimeMillis()));
        p1.addProperty(new AbstractProperty("prop.bytes[]", Array.class, new byte[]{1, 2, 3}));
        p1.addChild(new AbstractProjection("F1", "folder"));
        p1.addChild(new AbstractProjection("F2", "folder"));
        p1.addChild(new AbstractProjection("F4", "folder"));

        AbstractProjection p2 = new AbstractProjection("p2", "project");
        p2.addProperty(new AbstractProperty("prop.str", String.class, "some-string"));
        p2.addProperty(new AbstractProperty("prop.date", Long.class, System.currentTimeMillis()));
        p2.addProperty(new AbstractProperty("prop.bytes[]", Array.class, new byte[]{1, 2, 3}));
        p2.addChild(new AbstractProjection("F2", "folder"));
        p2.addChild(new AbstractProjection("F3", "folder"));
        p2.addChild(new AbstractProjection("F4", "file"));

        DiffTreeBuilderImpl builder = new DiffTreeBuilderImpl();
        builder.setProjectionDiffer(new ProjectionDifferImpl());

        DiffTreeNode tree = builder.compare(p1, p2);
        printTree(tree);
    }

    void printTree(DiffTreeNode root) {
        printR(root, 0);
    }

    void printR(DiffTreeNode node, int i) {
        printNode(node, i);

        for (DiffTreeNode c : node.getChildren()) {
            printR(c, i+1);
        }
    }
    void printNode(DiffTreeNode node, int i) {
        intend(i);
        for (DiffElement e : node.getElements()) {
            DiffStatus s = e.getDiffStatus();
            switch (s) {
                case ADDED:
                    out.print("+");
                    break;
                case REMOVED:
                    out.print("-");
                    break;
                case DIFFERS:
                    out.print("~");
                    break;
                case EQUALS:
                    out.print("=");
                    break;
                default:
                    out.print(" ");
                    break;
            }

            out.print(" ");
            out.print(e.isHierarhyEqual() ? "H" : "-");
            out.print(e.isChildrenEqual() ? "C" : "-");
            out.print(e.isSelfEqual() ? "S" : "-");
            out.print(" ");

            Projection p = e.getProjection();
            out.print((p == null) ? "---" : p.getType() + ":" + p.getName());

            out.print("\t");
        }
        out.println();
    }

    private void intend(int in) {
        for (int i = 0; i < in; i++) {
            out.print("  ");
        }
    }
}
