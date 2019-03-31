package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.util.StringUtils;

/**
 * Builder for category properties table.
 * 
 * @author DLiauchuk
 *
 */
public class CategoryPropertiesTableNodeBuilder extends BaseTableTreeNodeBuilder {

    private static final String FOLDER_NAME = "Category Properties";
    private static final String CATEGORY_PROPERTIES_TABLE = "Category Properties Table";

    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {
        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) nodeObject;
        return TableSyntaxNodeUtils.getTableDisplayValue(tableSyntaxNode, i);
    }

    @Override
    public String getName() {
        return CATEGORY_PROPERTIES_TABLE;
    }

    @Override
    public Object getProblems(Object nodeObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        return tsn.getErrors() != null ? tsn.getErrors() : tsn.getValidationResult();
    }

    @Override
    public String getType(Object nodeObject) {
        return IProjectTypes.PT_TABLE_GROUP;
    }

    @Override
    public String getUrl(Object nodeObject) {
        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) nodeObject;
        return tableSyntaxNode.getUri();
    }

    @Override
    public int getWeight(Object nodeObject) {
        return 0;
    }

    @Override
    protected Object makeObject(TableSyntaxNode tableSyntaxNode) {
        return tableSyntaxNode;
    }

    @Override
    public boolean isBuilderApplicableForObject(TableSyntaxNode tableSyntaxNode) {
        return XlsNodeTypes.XLS_PROPERTIES.toString()
            .equals(tableSyntaxNode.getType()) && isCategoryPropertyTable(tableSyntaxNode);
    }

    @Override
    public ProjectTreeNode makeNode(TableSyntaxNode tableSyntaxNode, int i) {
        return makeFolderNode(FOLDER_NAME);
    }

    private boolean isCategoryPropertyTable(TableSyntaxNode tableSyntaxNode) {
        boolean result = false;
        ITableProperties tableProperties = tableSyntaxNode.getTableProperties();
        if (tableProperties != null) {
            String propValue = tableProperties.getScope();
            if (StringUtils.isNotEmpty(propValue) && InheritanceLevel.CATEGORY.getDisplayName().equals(propValue)) {
                result = true;
            }
        }
        return result;
    }

    private ProjectTreeNode makeFolderNode(String folderName) {
        return new ProjectTreeNode(new String[] { folderName, folderName, folderName },
            IProjectTypes.PT_FOLDER,
            null,
            null,
            0,
            null);
    }
}
