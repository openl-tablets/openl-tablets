package org.openl.rules.webstudio.web.repository.tree;

import org.openl.rules.webstudio.web.repository.UiConst;

public class TreeProductFile extends TreeFile {

    public TreeProductFile(String id, String name) {
        super(id, name);
    }

    @Override
    public String getType() {
        return UiConst.TYPE_PRODUCTION_FILE;
    }

    /** {@inheritDoc} */
    @Override
    public String getIconLeaf() {
        // TODO: different types of files should have own icons
        return UiConst.ICON_FILE;
    }

    /** {@inheritDoc} */
    @Override
    public String getIcon() {
        // file is always leaf node
        return getIconLeaf();
    }
}
