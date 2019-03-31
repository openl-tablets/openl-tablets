package org.openl.rules.diff.print;

import java.io.OutputStream;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.tree.DiffStatus;
import org.openl.rules.diff.tree.DiffTreeNode;

@Deprecated
public class SimpleDiffTreePrinter extends DiffTreePrinter {

    public SimpleDiffTreePrinter(DiffTreeNode tree, OutputStream out) {
        super(tree, out);
    }

    @Override
    public void print() {
        printRoot(tree, 0);
    }

    private void printRoot(DiffTreeNode node, int i) {
        printNode(node, i);

        for (DiffTreeNode c : node.getChildren()) {
            printRoot(c, i + 1);
        }
    }

    private void printNode(DiffTreeNode node, int i) {
        intend(i);
        for (DiffElement e : node.getElements()) {
            DiffStatus s = e.getDiffStatus();
            writeStatus(s);

            /*
             * write(" "); write(e.isHierarhyEqual() ? "H" : "-"); write(e.isChildrenEqual() ? "C" : "-");
             * write(e.isSelfEqual() ? "S" : "-");
             */

            write(" ");
            Projection p = e.getProjection();
            write((p == null) ? "---" : p.getType() + ":" + p.getName());
            write("\t");
        }
        write("\n");

        // TEMP!!! FOR 2 COMPARING ELEMENTS ONLY!!! TO FIX!!!
        DiffElement[] des = node.getElements();
        if (des.length > 2) {
            System.err.println("2 only in this printer");
        }
    }

    private void writeStatus(DiffStatus status) {
        if (status == null) {
            return;
        }
        switch (status) {
            case ADDED:
                write("+");
                break;
            case REMOVED:
                write("-");
                break;
            case DIFFERS:
                write("~");
                break;
            case EQUALS:
                write("=");
                break;
            default:
                write(" ");
                break;
        }
    }

    private void intend(int in) {
        for (int i = 0; i < in; i++) {
            write("  ");
        }
    }

}
