package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

public class TypeView implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = { new TableTreeNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder() };

    @Override
    public String getName() {
        return "type";
    }

    @Override
    public String getDisplayName() {
        return "Type";
    }

    @Override
    public String getDescription() {
        return "Organize projects by component type";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}