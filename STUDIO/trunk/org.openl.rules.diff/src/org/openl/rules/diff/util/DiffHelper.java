package org.openl.rules.diff.util;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.tree.DiffTreeNode;

/**
 * @deprecated delete
 */
public class DiffHelper {

    /** @deprecated delete */
    public static List<DiffTreeNode> getDiffNodesByType(DiffTreeNode parent, String type) {
        List<DiffTreeNode> nodes = new ArrayList<DiffTreeNode>();
        for (DiffTreeNode child : parent.getChildren()) {
            DiffElement[] elements = child.getElements();
            Projection proj = null;
            int i = 0;
            do {
                proj = child.getElements()[i++].getProjection();
            } while(i < elements.length && proj == null);
            if (proj != null) {
                if (proj.getType().equals(type)) {
                    nodes.add(child);
                } else {
                    nodes.addAll((getDiffNodesByType(child, type)));
                }
            }
        }
        return nodes;
    }
}
