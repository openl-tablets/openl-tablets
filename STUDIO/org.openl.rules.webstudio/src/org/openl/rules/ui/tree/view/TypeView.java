package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.*;

public class TypeView extends TypeProfile implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = { new TableTreeNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder() };

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}