package org.openl.rules.ui.tree;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;

public class ProjectTreeNode {

    private String uri;
    private String[] displayName;
    private TableSyntaxNode tableSyntaxNode;

    public ProjectTreeNode(String[] displayName, String type, String uri, TableSyntaxNode tsn) {
        this.type = type;
        this.uri = uri;
        this.displayName = displayName;
        this.tableSyntaxNode = tsn;
    }

    public String getDisplayName(int mode) {
        return displayName[mode];
    }

    public String getUri() {
        return uri;
    }

    public int getNumErrors() {
        int result = 0;

        TableSyntaxNode table = getTableSyntaxNode();
        Collection<ProjectTreeNode> children = getChildren();
        if (table != null) {
            SyntaxNodeException[] errors = table.getErrors();
            if (errors != null) {
                result += errors.length;
            }
            if (children.isEmpty()) {
                return result;
            }
        }

        for (ProjectTreeNode treeNode : children) {
            result += treeNode.getNumErrors();
        }
        return result;
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
    private String type;

    /**
     * Contained object.
     */
    private Object object;

    /**
     * {@inheritDoc}
     */
    public void addChild(Object key, ProjectTreeNode child) {
        elements.put(key, child);
    }

    /**
     * {@inheritDoc}
     */
    public ProjectTreeNode getChild(Object key) {
        return elements.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Collection<ProjectTreeNode> getChildren() {
        return elements.values();
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

    /**
     * {@inheritDoc}
     */
    public Object getObject() {
        return object;
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return type;
    }

    /**
     * Checks that node is leaf.
     *
     * @return <code>true</code> if node is leaf; <code>false</code> - otherwise
     */
    public boolean isLeaf() {

        return elements == null || elements.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public void setObject(Object object) {
        this.object = object;
    }

}
