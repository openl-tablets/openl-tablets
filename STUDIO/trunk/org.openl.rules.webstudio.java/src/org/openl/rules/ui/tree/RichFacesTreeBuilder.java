package org.openl.rules.ui.tree;

import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.openl.base.INamedThing;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.StringTool;
import org.openl.util.tree.ITreeElement;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

public class RichFacesTreeBuilder {

    public RichFacesTreeBuilder() {
    }

    @SuppressWarnings("unchecked")
    public TreeNode<?> build(org.openl.rules.ui.tree.TreeNode<?> root) {
        TreeNode<?> rfRoot = new TreeNodeImpl();
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
            addNodes(rfChild, child);
            counter++;
        }
    }

    protected TreeNodeData getNodeData(org.openl.rules.ui.tree.TreeNode<?> node) {
        String name = StringEscapeUtils.escapeHtml(getDisplayName(node, INamedThing.SHORT));
        String title = StringEscapeUtils.escapeHtml(getDisplayName(node, INamedThing.REGULAR));
        String url = getUrl(node);
        String icon = getIcon(node);
        String type = getType(node);
        TreeNodeData nodeData = new TreeNodeData(name, title, url, icon, type);
        return nodeData;
    }

    protected String getType(ITreeElement<?> element) {
        String type = element.getType();
        if (type != null) {
            return type;
        }
        return "folder";
    }

    protected String getIcon(ITreeElement<?> element) {
        return null;
    }

    @SuppressWarnings("unchecked")
    protected String getUrl(ITreeElement element) {
        String elementType = element.getType();
        if (elementType.startsWith(IProjectTypes.PT_TABLE + ".")) {
            String uri = ((ProjectTreeNode) element).getUri();
            return "tableeditor/showTable.xhtml" + "?" + Constants.REQUEST_PARAM_URI + "=" + StringTool.encodeURL(uri);
        } else if (elementType.startsWith(IProjectTypes.PT_PROBLEM)) {
            return "tableeditor/showError.xhtml" + "?" + Constants.REQUEST_PARAM_ID + "=1";
        }
        return null;
    }

    protected String getDisplayName(Object obj, int mode) {
        if (obj instanceof INamedThing) {
            INamedThing nt = (INamedThing) obj;
            return nt.getDisplayName(mode);
        }
        return getCustomDisplayName(obj);
    }

    protected String getCustomDisplayName(Object obj) {
        return String.valueOf(obj);
    }
}
