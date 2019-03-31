package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;
import org.openl.util.StringTool;

/**
 * Builds tree node for table category.
 */
public class CategoryNTreeNodeBuilder extends CategoryTreeNodeBuilder {

    private int categoryLevel;
    private String separators;

    public CategoryNTreeNodeBuilder(int categoryLevel, String separators) {
        this.categoryLevel = categoryLevel;
        this.separators = separators;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCategory(TableSyntaxNode tableSyntaxNode) {

        String result;
        String category = super.getCategory(tableSyntaxNode);

        String[] categories = StringTool.tokenize(category, separators);

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
    public String getName() {
        return "category." + categoryLevel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Object sorterObject) {
        // return "category." + categoryLevel;
        return IProjectTypes.PT_FOLDER;
    }

    @Override
    public boolean isBuilderApplicableForObject(TableSyntaxNode tableSyntaxNode) {
        if (!super.isBuilderApplicableForObject(tableSyntaxNode)) {
            return false;
        }

        String category = super.getCategory(tableSyntaxNode);
        String[] categories = StringTool.tokenize(category, separators);

        return categoryLevel < categories.length;
    }
}
