package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.*;

public class CategoryDetailedView extends CategoryDetailedProfile implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = { new ModulePropertiesTableNodeBuilder(),
            new CategoryNTreeNodeBuilder(0),
            new CategoryNTreeNodeBuilder(1),
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
