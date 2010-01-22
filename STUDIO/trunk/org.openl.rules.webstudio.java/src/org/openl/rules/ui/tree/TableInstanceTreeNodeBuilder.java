package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeKey;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.ui.TableSyntaxNodeUtils;
import org.openl.types.IOpenMethod;

/**
 * Builds tree node for table instance.
 * 
 */
public class TableInstanceTreeNodeBuilder extends OpenMethodsGroupTreeNodeBuilder {

    private static final String TABLE_INSTANCE_NAME = "Table Instance";

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDisplayValue(Object sorterObject, int i) {

        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;

        return TableSyntaxNodeUtils.getTableDisplayValue(tsn, i, getOpenMethodGroupsDictionary());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return TABLE_INSTANCE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getProblems(Object sorterObject) {

        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;

        return tsn.getErrors() != null ? tsn.getErrors() : tsn.getValidationResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Object sorterObject) {

        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;

        return IProjectTypes.PT_TABLE + "." + tsn.getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl(Object sorterObject) {

        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;

        return tsn.getUri();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWeight(Object sorterObject) {

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnique() {

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object makeObject(TableSyntaxNode tsn) {

        return tsn;
    }

    @Override
    public Comparable<?> makeKey(TableSyntaxNode tableSyntaxNode) {
        if (tableSyntaxNode.getMember() instanceof IOpenMethod) {

            TableSyntaxNodeKey key = new TableSyntaxNodeKey(tableSyntaxNode);

            int hash = key.hashCode();
            String hashString = String.valueOf(hash);

            Object nodeObject = makeObject(tableSyntaxNode);

            return new NodeKey(getWeight(nodeObject), new String[] { hashString, hashString, hashString });
        }
        return null;
    }
    
    @Override
    public ITreeNode<Object> makeNode(TableSyntaxNode tableSyntaxNode, int i) {
        Object nodeObject = makeObject(tableSyntaxNode);
        String[] displayNames = getDisplayValue(nodeObject, 0);
        ProjectTreeNode projectTreeNode = new VersionedTreeNode(displayNames, tableSyntaxNode);
        projectTreeNode.setObject(nodeObject);
        return projectTreeNode;
    }
}
