package org.openl.rules.ui.tree;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.InheritanceLevel;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.ui.TableSyntaxNodeUtils;

/**
 * Builder for module properties table. 
 * 
 * @author DLiauchuk
 *
 */
public class ModulePropertiesTableNodeBuilder extends BaseTableTreeNodeBuilder {
    
    private static final String FOLDER_NAME = "Module Properties";
    private static final String MODULE_PROPERTIES_TABLE = "Module Properties Table";
    
    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {
        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) nodeObject;        
        return TableSyntaxNodeUtils.getTableDisplayValue(tableSyntaxNode, i);
    }

    @Override
    public String getName() {
        return MODULE_PROPERTIES_TABLE;
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
        if (ITableNodeTypes.XLS_PROPERTIES.equals(tableSyntaxNode.getType()) && isModulePropertyTable(tableSyntaxNode)) {
            String folderName = FOLDER_NAME;
            return makeFolderNode(folderName);
        }
        return null;
    }
    
    private boolean isModulePropertyTable(TableSyntaxNode tableSyntaxNode) {
        boolean result = false;
        ITableProperties tableProperties = tableSyntaxNode.getTableProperties();
        if (tableProperties != null) {
            String propValue = tableProperties.getScope();
            if (StringUtils.isNotEmpty(propValue) && InheritanceLevel.MODULE.getDisplayName().equals(propValue)) {
                result = true;
            }
        }
        return result;
    }
    
    private ProjectTreeNode makeFolderNode(String folderName) {
        return new ProjectTreeNode(new String[] { folderName, folderName, folderName }, IProjectTypes.PT_FOLDER, null, null, 0, null);
    }

}
