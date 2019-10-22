package org.openl.rules.ui.tree.richfaces;

import org.openl.base.INamedThing;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.ui.IProjectTypes;
import org.openl.util.ClassUtils;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.openl.util.tree.ITreeElement;

abstract class TreeBuilder {

    private CollectionUtils.Predicate<ITreeElement> utilityTablePredicate;

    TreeBuilder() {
    }

    TreeBuilder(CollectionUtils.Predicate<ITreeElement> utilityTablePredicate) {
        this.utilityTablePredicate = utilityTablePredicate;
    }

    public TreeNode build(ITreeElement<?> root) {
        return buildNode(root);
    }

    public TreeNode buildWithRoot(ITreeElement<?> root) {
        TreeNode subNode = build(root);
        // Wrap to root node
        TreeNode node = new TreeNode();
        node.addChild(0, subNode);
        return node;
    }

    private TreeNode buildNode(ITreeElement<?> element) {
        if (element == null) {
            return createNullNode();
        }
        TreeNode node = createNode(element);
        Iterable<? extends ITreeElement<?>> children = element.getChildren();
        for (ITreeElement<?> child : children) {
            if (utilityTablePredicate != null && utilityTablePredicate.evaluate(child)) {
                continue;
            }
            TreeNode rfChild = buildNode(child);
            if (IProjectTypes.PT_WORKSHEET.equals(rfChild.getType()) || IProjectTypes.PT_WORKBOOK
                .equals(rfChild.getType())) {
                // skip workbook or worksheet node if it has no children nodes
                if (!rfChild.getChildrenKeysIterator().hasNext()) {
                    continue;
                }
            }
            node.addChild(rfChild, rfChild);
        }
        return node;
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

    String getType(ITreeElement<?> element) {
        String type = element.getType();
        if (type != null) {
            return type;
        }
        return StringUtils.EMPTY;
    }

    abstract String getUrl(ITreeElement<?> element);

    String getDisplayName(Object obj, int mode) {
        if (ClassUtils.isAssignable(obj.getClass(), Number.class)) {
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
        dest.setType("value");
        return dest;
    }
}
