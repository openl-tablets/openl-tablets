package org.openl.rules.webstudio.web.diff;

import org.openl.rules.diff.tree.DiffStatus;
import org.openl.rules.diff.tree.DiffTreeNode;

public class UiTreeData2 extends UiTreeData {
    private String title;

    public UiTreeData2(DiffTreeNode node, String title) {
        super(node);
        this.title = title;
    }

//    @Override
    public DiffStatus getStatus() {
        return DiffStatus.DIFFERS;
    }

//    @Override
    public String getIcon() {
        // TODO use UiConst.ICON_DIFF_DIFFERS
        String icon = "/webresource/images/diff/propmodified.gif";
        return icon;
    }

//    @Override
    public String getName() {
        return title;
    }

//    @Override
    public String getType() {
        return "property";
    }
}
