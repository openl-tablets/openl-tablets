package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.*;

public class CategoryView extends CategoryProfile implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = { new ModulePropertiesTableNodeBuilder(),
            new CategoryTreeNodeBuilder(),
            new CategoryPropertiesTableNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder() };

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}
