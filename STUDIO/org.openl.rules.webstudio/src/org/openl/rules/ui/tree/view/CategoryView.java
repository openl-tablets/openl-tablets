package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.*;

public class CategoryView implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = { new ModulePropertiesTableNodeBuilder(),
            new CategoryTreeNodeBuilder(),
            new CategoryPropertiesTableNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder() };

    @Override
    public String getName() {
        return "category";
    }

    @Override
    public String getDisplayName() {
        return "Category";
    }

    @Override
    public String getDescription() {
        return "Provides categorized view";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}