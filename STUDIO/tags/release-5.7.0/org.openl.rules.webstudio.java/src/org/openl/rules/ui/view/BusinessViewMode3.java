package org.openl.rules.ui.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryNTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryPropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.ModulePropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

public class BusinessViewMode3 extends BaseBusinessViewMode {

    private static final BaseTableTreeNodeBuilder[] sorters = {
        new ModulePropertiesTableNodeBuilder(),
        new CategoryNTreeNodeBuilder(1, "-"),
        new CategoryNTreeNodeBuilder(0, "-"),
        new CategoryPropertiesTableNodeBuilder(),
        new OpenMethodInstancesGroupTreeNodeBuilder(),
        new TableInstanceTreeNodeBuilder(),
        new TableVersionTreeNodeBuilder()
    };

    public BusinessViewMode3() {
        setName(getType() + ".3");
        displayName = "By Category Inversed";
        description = "Business View 3. Provides inversed categorized view";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}