package org.openl.rules.cmatch;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.cmatch.algorithm.Argument;
import org.openl.rules.cmatch.matcher.IMatcher;

public class MatchNode {
    private final List<MatchNode> children;
    @Getter
    private final int rowIndex;

    @Getter
    private MatchNode parent;

    @Getter
    @Setter
    private IMatcher matcher;

    @Getter
    @Setter
    private Argument argument;

    @Getter
    @Setter
    private int weight;

    /**
     * Actual values in a row
     */
    @Getter
    @Setter
    private Object[] checkValues;

    public MatchNode(int rowIndex) {
        children = new LinkedList<>();
        this.rowIndex = rowIndex;
    }

    public void add(MatchNode child) {
        children.add(child);
        child.parent = this;
    }

    public void clearChildren() {
        children.clear();
    }

    public List<MatchNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}
