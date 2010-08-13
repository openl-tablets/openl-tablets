package org.openl.rules.ui.tree;

import java.util.Iterator;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class ProjectTreeNode extends TreeNode<Object> implements INamedThing {

    private String uri;
    private String[] displayName;
    private int nameCount = 0;
    private TableSyntaxNode tableSyntaxNode;
    private Object problems;

    public ProjectTreeNode(String[] displayName, String type, String uri, Object problems, int nameCount,
            TableSyntaxNode tsn) {

        setType(type);
        this.uri = uri;
        this.displayName = displayName;
        this.problems = problems;
        this.nameCount = nameCount;
        this.tableSyntaxNode = tsn;
    }

    public String[] getDisplayName() {
        return displayName;
    }

    public String getDisplayName(int mode) {
        return displayName[mode];
    }

    public String getName() {
        return getDisplayName(SHORT);
    }

    public int getNameCount() {
        return nameCount;
    }

    public Object getProblems() {
        return problems;
    }

    public String getUri() {
        return uri;
    }

    public boolean hasProblems() {

        if (problems != null) {
            return true;
        }

        Iterator<ITreeNode<Object>> iterator = getChildren();

        while (iterator.hasNext()) {

            ITreeNode<Object> treeNode = iterator.next();

            if (treeNode instanceof ProjectTreeNode) {

                ProjectTreeNode projectTreeNode = (ProjectTreeNode) treeNode;

                if (projectTreeNode.hasProblems()) {
                    return true;
                }
            }
        }

        return false;
    }

    public void setProblems(Object problems) {
        this.problems = problems;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }
    
    
}
