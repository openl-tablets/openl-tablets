package org.openl.rules.datatype.binding;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;

/**
 * Created by dl on 4/16/14.
 */
class TopologicalSort<T> {
    Set<TopoGraphNode<T>> sort(final Collection<TopoGraphNode<T>> nodes)
            throws IllegalStateException {
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
            return;
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

    static class TopoGraphNode<T> {
        private List<TopoGraphNode<T>> dependencies = new ArrayList<TopoGraphNode<T>>();

        private T obj;

        TopoGraphNode(T obj) {
            this.obj = obj;
        }

        List<TopoGraphNode<T>> getDependents() {
            return dependencies;
        }

        void addDependency(TopoGraphNode<T> node) {
            dependencies.add(node);
        }

        T getObj() {
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
