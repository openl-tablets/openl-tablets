package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.*;

public class CategoryDetailedView implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = { new ModulePropertiesTableNodeBuilder(),
            new CategoryNTreeNodeBuilder(0, "-"),
            new CategoryNTreeNodeBuilder(1, "-"),
            new CategoryPropertiesTableNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder() };

    @Override
    public String getName() {
        return "categoryDetailed";
    }

    @Override
    public String getDisplayName() {
        return "Category Detailed";
    }

    @Override
    public String getDescription() {
        return "Provides more detailed categorized view";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}
