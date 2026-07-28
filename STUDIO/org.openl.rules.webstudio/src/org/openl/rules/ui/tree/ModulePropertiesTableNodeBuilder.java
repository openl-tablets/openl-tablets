package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.ui.IProjectTypes;
import org.openl.util.StringUtils;

/**
 * Builder for module properties table.
 *
 * @author DLiauchuk
 */
public class ModulePropertiesTableNodeBuilder extends BaseTableTreeNodeBuilder {

    private static final String[] DISPLAY_NAMES = {"Module Properties", "Module Properties", "Module Properties"};

    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {
        return DISPLAY_NAMES;
    }

    @Override
    public String getType(Object nodeObject) {
        return IProjectTypes.PT_TABLE_GROUP;
    }

    @Override
    public String getUrl(Object nodeObject) {
        var tableSyntaxNode = (TableSyntaxNode) nodeObject;
        return tableSyntaxNode.getUri();
    }

    @Override
    public boolean isBuilderApplicableForObject(TableSyntaxNode tableSyntaxNode) {
        return XlsNodeTypes.XLS_PROPERTIES.toString()
                .equals(tableSyntaxNode.getType()) && isModulePropertyTable(tableSyntaxNode);
    }

    @Override
    public ProjectTreeNode makeNode(TableSyntaxNode tableSyntaxNode, int i) {
        return new ProjectTreeNode(DISPLAY_NAMES, IProjectTypes.PT_FOLDER, null);
    }

    public static boolean isModulePropertyTable(TableSyntaxNode tableSyntaxNode) {
        var result = false;
        var tableProperties = tableSyntaxNode.getTableProperties();
        if (tableProperties != null) {
            var propValue = tableProperties.getScope();
            if (StringUtils.isNotEmpty(propValue) && InheritanceLevel.MODULE.getDisplayName().equals(propValue)) {
                result = true;
            }
        }
        return result;
    }
}
