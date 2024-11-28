package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.ui.tree.WorksheetTreeNodeBuilder;

public class ExcelSheetView extends ExcelSheetProfile implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = {
            new WorksheetTreeNodeBuilder(),
            new OpenMethodInstancesGroupTreeNodeBuilder(),
            new TableInstanceTreeNodeBuilder(),
            new TableVersionTreeNodeBuilder()};

    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}
