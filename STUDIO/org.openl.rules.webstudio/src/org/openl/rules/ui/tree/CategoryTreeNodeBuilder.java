package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;

/**
 * Builds tree node for table category.
 */
public class CategoryTreeNodeBuilder extends BaseTableTreeNodeBuilder {

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {

        var tableSyntaxNode = (TableSyntaxNode) nodeObject;
        var category = getCategory(tableSyntaxNode);

        return new String[]{category, category, category};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Object nodeObject) {
        return IProjectTypes.PT_FOLDER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl(Object nodeObject) {

        var tableSyntaxNode = (TableSyntaxNode) nodeObject;

        return tableSyntaxNode.getUri();
    }

    @Override
    public boolean isBuilderApplicableForObject(TableSyntaxNode tableSyntaxNode) {
        return !(XlsNodeTypes.XLS_PROPERTIES.toString()
                .equals(
                        tableSyntaxNode.getType()) && ModulePropertiesTableNodeBuilder.isModulePropertyTable(tableSyntaxNode));
    }

    /**
     * Gets name of category.
     *
     * @param tableSyntaxNode table syntax node
     * @return name of category
     */
    protected String getCategory(TableSyntaxNode tableSyntaxNode) {

        String category = null;

        var tableProperties = tableSyntaxNode.getTableProperties();

        if (tableProperties != null && tableProperties.getCategory() != null) {
            category = tableProperties.getCategory();
        }

        if (category == null) {

            var sheet = tableSyntaxNode.getXlsSheetSourceCodeModule();
            category = sheet.getSheetName();
        }

        return category;
    }
}
