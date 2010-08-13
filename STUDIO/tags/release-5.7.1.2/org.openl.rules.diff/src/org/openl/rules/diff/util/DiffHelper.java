package org.openl.rules.diff.util;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.hierarchy.ProjectionProperty;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.tree.DiffTreeNode;

public class DiffHelper {

    public static Object getPropValue(ProjectionProperty[] props, String propName) {
        for (ProjectionProperty prop : props) {
            if (prop.getName().equalsIgnoreCase((propName))) {
                return prop.getRawValue();
            }
        }
        return null;
    }

    public static List<DiffTreeNode> getDiffNodesByType(DiffTreeNode parent, String type) {
        List<DiffTreeNode> nodes = new ArrayList<DiffTreeNode>();
        DiffTreeNode[] children = parent.getChildren();
        for (DiffTreeNode child : children) {
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

    public static DiffTreeNode getDiffNodeById(DiffTreeNode parent, String id) {
        if (parent != null) {
            if (id == null || id.equals(parent.getId())) {
                return parent;
            } else {
                for (DiffTreeNode child : parent.getChildren()) {
                    DiffTreeNode found = getDiffNodeById(child, id);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

}
