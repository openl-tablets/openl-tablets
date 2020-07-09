package org.openl.rules.data;

import java.util.Arrays;
import java.util.Objects;

import org.openl.syntax.impl.IdentifierNode;

public class IdentifierNodesBucket {

    private IdentifierNode[] node;
    private int length;
    private String[] ids;

    public IdentifierNodesBucket(IdentifierNode[] node) {
        this.node = node;
        if (node != null) {
            this.length = node.length;
            if (DataTableBindHelper.isPrecisionNode(node[this.length - 1])) {
                this.length--;
            }
            ids = new String[length];
            for (int i = 0; i < length; i++) {
                ids[i] = node[i].getIdentifier();
            }
        }
    }

    public IdentifierNode[] getNode() {
        return node;
    }

    public int getLength() {
        return length;
    }

    public String[] getIds() {
        return ids;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IdentifierNodesBucket that = (IdentifierNodesBucket) o;
        if (this.length != that.getLength()) {
            return false;
        }
        if (this.node == null || that.getNode() == null) {
            return false;
        }
        return Arrays.equals(ids, that.getIds());
    }

    @Override
    public int hashCode() {
        return Objects.hash(length, Arrays.hashCode(ids));
    }
}
