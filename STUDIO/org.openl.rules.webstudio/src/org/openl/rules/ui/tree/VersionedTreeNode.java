package org.openl.rules.ui.tree;

import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.lang.xls.binding.TableVersionComparator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;

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

    VersionedTreeNode(String[] displayName, TableSyntaxNode table) {
        super(displayName, String.format("%s.%s", IProjectTypes.PT_TABLE, table.getType()).intern(), null);
    }

    @Override
    public String getType() {
        return String.format("%s.%s", IProjectTypes.PT_TABLE, linkedChild.getType()).intern();
    }

    @Override
    public TableSyntaxNode getTableSyntaxNode() {
        return linkedChild;
    }

    @Override
    public Map<Object, ProjectTreeNode> getElements() {
        Map<Object, ProjectTreeNode> elements = super.getElements();
        if (elements.size() < 2) {
            return new TreeMap<>();
        }
        return elements;
    }

    @Override
    public void addChild(Object key, ProjectTreeNode child) {
        super.addChild(key, child);
        TableSyntaxNode childTableSyntaxNode = child.getTableSyntaxNode();
        if (linkedChild == null) {
            linkedChild = childTableSyntaxNode;
        } else {
            if (findLaterTable(linkedChild, childTableSyntaxNode) > 0) {
                linkedChild = childTableSyntaxNode;
            }
        }
    }

    static int findLaterTable(TableSyntaxNode first, TableSyntaxNode second) {
        return TableVersionComparator.getInstance().compare(first, second);
    }
}
