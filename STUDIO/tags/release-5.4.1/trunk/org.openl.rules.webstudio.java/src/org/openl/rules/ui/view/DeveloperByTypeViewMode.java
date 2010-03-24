package org.openl.rules.ui.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

public class DeveloperByTypeViewMode extends BaseDeveloperViewMode {

    private static final BaseTableTreeNodeBuilder[] sorters = {
        new TableTreeNodeBuilder(),
        new OpenMethodInstancesGroupTreeNodeBuilder(),
        new TableInstanceTreeNodeBuilder(),
        new TableVersionTreeNodeBuilder()
    };

    public DeveloperByTypeViewMode() {
        setName(getType() + ".byType");
        displayName = "By Type";
        description = "Developer Mode 1. Organize Project by component type";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}