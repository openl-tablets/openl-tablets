package org.openl.rules.datatype.binding;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.*;

/**
 * Created by dl on 4/16/14.
 */
public class TopologicalSort<T extends Object> {
    public Set<TopoGraphNode<T>> sort(final Collection<TopoGraphNode<T>> nodes)
            throws IllegalStateException
    {
        final LinkedHashSet<TopoGraphNode<T>> order = new LinkedHashSet<TopoGraphNode<T>>();
        final Set<TopoGraphNode<T>> visited = new HashSet<TopoGraphNode<T>>();

        final Set<TopoGraphNode<T>> alreadySeen = new HashSet<TopoGraphNode<T>>();
        for (final TopoGraphNode<T> n : nodes) {
            alreadySeen.clear();
            visit(n, alreadySeen, visited, order);
        }

        return order;
    }

    private void visit(final TopoGraphNode<T> n, final Set<TopoGraphNode<T>> alreadySeen,
                              final Set<TopoGraphNode<T>> visited, final LinkedHashSet<TopoGraphNode<T>> order) {
        if (alreadySeen.contains(n)) {
            throw new IllegalStateException("cycle containing " + n + " found!");
        }

        alreadySeen.add(n);

        if (!visited.contains(n)) {
            visited.add(n);
            for (final TopoGraphNode<T> m : n.getDependents()) {
                visit(m, alreadySeen, visited, order);
            }

            order.add(n);
        }

        alreadySeen.remove(n);
    }

    public static class TopoGraphNode<T extends Object> {
        private List<TopoGraphNode<T>> dependencies = new ArrayList<TopoGraphNode<T>>();

        private T obj;

        public TopoGraphNode(T obj) {
            this.obj = obj;
        }

        public List<TopoGraphNode<T>> getDependents() {
            return dependencies;
        }

        public void addDependency(TopoGraphNode<T> node) {
            dependencies.add(node);
        }

        public T getObj() {
            return obj;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TopoGraphNode) {
                TopoGraphNode<T> node = (TopoGraphNode<T>) obj;
                return new EqualsBuilder().append(this.obj, node.getObj()).isEquals();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(obj).toHashCode();
        }
    }
}
