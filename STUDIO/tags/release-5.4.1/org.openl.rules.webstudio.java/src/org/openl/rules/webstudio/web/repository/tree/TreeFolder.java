package org.openl.rules.webstudio.web.repository.tree;

import org.openl.rules.webstudio.web.repository.UiConst;

/**
 * Represents OpenL folder in a tree.
 *
 * @author Aleh Bykhavets
 *
 */
public class TreeFolder extends AbstractTreeNode {

    private static final long serialVersionUID = -8236498990436429491L;

    public TreeFolder(String id, String name) {
        super(id, name);
    }

    // ------ UI methods ------

    /** {@inheritDoc} */
    @Override
    public String getIcon() {
        return UiConst.ICON_FOLDER;
    }

    /** {@inheritDoc} */
    @Override
    public String getIconLeaf() {
        // in both cases we use the same icons
        return getIcon();
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        return UiConst.TYPE_FOLDER;
    }
}
