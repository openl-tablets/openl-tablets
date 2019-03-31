package org.openl.rules.diff.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openl.rules.diff.differs.ProjectionDiffer;
import org.openl.rules.diff.hierarchy.Projection;

public class DiffTreeBuilderImpl implements DiffTreeBuilder {
    private ProjectionDiffer projectionDiffer;

    public void setProjectionDiffer(ProjectionDiffer projectionDiffer) {
        this.projectionDiffer = projectionDiffer;
    }

    @Override
    public DiffTreeNode compare(Projection p1, Projection p2) {
        return compare(new Projection[] { p1, p2 });
    }

    @Override
    public DiffTreeNode compare(Projection[] projections) {
        if (projections.length < 2) {
            throw new IllegalArgumentException("At least 2 elements are required!");
        }

        if (projectionDiffer == null) {
            throw new IllegalStateException("projectionDiffer was not set!");
        }

        DiffTreeNodeImpl root = new DiffTreeNodeImpl();
        buildTree(root, projections);
        diffTree(root);
        return root;
    }

    protected void buildTree(DiffTreeNodeImpl root, Projection[] projections) {
        int len = projections.length;

        DiffElement[] diffElements = new DiffElementImpl[len];

        for (int i = 0; i < len; i++) {
            Projection p = projections[i];
            diffElements[i] = new DiffElementImpl(p);
        }

        root.setElements(diffElements);

        buildSubTree(root);
    }

    protected void buildSubTree(DiffTreeNodeImpl node) {
        DiffElement[] elements = node.getElements();
        int len = elements.length;

        @SuppressWarnings("unchecked")
        List<Projection>[] children = new List[len];

        for (int i = 0; i < len; i++) {
            Projection p = elements[i].getProjection();
            children[i] = getChildren(p);
        }

        List<DiffTreeNode> diffChildren = combineChildren(children);
        node.setChildren(diffChildren);

        for (DiffTreeNode child : diffChildren) {
            buildSubTree((DiffTreeNodeImpl) child);
        }
    }

    protected List<Projection> getChildren(Projection p) {
        if (p == null) {
            return Collections.emptyList();
        }

        return p.getChildren();
    }

    protected List<DiffTreeNode> combineChildren(List<Projection>[] children) {
        int len = children.length;
        Set<ProjectionKey> uniqKeys = new TreeSet<>();

        @SuppressWarnings("unchecked")
        Map<ProjectionKey, Projection>[] n2p = new HashMap[len];

        for (int i = 0; i < len; i++) {
            Map<ProjectionKey, Projection> map = new HashMap<>();
            n2p[i] = map;

            for (Projection p : children[i]) {
                ProjectionKey key = new ProjectionKey(p);

                map.put(key, p);
                uniqKeys.add(key);
            }
        }

        List<DiffTreeNode> result = new ArrayList<>(uniqKeys.size());
        for (ProjectionKey key : uniqKeys) {
            DiffElementImpl[] diffElements = new DiffElementImpl[len];

            for (int j = 0; j < len; j++) {
                Projection p = n2p[j].get(key);
                diffElements[j] = new DiffElementImpl(p);
            }

            DiffTreeNodeImpl node = new DiffTreeNodeImpl();
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
        DiffElement[] elements = node.getElements();
        int len = elements.length;

        DiffElementImpl first = (DiffElementImpl) elements[0];
        Projection original = first.getProjection();
        first.asOriginal(original != null);

        for (int i = 1; i < len; i++) {
            compare(node, 0, i);
        }
    }

    /**
     * originalIdx & otherIdx have the same type and name
     */
    protected void compare(DiffTreeNodeImpl node, int originalIdx, int otherIdx) {
        Projection original = node.getElement(originalIdx).getProjection();
        DiffElementImpl diff = (DiffElementImpl) node.getElement(otherIdx);
        Projection other = diff.getProjection();

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
                boolean selfEqual = projectionDiffer.compare(original, other);

                boolean hierarhyEqual = true;
                boolean childrenEqual = true;

                for (DiffTreeNode child : node.getChildren()) {
                    DiffElement ce1 = child.getElement(originalIdx);
                    DiffElement ce2 = child.getElement(otherIdx);

                    Projection p1 = ce1.getProjection();
                    Projection p2 = ce2.getProjection();

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
            int diff = type.compareTo(o.type);
            if (diff == 0) {
                diff = name.compareTo(o.name);
            }

            return diff;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof ProjectionKey))
                return false;
            ProjectionKey other = (ProjectionKey) obj;

            return type.equals(other.type) && name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return type.hashCode() * 37 + name.hashCode();
        }
    }
}
