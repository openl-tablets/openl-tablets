package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.ui.tree.WorkbookTreeNodeBuilder;
import org.openl.rules.ui.tree.WorksheetTreeNodeBuilder;

public class FileView implements RulesTreeView {

    private static final BaseTableTreeNodeBuilder[] sorters = {
        new WorkbookTreeNodeBuilder(),
        new WorksheetTreeNodeBuilder(),
        new OpenMethodInstancesGroupTreeNodeBuilder(),
        new TableInstanceTreeNodeBuilder(),
        new TableVersionTreeNodeBuilder()
    };

    public String getName() {
    	return "file";
    }
    
    public String getDisplayName() {
    	return "File";
    }

    public String getDescription() {
    	return "Organize projects by physical location";
    }

    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}