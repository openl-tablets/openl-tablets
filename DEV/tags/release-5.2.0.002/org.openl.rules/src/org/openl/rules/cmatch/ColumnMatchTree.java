package org.openl.rules.cmatch;

import java.util.List;
import java.util.ArrayList;

import org.openl.meta.StringValue;

@Deprecated
public class ColumnMatchTree {

    private StringValue[] returnValues;
    List<ColumnMatchTreeNode> nodes;

    /**
     * @return the returnValues
     */
    public StringValue[] getReturnValues() {
        return returnValues;
    }
    /**
     * @param returnValues the returnValues to set
     */
    public void setReturnValues(StringValue[] returnValues) {
        this.returnValues = returnValues;
    }
    /**
     * @return the treeNodes
     */
    public List<ColumnMatchTreeNode> getNodes() {
        return nodes;
    }
    /**
     * @param treeNodes the treeNodes to set
     */
    public void setNodes(List<ColumnMatchTreeNode> nodes) {
        this.nodes = nodes;
    }

    public void addNode(ColumnMatchTreeNode node) {
        if (nodes == null) {
            nodes = new ArrayList<ColumnMatchTreeNode>();
        }
        nodes.add(node);
    }

    public void addNode(ColumnMatchTreeNode parent, ColumnMatchTreeNode node) {
        if (parent != null) {
            List<ColumnMatchTreeNode> children = parent.getChildren();
            if (children == null) {
                children = new ArrayList<ColumnMatchTreeNode>();
                parent.setChildren(children);
            }
            children.add(node);
        } else {
            addNode(node);
        }
    }

}
