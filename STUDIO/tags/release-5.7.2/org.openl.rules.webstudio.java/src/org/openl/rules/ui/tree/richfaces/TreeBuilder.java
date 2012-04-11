package org.openl.rules.ui.tree.richfaces;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.openl.base.INamedThing;
import org.openl.rules.ui.tree.AbstractTreeBuilder;
import org.openl.rules.ui.tree.TreeNodeData;
import org.openl.util.tree.ITreeElement;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

public class TreeBuilder extends AbstractTreeBuilder<TreeNode<?>> {

    private ITreeElement<?> root;

    public TreeBuilder(ITreeElement<?> root) {
        this.root = root;
    }

    @Override
    public TreeNode<?> build() {
        return build(false);
    }

    @SuppressWarnings("unchecked")
    public TreeNode<?> build(boolean hasRoot) {
        TreeNode rfTree = new TreeNodeImpl();
        addNodes(rfTree, root);
        if (hasRoot) {
            TreeNodeData data = getNodeData(root);
            rfTree.setData(data);
            TreeNode rfRoot = new TreeNodeImpl();
            rfRoot.addChild(0, rfTree);
            return rfRoot;
        }
        return rfTree;
    }

    @SuppressWarnings("unchecked")
    private void addNodes(TreeNode<?> rfParent, ITreeElement<?> parent) {
        int index = 1;
        for (Iterator pi = parent.getChildren(); pi.hasNext();) {
            ITreeElement child = (ITreeElement) pi.next();
            TreeNode rfChild = toRFNode(child);
            rfParent.addChild(index, rfChild);
            addNodes(rfChild, child);
            index++;
        }
    }

    @SuppressWarnings("unchecked")
    private TreeNode<?> toRFNode(ITreeElement<?> node) {
        TreeNode rfNode = new TreeNodeImpl();
        TreeNodeData data = getNodeData(node);
        rfNode.setData(data);
        return rfNode;
    }

    protected TreeNodeData getNodeData(ITreeElement<?> node) {
        String name = getDisplayName(node, INamedThing.SHORT);
        String title = getDisplayName(node, INamedThing.REGULAR);
        String url = getUrl(node);
        String type = getType(node);
        int state = getState(node);
        boolean active = isActive(node);
        TreeNodeData nodeData = new TreeNodeData(name, title, url, state, type, active);
        return nodeData;
    }

    protected boolean isActive(ITreeElement<?> element) {
        return true;
    }

    protected String getType(ITreeElement<?> element) {
        String type = element.getType();
        if (type != null) {
            return type;
        }
        return StringUtils.EMPTY;
    }

    protected String getUrl(ITreeElement<?> element) {
        return StringUtils.EMPTY;
    }

    protected String getDisplayName(Object obj, int mode) {
        if (obj instanceof INamedThing) {
            INamedThing nt = (INamedThing) obj;
            return nt.getDisplayName(mode);
        }
        return String.valueOf(obj);
    }

    protected int getState(ITreeElement<?> element) {
        return 0;
    }

}
