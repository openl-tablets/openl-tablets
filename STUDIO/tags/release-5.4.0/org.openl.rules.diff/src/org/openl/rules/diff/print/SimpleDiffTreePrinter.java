package org.openl.rules.diff.print;

import java.io.OutputStream;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.hierarchy.ProjectionProperty;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.tree.DiffProperty;
import org.openl.rules.diff.tree.DiffStatus;
import org.openl.rules.diff.tree.DiffTreeNode;

public class SimpleDiffTreePrinter extends DiffTreePrinter {

    public SimpleDiffTreePrinter(DiffTreeNode tree, OutputStream out) {
        super(tree, out);
    }

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

            /*write(" ");
            write(e.isHierarhyEqual() ? "H" : "-");
            write(e.isChildrenEqual() ? "C" : "-");
            write(e.isSelfEqual() ? "S" : "-");*/

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
        DiffElement dif1 = des[0];
        DiffElement dif2 = des[1];
        DiffProperty[] diffProperties1 = dif1.getDiffProperties();
        DiffProperty[] diffProperties2 = dif2.getDiffProperties();
        for (int j = 0; j < diffProperties1.length; j++) {
            ProjectionProperty prop1 = diffProperties1[j].getProjectionProperty();
            ProjectionProperty prop2 = null;
            String prop1Name = prop1.getName();
            DiffProperty diffProp2 = getProperty(prop1Name, diffProperties2);
            if (diffProp2 != null) {
                prop2 = diffProp2.getProjectionProperty();
            }
            intend(i + 2);
            write(prop1.getName() + ":" + (prop1.getRawValue() != null ? prop1.getRawValue().toString() : ""));
            if (prop2 != null) {
                write("  ");
                writeStatus(diffProp2.getDiffStatus());
                write("  ");
                write(prop2.getName() + ":" + (prop2.getRawValue() != null ? prop2.getRawValue().toString() : ""));
            }
            write("\n");
        }
    }

    private DiffProperty getProperty(String name, DiffProperty[] props) {
        for (DiffProperty diffProperty : props) {
            ProjectionProperty property = diffProperty.getProjectionProperty();
            if (property.getName().equals(name)) {
                return diffProperty;
            }
        }
        return null;
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
