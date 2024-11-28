package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;
import org.openl.util.StringUtils;

/**
 * Builds tree node for table category.
 */
public class CategoryNTreeNodeBuilder extends CategoryTreeNodeBuilder {

    private final int categoryLevel;

    public CategoryNTreeNodeBuilder(int categoryLevel) {
        this.categoryLevel = categoryLevel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCategory(TableSyntaxNode tableSyntaxNode) {

        String result;
        String category = super.getCategory(tableSyntaxNode);

        String[] categories = StringUtils.split(category, '-');

        if (categories.length == 0) {
            result = category;
        } else {
            result = categoryLevel < categories.length ? categories[categoryLevel] : categories[categories.length - 1];
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Object sorterObject) {
        return IProjectTypes.PT_FOLDER;
    }

    @Override
    public boolean isBuilderApplicableForObject(TableSyntaxNode tableSyntaxNode) {
        if (!super.isBuilderApplicableForObject(tableSyntaxNode)) {
            return false;
        }

        String category = super.getCategory(tableSyntaxNode);
        String[] categories = StringUtils.split(category, '-');

        return categoryLevel < categories.length;
    }
}
