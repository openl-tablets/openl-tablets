package org.openl.rules.ui.tree.richfaces;

import java.util.function.Predicate;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.ui.tree.ProjectTreeNode;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;

public class ProjectTreeBuilder {

    private Predicate<ProjectTreeNode> utilityTablePredicate;

    public ProjectTreeBuilder(Predicate<ProjectTreeNode> utilityTablePredicate) {
        this.utilityTablePredicate = utilityTablePredicate;
    }

    @Deprecated
    int getState(ProjectTreeNode element) {
        ProjectTreeNode pte = (ProjectTreeNode) element;
        if (pte.getTableSyntaxNode() != null && WebStudioUtils.getProjectModel().isTestable(pte.getTableSyntaxNode())) {
            return 2; // has tests
        }
        return 0;
    }

    int getNumErrors(ProjectTreeNode element) {
        ProjectTreeNode pte = (ProjectTreeNode) element;
        return pte.getNumErrors();
    }

    boolean isActive(ProjectTreeNode element) {
        ProjectTreeNode projectNode = (ProjectTreeNode) element;
        TableSyntaxNode syntaxNode = projectNode.getTableSyntaxNode();
        if (syntaxNode != null) {
            ITableProperties tableProperties = syntaxNode.getTableProperties();
            if (tableProperties != null) {
                Boolean active = tableProperties.getActive();
                if (active != null) {
                    return active;
                }
            }
        }
        return true;
    }

    String getUrl(ProjectTreeNode element) {
        String elementType = element.getType();
        if (elementType.startsWith(IProjectTypes.PT_TABLE + ".")) {
            TableSyntaxNode tsn = element.getTableSyntaxNode();
            return WebStudioUtils.getWebStudio().url("table?" + Constants.REQUEST_PARAM_ID + "=" + tsn.getId());
        }
        return null;
    }

    public TreeNode build(ProjectTreeNode root) {
        return buildNode(root);
    }

    private TreeNode buildNode(ProjectTreeNode element) {
        TreeNode node = createNode(element);
        Iterable<ProjectTreeNode> children = element.getChildren();
        for (ProjectTreeNode child : children) {
            if (utilityTablePredicate != null && utilityTablePredicate.test(child)) {
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

    private TreeNode createNode(ProjectTreeNode element) {
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

    String getType(ProjectTreeNode element) {
        String type = element.getType();
        if (type != null) {
            return type;
        }
        return StringUtils.EMPTY;
    }

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
}
