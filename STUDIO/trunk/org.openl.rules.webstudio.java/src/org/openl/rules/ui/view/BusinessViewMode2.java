package org.openl.rules.ui.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryNTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

public class BusinessViewMode2 extends BaseBusinessViewMode {

    private static final BaseTableTreeNodeBuilder[][] sorters = { { new CategoryNTreeNodeBuilder(0, "-"),
            new CategoryNTreeNodeBuilder(1, "-"), new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder() } };

    public BusinessViewMode2() {
        setName(WebStudioViewMode.BUSINESS_MODE_TYPE + ".2");
        displayName = "Business View 2. Provides more detailed categorized view";
    }

    @Override
    public TreeNodeBuilder[][] getBuilders() {
        return sorters;
    }
}
