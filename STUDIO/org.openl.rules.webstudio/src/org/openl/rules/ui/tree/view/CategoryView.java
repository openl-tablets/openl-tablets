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

    /**
     * A fresh set of builders for every tree that is built.
     *
     * <p>A builder carries the state of the tree being built — the dictionary of overloaded methods the groups
     * are named from — so a set shared between two builds running at once would leave each of them reading the
     * other's module.
     */
    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return new BaseTableTreeNodeBuilder[]{new ModulePropertiesTableNodeBuilder(),
                new CategoryTreeNodeBuilder(),
                new CategoryPropertiesTableNodeBuilder(),
                new OpenMethodInstancesGroupTreeNodeBuilder(),
                new TableInstanceTreeNodeBuilder(),
                new TableVersionTreeNodeBuilder()};
    }

}
