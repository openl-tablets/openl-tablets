package org.openl.rules.datatype.binding;

import java.util.*;

/**
 * Created by dl on 4/16/14.
 */
public class TopologicalSort<T> {
    public Set<TopoGraphNode<T>> sort(final Collection<TopoGraphNode<T>> nodes) {
        final LinkedHashSet<TopoGraphNode<T>> order = new LinkedHashSet<>();
        final Set<TopoGraphNode<T>> visited = new HashSet<>();

        final Set<TopoGraphNode<T>> alreadySeen = new HashSet<>();
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

    public static class TopoGraphNode<T> {
        private List<TopoGraphNode<T>> dependencies = new ArrayList<>();

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
                TopoGraphNode<?> node = (TopoGraphNode<?>) obj;
                return Objects.equals(this.obj, node.getObj());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(obj);
        }
    }
}
