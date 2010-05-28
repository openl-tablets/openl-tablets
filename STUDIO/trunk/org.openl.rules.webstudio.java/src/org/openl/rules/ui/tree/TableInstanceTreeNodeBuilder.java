package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.ITableNodeTypes;
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
    public Object getProblems(Object nodeObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
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
    public boolean isUnique(TableSyntaxNode tsn) {
        if (ITableNodeTypes.XLS_PROPERTIES.equals(tsn.getType())
                || ITableNodeTypes.XLS_ENVIRONMENT.equals(tsn.getType())) {
            return true;
        }
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

            String keyString = key.toString();

            Object nodeObject = makeObject(tableSyntaxNode);

            String[] displayNames = getDisplayValue(tableSyntaxNode, 0);
            for(int i = 0; i < displayNames.length; i++){
                displayNames[i] += keyString;
            }
            return new NodeKey(getWeight(nodeObject), displayNames);
        } else {
            return super.makeKey(tableSyntaxNode);
        }
    }

    @Override
    public ITreeNode<Object> makeNode(TableSyntaxNode tableSyntaxNode, int i) {
        Object nodeObject = makeObject(tableSyntaxNode);
        String[] displayNames = getDisplayValue(nodeObject, 0);
        // it seems we need to process only those tables that have properties that are using for version sorting.
        // in other case return original tableSyntaxNode.
        // ???
        // author: DLiauchuk
        ProjectTreeNode projectTreeNode = new VersionedTreeNode(displayNames, tableSyntaxNode);
        projectTreeNode.setObject(nodeObject);
        return projectTreeNode;
    }
}
