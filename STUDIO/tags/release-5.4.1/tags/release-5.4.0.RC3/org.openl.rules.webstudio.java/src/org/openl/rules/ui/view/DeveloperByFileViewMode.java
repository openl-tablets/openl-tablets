package org.openl.rules.ui.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.ui.tree.WorkbookTreeNodeBuilder;
import org.openl.rules.ui.tree.WorksheetTreeNodeBuilder;

public class DeveloperByFileViewMode extends BaseDeveloperViewMode {

    private static final BaseTableTreeNodeBuilder[] sorters = {
        new WorkbookTreeNodeBuilder(),
        new WorksheetTreeNodeBuilder(),
        new OpenMethodInstancesGroupTreeNodeBuilder(),
        new TableInstanceTreeNodeBuilder(),
        new TableVersionTreeNodeBuilder()
    };

    public DeveloperByFileViewMode() {
        setName(getType() + ".byFile");
        displayName = "By File";
        description = "Developer Mode 2. Organize project by physical location";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}