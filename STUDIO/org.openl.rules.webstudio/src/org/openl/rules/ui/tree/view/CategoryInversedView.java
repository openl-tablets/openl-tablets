package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.*;

public class CategoryInversedView extends CategoryInversedProfile implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = { new ModulePropertiesTableNodeBuilder(),
            new CategoryNTreeNodeBuilder(1),
            new CategoryNTreeNodeBuilder(0),
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
