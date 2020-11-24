package org.openl.rules.ui.tree.richfaces;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.ui.tree.ProjectTreeNode;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class ProjectTreeBuilder {

    public TreeNode build(ProjectTreeNode root) {
        TreeNode node = createNode(root);
        Iterable<ProjectTreeNode> children = root.getChildren();
        for (ProjectTreeNode child : children) {
            TreeNode rfChild = build(child);
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

        String name = element.getDisplayName(INamedThing.SHORT);
        node.setName(name);

        String title = element.getDisplayName(INamedThing.REGULAR);
        node.setTitle(title);

        String type = element.getType();
        node.setType(type);

        String url = null;
        if (type.startsWith(IProjectTypes.PT_TABLE + ".")) {
            TableSyntaxNode tsn = element.getTableSyntaxNode();
            url = WebStudioUtils.getWebStudio().url("table?" + Constants.REQUEST_PARAM_ID + "=" + tsn.getId());
        }
        node.setUrl(url);

        int state1 = 0;
        if (element.getTableSyntaxNode() != null && WebStudioUtils.getProjectModel()
            .isTestable(element.getTableSyntaxNode())) {
            state1 = 2; // has tests
        }
        int state = state1;
        node.setState(state);

        int numErrors = element.getNumErrors();
        node.setNumErrors(numErrors);

        boolean active = isActive(element);
        node.setActive(active);

        return node;
    }

    private boolean isActive(ProjectTreeNode element) {
        TableSyntaxNode syntaxNode = element.getTableSyntaxNode();
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
}
