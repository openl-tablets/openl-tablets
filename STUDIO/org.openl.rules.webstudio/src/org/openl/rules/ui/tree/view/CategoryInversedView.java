package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.*;

public class CategoryInversedView implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = { new ModulePropertiesTableNodeBuilder(),
            new CategoryNTreeNodeBuilder(1, "-"),
            new CategoryNTreeNodeBuilder(0, "-"),
            new CategoryPropertiesTableNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder() };

    @Override
    public String getName() {
        return "categoryInversed";
    }

    @Override
    public String getDisplayName() {
        return "Category Inversed";
    }

    @Override
    public String getDescription() {
        return "Provides inversed categorized view";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}