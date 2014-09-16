package org.openl.rules.ui.tree.richfaces;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.base.INamedThing;
import org.openl.meta.number.NumberValue.ValueType;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.util.tree.ITreeElement;

abstract class TreeBuilder {

    private ITreeElement<?> root;
    private boolean hideDispatcherTables;

    TreeBuilder(ITreeElement<?> root, boolean hideDispatcherTables) {
        this.root = root;
        this.hideDispatcherTables = hideDispatcherTables;
    }

    public TreeNode build() {
        return build(false);
    }

    public TreeNode build(boolean hasRoot) {
        TreeNode node = buildNode(root);
        if (hasRoot) {
            // Wrap to root node
            TreeNode rfRoot = new TreeNode();
            rfRoot.addChild(0, node);
            node = rfRoot;
        }
        return node;
    }

    private TreeNode buildNode(ITreeElement<?> element) {
        if (element == null) {
            return createNullNode();
        }
        TreeNode node = createNode(element);
        Iterable<? extends ITreeElement<?>> children = getChildrenIterator(element);
        for (ITreeElement<?> child : children) {
            TreeNode rfChild = buildNode(child);
            if (hideDispatcherTables && rfChild.getName().startsWith(DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME)) {
                continue;
            }
            node.addChild(rfChild, rfChild);
        }
        return node;
    }

    Iterable<? extends ITreeElement<?>> getChildrenIterator(ITreeElement<?> source) {
        return source.getChildren();
    }

    private TreeNode createNode(ITreeElement<?> element) {
        boolean leaf = element.isLeaf();
        TreeNode node = new TreeNode(leaf);

        String name = getDisplayName(element, INamedThing.SHORT);
        node.setName(name);

        String title = getDisplayName(element, INamedThing.REGULAR);
        node.setTitle(title);

        String url = getUrl(element);
        node.setUrl(url);

        int state = getState(element);
        node.setState(state);

        int numErrors = getNumErrors(element);
        node.setNumErrors(numErrors);

        String type = getType(element);
        node.setType(type);

        boolean active = isActive(element);
        node.setActive(active);

        return node;
    }

    boolean isActive(ITreeElement<?> element) {
        return true;
    }

    private String getType(ITreeElement<?> element) {
        String type = element.getType();
        if (type != null) {
            return type;
        }
        return StringUtils.EMPTY;
    }

    abstract String getUrl(ITreeElement<?> element);

    String getDisplayName(Object obj, int mode) {
        if ((ClassUtils.isAssignable(obj.getClass(), Number.class, true))) {
            return FormattersManager.format(obj);
        }
        if (obj instanceof INamedThing) {
            INamedThing nt = (INamedThing) obj;
            return nt.getDisplayName(mode);
        }
        return String.valueOf(obj);
    }

    int getState(ITreeElement<?> element) {
        return 0;
    }

    int getNumErrors(ITreeElement<?> element) {
        return 0;
    }

    private TreeNode createNullNode() {
        TreeNode dest = new TreeNode(true);
        dest.setName("null");
        dest.setTitle("null");
        dest.setUrl(getUrl(null));
        dest.setType(ValueType.SINGLE_VALUE.toString());
        return dest;
    }
}
