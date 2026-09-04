package org.openl.rules.diff.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import lombok.Setter;

import org.openl.rules.diff.differs.ProjectionDiffer;
import org.openl.rules.diff.hierarchy.Projection;

public class DiffTreeBuilderImpl implements DiffTreeBuilder {
    @Setter
    private ProjectionDiffer projectionDiffer;

    @Override
    public DiffTreeNode compare(Projection p1, Projection p2) {
        return compare(new Projection[]{p1, p2});
    }

    @Override
    public DiffTreeNode compare(Projection[] projections) {
        if (projections.length < 2) {
            throw new IllegalArgumentException("At least 2 elements are required.");
        }

        if (projectionDiffer == null) {
            throw new IllegalStateException("projectionDiffer has not been set.");
        }

        var root = new DiffTreeNodeImpl();
        buildTree(root, projections);
        diffTree(root);
        return root;
    }

    protected void buildTree(DiffTreeNodeImpl root, Projection[] projections) {
        var len = projections.length;

        DiffElement[] diffElements = new DiffElementImpl[len];

        for (var i = 0; i < len; i++) {
            var p = projections[i];
            diffElements[i] = new DiffElementImpl(p);
        }

        root.setElements(diffElements);

        buildSubTree(root);
    }

    protected void buildSubTree(DiffTreeNodeImpl node) {
        var elements = node.getElements();
        var len = elements.length;

        @SuppressWarnings("unchecked")
        List<Projection>[] children = new List[len];

        for (var i = 0; i < len; i++) {
            var p = elements[i].getProjection();
            children[i] = getChildren(p);
        }

        var diffChildren = combineChildren(children);
        node.setChildren(diffChildren);

        for (DiffTreeNode child : diffChildren) {
            buildSubTree((DiffTreeNodeImpl) child);
        }
    }

    protected List<Projection> getChildren(Projection p) {
        if (p == null) {
            return List.of();
        }

        return p.getChildren();
    }

    protected List<DiffTreeNode> combineChildren(List<Projection>[] children) {
        var len = children.length;
        var uniqKeys = new TreeSet<ProjectionKey>();

        @SuppressWarnings("unchecked")
        Map<ProjectionKey, Projection>[] n2p = new HashMap[len];

        for (var i = 0; i < len; i++) {
            var map = new HashMap<ProjectionKey, Projection>();
            n2p[i] = map;

            for (Projection p : children[i]) {
                var key = new ProjectionKey(p);

                map.put(key, p);
                uniqKeys.add(key);
            }
        }

        var result = new ArrayList<DiffTreeNode>(uniqKeys.size());
        for (ProjectionKey key : uniqKeys) {
            DiffElementImpl[] diffElements = new DiffElementImpl[len];

            for (var j = 0; j < len; j++) {
                var p = n2p[j].get(key);
                diffElements[j] = new DiffElementImpl(p);
            }

            var node = new DiffTreeNodeImpl();
            node.setElements(diffElements);
            result.add(node);
        }

        return result;
    }

    protected void diffTree(DiffTreeNodeImpl node) {
        // from bottom...
        List<DiffTreeNode> children = node.getChildren();
        for (DiffTreeNode child : children) {
            diffTree((DiffTreeNodeImpl) child);
        }

        // ... to top
        var elements = node.getElements();
        var len = elements.length;

        var first = (DiffElementImpl) elements[0];
        var original = first.getProjection();
        first.asOriginal(original != null);

        for (var i = 1; i < len; i++) {
            compare(node, 0, i);
        }
    }

    /**
     * originalIdx & otherIdx have the same type and name
     */
    protected void compare(DiffTreeNodeImpl node, int originalIdx, int otherIdx) {
        var original = node.getElement(originalIdx).getProjection();
        var diff = (DiffElementImpl) node.getElement(otherIdx);
        var other = diff.getProjection();

        if (original == null) {
            if (other == null) {
                diff.asExists(true, true, true);
            } else {
                diff.asAdded();
            }
        } else {
            if (other == null) {
                diff.asRemoved();
            } else {
                // full compare
                var selfEqual = projectionDiffer.compare(original, other);

                var hierarhyEqual = true;
                var childrenEqual = true;

                for (DiffTreeNode child : node.getChildren()) {
                    var ce1 = child.getElement(originalIdx);
                    var ce2 = child.getElement(otherIdx);

                    var p1 = ce1.getProjection();
                    var p2 = ce2.getProjection();

                    if (!ce2.isHierarhyEqual() || p1 == null || p2 == null) {
                        hierarhyEqual = false;
                        childrenEqual = false;
                        break;
                    }
                    if (!ce2.isChildrenEqual() || !ce2.isSelfEqual()) {
                        childrenEqual = false;
                    }
                }

                diff.asExists(hierarhyEqual, childrenEqual, selfEqual);
            }
        }
    }

    /**
     * ProjectionKey distinct Projections by name and type.
     */
    static class ProjectionKey implements Comparable<ProjectionKey> {
        String name;
        String type;

        ProjectionKey(Projection p) {
            name = p.getName();
            type = p.getType();
        }

        @Override
        public int compareTo(ProjectionKey o) {
            var diff = type.compareTo(o.type);
            if (diff == 0) {
                diff = name.compareTo(o.name);
            }

            return diff;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof ProjectionKey other)) {
                return false;
            }

            return type.equals(other.type) && name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return type.hashCode() * 37 + name.hashCode();
        }
    }
}
