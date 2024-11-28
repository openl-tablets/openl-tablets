package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryPropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.CategoryTreeNodeBuilder;
import org.openl.rules.ui.tree.ModulePropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

public class CategoryView extends CategoryProfile implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = {new ModulePropertiesTableNodeBuilder(),
            new CategoryTreeNodeBuilder(),
            new CategoryPropertiesTableNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder()};

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}
