package org.openl.rules.diff.test;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.hierarchy.ProjectionProperty;
import org.openl.rules.diff.tree.DiffTreeBuilder;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.xls.XlsProjectionType;
import org.openl.rules.diff.differs.ProjectionDiffer;

import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;

public class DiffTreeBuilderImpl implements DiffTreeBuilder {
    private ProjectionDiffer projectionDiffer;
    private int idCounter = 0;

    private String getId() {
        return "_" + idCounter++;
    }
    
    public void setProjectionDiffer(ProjectionDiffer projectionDiffer) {
        this.projectionDiffer = projectionDiffer;
    }

    public DiffTreeNode compare(Projection p1, Projection p2) {
        return compare(new Projection[]{p1, p2});
    }

    public DiffTreeNode compare(Projection[] projections) {
        if (projections.length < 2) {
            throw new IllegalArgumentException("At least 2 elemnts is required!");
        }

        if (projectionDiffer == null) {
            throw new IllegalStateException("projectionDiffer was not set!");
        }

        DiffTreeNodeImpl root = new DiffTreeNodeImpl();
        root.setId(getId());
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
            for (ProjectionProperty projectionProperty : p.getProperties()) {
                diffElements[i].addDiffProperty(new DiffPropertyImpl(projectionProperty));
            }
        }

        root.setElements(diffElements);

        buildSubTree(root);
    }

    protected void buildSubTree(DiffTreeNodeImpl node) {
        DiffElement[] elements = node.getElements();
        int len = elements.length;

        Projection[][] children = new Projection[len][];

        for (int i = 0; i < len; i++) {
            Projection p = elements[i].getProjection();
            children[i] = getChildren(p);
            if (p != null) {
                for (ProjectionProperty projectionProperty : p.getProperties()) {
                    elements[i].addDiffProperty(new DiffPropertyImpl(projectionProperty));
                }
            }
        }

        DiffTreeNodeImpl[] diffChildren = combineChildren(children);
        node.setChildren(diffChildren);

        for (DiffTreeNodeImpl child : diffChildren) {
            buildSubTree(child);
        }
    }

    protected Projection[] getChildren(Projection p) {
        if (p == null) return new Projection[0];

        return p.getChildren();
    }

    protected DiffTreeNodeImpl[] combineChildren(Projection[][] children) {
        int len = children.length;
        Set<ProjectionKey> uniqKeys = new TreeSet<ProjectionKey>();

        Map<ProjectionKey, Projection>[] n2p = (Map<ProjectionKey, Projection>[]) new HashMap[len];

        for (int i = 0; i < len; i++) {
            Projection[] projections = children[i];
            Map<ProjectionKey, Projection> map = new HashMap<ProjectionKey, Projection>();
            n2p[i] = map;

            for (Projection p : projections) {
                ProjectionKey key = new ProjectionKey(p);

                map.put(key, p);
                uniqKeys.add(key);
            }
        }

        DiffTreeNodeImpl[] result = new DiffTreeNodeImpl[uniqKeys.size()];
        int i = 0;
        for (ProjectionKey key : uniqKeys) {
            DiffElementImpl[] diffElements = new DiffElementImpl[len];

            for (int j = 0; j < len; j++) {
                Projection p = n2p[j].get(key);
                diffElements[j] = new DiffElementImpl(p);
            }

            result[i] = new DiffTreeNodeImpl();
            result[i].setElements(diffElements);
            result[i].setId(getId());
            i++;
        }

        return result;
    }

    protected void diffTree(DiffTreeNodeImpl node) {
        // from bottom...
        DiffTreeNode[] children = node.getChildren();
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

    protected void compare(DiffTreeNodeImpl node, int originalIdx, int otherIdx) {
        Projection original = node.getElement(originalIdx).getProjection();
        DiffElementImpl diff = node.getElement(otherIdx);
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
                diff.setDiffProperties(projectionDiffer.getDiffProperties());

                boolean hierarhyEqual = true;
                boolean childrenEqual = true;

                DiffTreeNode[] children = node.getChildren();
                for (DiffTreeNode child : children) {
                    DiffElementImpl ce1 = ((DiffTreeNodeImpl) child).getElement(originalIdx);
                    DiffElementImpl ce2 = ((DiffTreeNodeImpl) child).getElement(otherIdx);

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


    static class ProjectionKey implements Comparable<ProjectionKey> {
        String name;
        String type;

        ProjectionKey(Projection p) {
            type = p.getType();
            name = p.getName();
            /* Temp hardcode */
            if (type.equalsIgnoreCase(XlsProjectionType.CELL.name())) {
                name = name.split("-")[0].trim();
            }
        }

        public int compareTo(ProjectionKey o) {
            int diff = type.compareTo(o.type);
            if (diff == 0) {
                diff = name.compareTo(o.name);
            }

            return diff;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof ProjectionKey)) return false;
            ProjectionKey other = (ProjectionKey) obj;

            return type.equals(other.type) && name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return type.hashCode() * 37 + name.hashCode();
        }
    }
}
