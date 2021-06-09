package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;

/**
 * Base class for building tree using tables meta information.
 *
 */
public abstract class BaseTableTreeNodeBuilder implements TreeNodeBuilder<TableSyntaxNode> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ProjectTreeNode makeNode(TableSyntaxNode tableSyntaxNode, int i) {

        Object nodeObject = makeObject(tableSyntaxNode);
        String[] displayNames = getDisplayValue(nodeObject, 0);

        ProjectTreeNode projectTreeNode;

        String type = getType(nodeObject);
        if (type.equals(IProjectTypes.PT_FOLDER)) {
            projectTreeNode = new ProjectTreeNode(displayNames, type, null);
        } else {
            projectTreeNode = new ProjectTreeNode(displayNames, type, tableSyntaxNode);
        }

        return projectTreeNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Comparable<?> makeKey(TableSyntaxNode tableSyntaxNode) {
        return makeKey(tableSyntaxNode, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Comparable<?> makeKey(TableSyntaxNode tableSyntaxNode, int i) {

        Object nodeObject = makeObject(tableSyntaxNode);

        return new NodeKey(getWeight(nodeObject), getDisplayValue(nodeObject, i));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnique(TableSyntaxNode tableSyntaxNode) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBuilderApplicableForObject(TableSyntaxNode tableSyntaxNode) {
        return true;
    }

    /**
     * Makes value object for node using table syntax node.
     *
     * @param tableSyntaxNode table syntax node
     * @return node object
     */
    protected Object makeObject(TableSyntaxNode tableSyntaxNode) {
        return tableSyntaxNode;
    }

    /**
     * Gets display value (triple of possible names) of node object.
     *
     * @param nodeObject node object
     * @param i display name mode
     * @return display value
     */
    public abstract String[] getDisplayValue(Object nodeObject, int i);

    /**
     * Gets type of node.
     *
     * @param nodeObject node object
     * @return string that represent node type
     */
    public abstract String getType(Object nodeObject);

    /**
     * Gets url of node.
     *
     * @param nodeObject node object
     * @return string that represent node url
     */
    public abstract String getUrl(Object nodeObject);

    /**
     * Gets weight of node.
     *
     * @param nodeObject node object
     * @return string that represent node weight
     */
    public int getWeight(Object nodeObject) {
        return 0;
    }
}
