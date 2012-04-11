package org.openl.rules.ui.tree.richfaces;

import java.util.Iterator;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.base.INamedThing;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.ui.tree.AbstractTreeBuilder;
import org.openl.util.tree.ITreeElement;

public class TreeBuilder extends AbstractTreeBuilder<TreeNode> {

    private ITreeElement<?> root;

    public TreeBuilder(ITreeElement<?> root) {
        this.root = root;
    }

    @Override
    public TreeNode build() {
        return build(false);
    }

    public TreeNode build(boolean hasRoot) {
        TreeNode rfTree = new TreeNode();
        addNodes(rfTree, root);
        if (hasRoot) {
            setNodeData(root, rfTree);
            TreeNode rfRoot = new TreeNode();
            rfRoot.addChild(0, rfTree);
            return rfRoot;
        }
        return rfTree;
    }

    @SuppressWarnings("unchecked")
    private void addNodes(TreeNode dest, ITreeElement<?> source) {
        int index = 1;
        for (Iterator<?> pi = source.getChildren(); pi.hasNext();) {
            ITreeElement<?> child = (ITreeElement) pi.next();
            TreeNode rfChild = toRFNode(child);
            dest.addChild(index, rfChild);
            addNodes(rfChild, child);
            index++;
        }
    }

    private TreeNode toRFNode(ITreeElement<?> node) {
        TreeNode rfNode = new TreeNode(node.isLeaf());
        setNodeData(node, rfNode);
        return rfNode;
    }

    protected void setNodeData(ITreeElement<?> source, TreeNode dest) {
        String name = getDisplayName(source, INamedThing.SHORT);
        String title = getDisplayName(source, INamedThing.REGULAR);
        String url = getUrl(source);
        int state = getState(source);
        String type = getType(source);
        boolean active = isActive(source);

        dest.setName(name);
        dest.setTitle(title);
        dest.setUrl(url);
        dest.setState(state);
        dest.setType(type);
        dest.setActive(active);
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
        if ((ClassUtils.isAssignable(obj.getClass(), Number.class, true))) {
            return FormattersManager.getFormatter(obj).format(obj);
        }
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
