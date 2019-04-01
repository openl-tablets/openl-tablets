package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.*;

public class FileView implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = { new WorkbookTreeNodeBuilder(),
            new WorksheetTreeNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder() };

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getDisplayName() {
        return "File";
    }

    @Override
    public String getDescription() {
        return "Organize projects by physical location";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}