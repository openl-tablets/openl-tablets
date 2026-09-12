package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.ui.tree.WorksheetTreeNodeBuilder;

public class ExcelSheetView extends ExcelSheetProfile implements RulesTreeView {

    /**
     * A fresh set of builders for every tree that is built.
     *
     * <p>A builder carries the state of the tree being built — the dictionary of overloaded methods the groups
     * are named from — so a set shared between two builds running at once would leave each of them reading the
     * other's module.
     */
    @Override
    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return new BaseTableTreeNodeBuilder[]{new WorksheetTreeNodeBuilder(),
                new OpenMethodInstancesGroupTreeNodeBuilder(),
                new TableInstanceTreeNodeBuilder(),
                new TableVersionTreeNodeBuilder()};
    }

}
