package org.openl.rules.ui.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryNTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

public class BusinessViewMode3 extends BaseBusinessViewMode {

    private static final BaseTableTreeNodeBuilder[][] sorters = { { new CategoryNTreeNodeBuilder(1, "-"),
            new CategoryNTreeNodeBuilder(0, "-"), new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(), new TableVersionTreeNodeBuilder() } };

    public BusinessViewMode3() {
        setName(WebStudioViewMode.BUSINESS_MODE_TYPE + ".3");
        displayName = "Business View 3. Provides inversed categorized view";
    }

    @Override
    public TreeNodeBuilder[][] getBuilders() {
        return sorters;
    }
}