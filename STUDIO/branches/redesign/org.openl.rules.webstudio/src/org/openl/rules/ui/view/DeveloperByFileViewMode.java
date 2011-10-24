package org.openl.rules.ui.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.ui.tree.WorkbookTreeNodeBuilder;
import org.openl.rules.ui.tree.WorksheetTreeNodeBuilder;

public class DeveloperByFileViewMode implements WebStudioViewMode {

    private static final BaseTableTreeNodeBuilder[] sorters = {
        new WorkbookTreeNodeBuilder(),
        new WorksheetTreeNodeBuilder(),
        new OpenMethodInstancesGroupTreeNodeBuilder(),
        new TableInstanceTreeNodeBuilder(),
        new TableVersionTreeNodeBuilder()
    };

    @Override
    public String getName() {
    	return "byFile";
    }
    
    @Override
    public String getDisplayName() {
    	return "By File";
    }

    @Override
    public String getDescription() {
    	return "Organize projects by physical location";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}