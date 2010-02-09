package org.openl.rules.ui.tree;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.ui.TableSyntaxNodeUtils;

/**
 * Builder for category properties table. 
 * 
 * @author DLiauchuk
 *
 */
public class CategoryPropertiesTableNodeBuilder extends BaseTableTreeNodeBuilder {
    
    private static final String FOLDER_NAME = "Category Properties";
    private static final String VALUE_PROPERTY_SCOPE = "category";
    private static final String NAME_PROPERTY_SCOPE = "scope";
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
        return null;
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
    public ProjectTreeNode makeNode(TableSyntaxNode tableSyntaxNode, int i) {
        if (ITableNodeTypes.XLS_PROPERTIES.equals(tableSyntaxNode.getType()) && isCategoryPropertyTable(tableSyntaxNode)) {
            String folderName = FOLDER_NAME;
            return makeFolderNode(folderName);
        }
        return null;
    }
    
    private boolean isCategoryPropertyTable(TableSyntaxNode tableSyntaxNode) {
        boolean result = false;
        ITableProperties tableProperties = tableSyntaxNode.getTableProperties();
        if (tableProperties != null) {
            String propValue = tableProperties.getPropertyValueAsString(NAME_PROPERTY_SCOPE);
            if (StringUtils.isNotEmpty(propValue)) {
                // if such property exists, check if value is equal to VALUE_PROPERTY_SCOPE.
                if (VALUE_PROPERTY_SCOPE.equals(propValue)) {
                    result = true;
                }
            } else {
                // also when there is no property with name NAME_PROPERTY_SCOPE, we consider that is property table for
                // the whole sheet.
                result = true;
            }
        }
        return result;
    }
    
    private ProjectTreeNode makeFolderNode(String folderName) {
        return new ProjectTreeNode(new String[] { folderName, folderName, folderName }, IProjectTypes.PT_FOLDER, null, null, 0, null);
    }
}
