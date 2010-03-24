package org.openl.rules.ui.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryNTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryPropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.ModulePropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

public class BusinessViewMode2 extends BaseBusinessViewMode {

    private static final BaseTableTreeNodeBuilder[] sorters = {
        new ModulePropertiesTableNodeBuilder(), 
        new CategoryNTreeNodeBuilder(0, "-"),
        new CategoryNTreeNodeBuilder(1, "-"),
        new CategoryPropertiesTableNodeBuilder(), 
        new OpenMethodInstancesGroupTreeNodeBuilder(),
        new TableInstanceTreeNodeBuilder(),
        new TableVersionTreeNodeBuilder()
    };

    public BusinessViewMode2() {
        setName(getType() + ".2");
        displayName = "By Category Detailed";
        description = "Business View 2. Provides more detailed categorized view";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}
