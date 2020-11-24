package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.util.StringUtils;

/**
 * Builder for category properties table.
 *
 * @author DLiauchuk
 *
 */
public class CategoryPropertiesTableNodeBuilder extends BaseTableTreeNodeBuilder {

    private static final String FOLDER_NAME = "Category Properties";

    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {
        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) nodeObject;
        return TableSyntaxNodeUtils.getTableDisplayValue(tableSyntaxNode, i, WebStudioFormats.getInstance());
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
    public boolean isBuilderApplicableForObject(TableSyntaxNode tableSyntaxNode) {
        return XlsNodeTypes.XLS_PROPERTIES.toString()
            .equals(tableSyntaxNode.getType()) && isCategoryPropertyTable(tableSyntaxNode);
    }

    @Override
    public ProjectTreeNode makeNode(TableSyntaxNode tableSyntaxNode, int i) {
        return makeFolderNode(FOLDER_NAME);
    }

    private static boolean isCategoryPropertyTable(TableSyntaxNode tableSyntaxNode) {
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

    private static ProjectTreeNode makeFolderNode(String folderName) {
        return new ProjectTreeNode(new String[] { folderName, folderName, folderName },
            IProjectTypes.PT_FOLDER,
            null,
                null);
    }
}
