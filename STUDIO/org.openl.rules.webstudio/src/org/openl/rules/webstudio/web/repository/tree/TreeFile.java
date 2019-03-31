package org.openl.rules.webstudio.web.repository.tree;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openl.rules.webstudio.web.repository.UiConst;

/**
 * Represents OpenL file in a tree.
 *
 * @author Aleh Bykhavets
 *
 */
public class TreeFile extends AbstractTreeNode {

    private static final long serialVersionUID = -4563895481021883236L;
    private static final List<TreeNode> EMPTY_LIST = new LinkedList<>();

    public TreeFile(String id, String name) {
        // File cannot have children !!!
        super(id, name, true);
    }

    // ------ UI methods ------

    @Override
    public List<TreeNode> getChildNodes() {
        return EMPTY_LIST;
    }

    /** {@inheritDoc} */
    @Override
    public String getIcon() {
        // file is always leaf node
        return getIconLeaf();
    }

    /** {@inheritDoc} */
    @Override
    public String getIconLeaf() {
        // TODO: different types of files should have own icons
        return UiConst.ICON_FILE;
    }

    @Override
    public String getType() {
        return UiConst.TYPE_FILE;
    }

    @Override
    public String getId() {
        return TreeNode.FILE_PREFIX + super.getId();
    }

    @Override
    public Map<Object, TreeNode> getElements() {
        return null;
    }
}
