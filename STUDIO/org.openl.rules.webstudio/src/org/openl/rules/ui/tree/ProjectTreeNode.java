package org.openl.rules.ui.tree;

import java.util.Collection;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.util.tree.ITreeElement;

public class ProjectTreeNode extends TreeNode<Object> implements INamedThing {

    private String uri;
    private String[] displayName;
    private TableSyntaxNode tableSyntaxNode;

    public ProjectTreeNode(String[] displayName,
                           String type,
                           String uri,
                           TableSyntaxNode tsn) {

        setType(type);
        this.uri = uri;
        this.displayName = displayName;
        this.tableSyntaxNode = tsn;
    }

    public String[] getDisplayName() {
        return displayName;
    }

    @Override
    public String getDisplayName(int mode) {
        return displayName[mode];
    }

    @Override
    public String getName() {
        return getDisplayName(SHORT);
    }

    public String getUri() {
        return uri;
    }

    public int getNumErrors() {
        int result = 0;

        TableSyntaxNode table = getTableSyntaxNode();
        Collection<? extends ITreeElement<Object>> children = getChildren();
        if (table != null) {
            SyntaxNodeException[] errors = table.getErrors();
            if (errors != null) {
                result += errors.length;
            }
            if (children.isEmpty()) {
                return result;
            }
        }

        for (ITreeElement<Object> treeNode : children) {
            if (treeNode instanceof ProjectTreeNode) {
                ProjectTreeNode projectTreeNode = (ProjectTreeNode) treeNode;
                result += projectTreeNode.getNumErrors();
            }
        }
        return result;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }

}
