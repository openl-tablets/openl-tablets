package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.*;

public class ExcelSheetView implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = {
            new WorksheetTreeNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder() };

    @Override
    public String getName() {
        return "excelSheet";
    }

    @Override
    public String getDisplayName() {
        return "Excel Sheet";
    }

    @Override
    public String getDescription() {
        return "Organize projects by physical file structure";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}
