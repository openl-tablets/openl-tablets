package org.openl.rules.ui.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryPropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.CategoryTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.ModulePropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

public class BusinessViewMode1 implements WebStudioViewMode {

    private static final BaseTableTreeNodeBuilder[] sorters = {
        new ModulePropertiesTableNodeBuilder(), 
        new CategoryTreeNodeBuilder(),
        new CategoryPropertiesTableNodeBuilder(),
        new OpenMethodInstancesGroupTreeNodeBuilder(),
        new TableInstanceTreeNodeBuilder(),
        new TableVersionTreeNodeBuilder()
    };

    @Override
    public String getName() {
    	return "byCategory";
    }
    
    @Override
    public String getDisplayName() {
    	return "By Category";
    }

    @Override
    public String getDescription() {
    	return "Provides categorized view";
    }

    @SuppressWarnings("unchecked")
	@Override
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}