package org.openl.rules.ui.tree;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.lang.xls.binding.TableVersionComparator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;
import org.openl.util.tree.ITreeElement;

/**
 * Folder tree node that represents table with several versions. If node has several versions then folder node will be
 * linked with one of its child. If node has only one version then it will be leaf in tree.
 *
 * Linked child is child with "active" table(there is only one "active" table among all version of the table) or the
 * child with greatest version.
 *
 * @author PUdalau
 */
public class VersionedTreeNode extends ProjectTreeNode {

    private TableSyntaxNode linkedChild;

    public VersionedTreeNode(String[] displayName, TableSyntaxNode table) {
        super(displayName,
            String.format("%s.%s", IProjectTypes.PT_TABLE, table.getType()).intern(),
            null,
            null,
            0,
            null);
    }

    @Override
    public String getType() {
        return String.format("%s.%s", IProjectTypes.PT_TABLE, linkedChild.getType()).intern();
    }

    @Override
    public String getUri() {
        return linkedChild.getUri();
    }

    @Override
    public Object getProblems() {
        return linkedChild.getErrors() != null ? linkedChild.getErrors() : linkedChild.getValidationResult();
    }

    @Override
    public boolean hasProblems() {
        return getProblems() != null;
    }

    @Override
    public TableSyntaxNode getTableSyntaxNode() {
        return linkedChild;
    }

    @Override
    public Map<Object, ITreeNode<Object>> getElements() {
        Map<Object, ITreeNode<Object>> elements = super.getElements();
        if (elements.size() < 2) {
            return new TreeMap<>();
        }
        return elements;
    }

    @Override
    public boolean isLeaf() {
        return getElements().size() < 1;
    }

    @Override
    public Collection<? extends ITreeElement<Object>> getChildren() {
        return getElements().values();
    }

    @Override
    public void addChild(Object key, ITreeNode<Object> child) {
        super.addChild(key, child);
        ProjectTreeNode newChild = (ProjectTreeNode) child;
        if (linkedChild == null) {
            linkedChild = newChild.getTableSyntaxNode();
        } else {
            if (findLaterTable(linkedChild, newChild.getTableSyntaxNode()) > 0) {
                linkedChild = newChild.getTableSyntaxNode();
            }
        }
    }

    public static int findLaterTable(TableSyntaxNode first, TableSyntaxNode second) {
        return TableVersionComparator.getInstance().compare(first, second);
    }
}
