package org.openl.rules.ui.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

public class BusinessViewMode1 extends BaseBusinessViewMode {

    private static final BaseTableTreeNodeBuilder[][] sorters = { { new CategoryTreeNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(), new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder() } };

    public BusinessViewMode1() {
        setName(WebStudioViewMode.BUSINESS_MODE_TYPE + ".1");
        displayName = "Business View 1. Provides categorized view";
    }

    @Override
    public TreeNodeBuilder[][] getBuilders() {
        return sorters;
    }
}