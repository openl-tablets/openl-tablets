package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.*;

public class ExcelSheetView extends ExcelSheetProfile implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = {
            new WorksheetTreeNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder() };

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}
