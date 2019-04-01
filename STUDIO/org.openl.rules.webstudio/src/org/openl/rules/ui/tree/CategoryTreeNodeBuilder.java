package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.ui.IProjectTypes;

/**
 * Builds tree node for table category.
 */
public class CategoryTreeNodeBuilder extends BaseTableTreeNodeBuilder {

    private static final String CATEGORY_BUILDER_NAME = "Category Builder";

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {

        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) nodeObject;
        String category = getCategory(tableSyntaxNode);

        return new String[] { category, category, category };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return CATEGORY_BUILDER_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Object nodeObject) {
        // return CATEGORY_TYPE;
        return IProjectTypes.PT_FOLDER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl(Object nodeObject) {

        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) nodeObject;

        return tableSyntaxNode.getUri();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWeight(Object nodeObject) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object makeObject(TableSyntaxNode tableSyntaxNode) {
        return tableSyntaxNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getProblems(Object nodeObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        return tsn.getErrors() != null ? tsn.getErrors() : tsn.getValidationResult();
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

        ITableProperties tableProperties = tableSyntaxNode.getTableProperties();

        if (tableProperties != null && tableProperties.getCategory() != null) {
            category = tableProperties.getCategory();
        }

        if (category == null) {

            XlsSheetSourceCodeModule sheet = tableSyntaxNode.getXlsSheetSourceCodeModule();
            category = sheet.getSheetName();
        }

        return category;
    }
}
