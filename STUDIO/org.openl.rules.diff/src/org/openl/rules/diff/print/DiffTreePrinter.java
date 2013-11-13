package org.openl.rules.diff.print;

import java.io.IOException;
import java.io.OutputStream;

import org.openl.rules.diff.tree.DiffTreeNode;

public abstract class DiffTreePrinter {

    protected DiffTreeNode tree;
    protected OutputStream out;

    public DiffTreePrinter(DiffTreeNode tree, OutputStream out) {
        this.tree = tree;
        this.out = out;
    }

    protected void write(String str) {
        try {
            out.write(str.getBytes());
        } catch (IOException e) {
            // TODO: handle exception
        }
    }

    abstract void print();
}