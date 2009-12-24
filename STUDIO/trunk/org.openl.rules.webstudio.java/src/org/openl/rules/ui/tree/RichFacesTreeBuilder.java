package org.openl.rules.ui.tree;

import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.base.INamedThing;
import org.openl.rules.ui.ObjectMap;
import org.openl.util.tree.ITreeElement;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

public class RichFacesTreeBuilder {

    private org.openl.rules.ui.tree.TreeNode<?> root;
    //protected ObjectMap indexNodeMap = new ObjectMap();

    public RichFacesTreeBuilder(org.openl.rules.ui.tree.TreeNode<?> root) {
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    public TreeNode<?> build() {
        TreeNode rfRoot = new TreeNodeImpl();
        addNodes(rfRoot, root);
        return rfRoot;
    }

    @SuppressWarnings("unchecked")
    private void addNodes(TreeNode<?> rfParent, org.openl.rules.ui.tree.TreeNode<?> parent) {
        int counter = 1;
        for (Iterator pi = parent.getChildren(); pi.hasNext();) {
            org.openl.rules.ui.tree.TreeNode child = (org.openl.rules.ui.tree.TreeNode) pi.next();
            TreeNode rfChild = new TreeNodeImpl();
            TreeNodeData data = getNodeData(child);
            rfChild.setData(data);
            rfParent.addChild(counter, rfChild);
            //indexNodeMap.getNewID(child);
            addNodes(rfChild, child);
            counter++;
        }
    }

    protected TreeNodeData getNodeData(org.openl.rules.ui.tree.TreeNode<?> node) {
        String name = StringEscapeUtils.escapeHtml(getDisplayName(node, INamedThing.SHORT));
        String title = StringEscapeUtils.escapeHtml(getDisplayName(node, INamedThing.REGULAR));
        String url = getUrl(node);
        String type = getType(node);
        int state = getState(node);
        TreeNodeData nodeData = new TreeNodeData(name, title, url, state, type);
        return nodeData;
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
