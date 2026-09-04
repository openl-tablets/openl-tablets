package org.openl.rules.ui.tree;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class ProjectTreeNode {

    private final String[] displayName;
    private final TableSyntaxNode tableSyntaxNode;

    public ProjectTreeNode(String[] displayName, String type, TableSyntaxNode tsn) {
        this.type = type;
        this.displayName = displayName;
        this.tableSyntaxNode = tsn;
    }

    public String getDisplayName(int mode) {
        return displayName[mode];
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }

    /**
     * Children of current node.
     */
    private Map<Object, ProjectTreeNode> elements = new TreeMap<>();

    /**
     * String that represent the node type.
     */
    private final String type;

    public void addChild(Object key, ProjectTreeNode child) {
        elements.put(key, child);
    }

    public ProjectTreeNode getChild(Object key) {
        return elements.get(key);
    }

    public Collection<ProjectTreeNode> getChildren() {
        return getElements().values();
    }

    /**
     * Gets the map of elements.
     *
     * @return map of elements
     */
    public Map<Object, ProjectTreeNode> getElements() {
        return elements;
    }

    public void setElements(Map<Object, ProjectTreeNode> elements) {
        this.elements = elements;
    }

    public String getType() {
        return type;
    }
}
