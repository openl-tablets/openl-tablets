package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.ui.TableSyntaxNodeUtils;

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

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object makeObject(TableSyntaxNode tsn) {

        return tsn;
    }
}
