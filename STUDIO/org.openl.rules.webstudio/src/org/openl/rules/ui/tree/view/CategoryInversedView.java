package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryNTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryPropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.ModulePropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

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