package org.openl.rules.dependency.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.graph.DefaultDirectedGraph;

import org.openl.binding.BindingDependencies;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ExecutableMethod;

/**
 * Dependency rules graph, implemented using {@link org.jgrapht.graph.DefaultDirectedGraph}. Vertexes are represented as
 * {@link ExecutableMethod}, edges {@link DirectedEdge}.
 *
 * @author DLiauchuk
 */
public class DependencyRulesGraph implements Graph<ExecutableMethod, DirectedEdge<ExecutableMethod>> {

    private final Graph<ExecutableMethod, DirectedEdge<ExecutableMethod>> graph = new DefaultDirectedGraph<>(null,
            DirectedEdge::new,
            false);

    private DependencyRulesGraph(Set<ExecutableMethod> rulesMethods) {
        fill(rulesMethods);
    }

    private void fill(Set<ExecutableMethod> rulesMethods) {
        Optional.ofNullable(rulesMethods)
                .stream()
                .flatMap(Collection::stream)
                .forEach(method -> {
                    graph.addVertex(method);
                    Optional.ofNullable(method.getDependencies())
                            .map(BindingDependencies::getRulesMethods)
                            .stream()
                            .flatMap(Collection::stream)
                            .forEach(dependentMethod -> {
                                graph.addVertex(dependentMethod);
                                graph.addEdge(method, dependentMethod);
                            });
                });
    }

    @Override
    public DirectedEdge<ExecutableMethod> addEdge(ExecutableMethod sourceVertex, ExecutableMethod targetVertex) {
        return graph.addEdge(sourceVertex, targetVertex);
    }

    @Override
    public boolean addEdge(ExecutableMethod sourceVertex,
                           ExecutableMethod targetVertex,
                           DirectedEdge<ExecutableMethod> e) {
        return graph.addEdge(sourceVertex, targetVertex, e);
    }

    @Override
    public boolean addVertex(ExecutableMethod v) {
        return graph.addVertex(v);
    }

    @Override
    public ExecutableMethod addVertex() {
        return graph.addVertex();
    }

    @Override
    public boolean containsEdge(DirectedEdge<ExecutableMethod> e) {
        return graph.containsEdge(e);
    }

    @Override
    public boolean containsEdge(ExecutableMethod sourceVertex, ExecutableMethod targetVertex) {
        return graph.containsEdge(sourceVertex, targetVertex);
    }

    @Override
    public boolean containsVertex(ExecutableMethod v) {
        return graph.containsVertex(v);
    }

    @Override
    public Set<DirectedEdge<ExecutableMethod>> edgeSet() {
        return graph.edgeSet();
    }

    @Override
    public Set<DirectedEdge<ExecutableMethod>> edgesOf(ExecutableMethod vertex) {
        return graph.edgesOf(vertex);
    }

    @Override
    public Set<DirectedEdge<ExecutableMethod>> getAllEdges(ExecutableMethod sourceVertex,
                                                           ExecutableMethod targetVertex) {
        return graph.getAllEdges(sourceVertex, targetVertex);
    }

    @Override
    public DirectedEdge<ExecutableMethod> getEdge(ExecutableMethod sourceVertex, ExecutableMethod targetVertex) {
        return graph.getEdge(sourceVertex, targetVertex);
    }

    @Override
    public Supplier<DirectedEdge<ExecutableMethod>> getEdgeSupplier() {
        return DirectedEdge::new;
    }

    @Override
    public ExecutableMethod getEdgeSource(DirectedEdge<ExecutableMethod> e) {
        return graph.getEdgeSource(e);
    }

    @Override
    public ExecutableMethod getEdgeTarget(DirectedEdge<ExecutableMethod> e) {
        return graph.getEdgeTarget(e);
    }

    @Override
    public double getEdgeWeight(DirectedEdge<ExecutableMethod> e) {
        return graph.getEdgeWeight(e);
    }

    @Override
    public void setEdgeWeight(DirectedEdge<ExecutableMethod> e, double weight) {
        graph.setEdgeWeight(e, weight);
    }

    @Override
    public GraphType getType() {
        return graph.getType();
    }

    @Override
    public int degreeOf(ExecutableMethod vertex) {
        return graph.degreeOf(vertex);
    }

    @Override
    public Supplier<ExecutableMethod> getVertexSupplier() {
        return graph.getVertexSupplier();
    }

    @Override
    public boolean removeAllEdges(Collection<? extends DirectedEdge<ExecutableMethod>> edges) {
        return graph.removeAllEdges(edges);
    }

    @Override
    public Set<DirectedEdge<ExecutableMethod>> removeAllEdges(ExecutableMethod sourceVertex,
                                                              ExecutableMethod targetVertex) {
        return graph.removeAllEdges(sourceVertex, targetVertex);
    }

    @Override
    public boolean removeAllVertices(Collection<? extends ExecutableMethod> vertices) {
        return graph.removeAllVertices(vertices);
    }

    @Override
    public boolean removeEdge(DirectedEdge<ExecutableMethod> e) {
        return graph.removeEdge(e);
    }

    @Override
    public DirectedEdge<ExecutableMethod> removeEdge(ExecutableMethod sourceVertex, ExecutableMethod targetVertex) {
        return graph.removeEdge(sourceVertex, targetVertex);
    }

    @Override
    public boolean removeVertex(ExecutableMethod v) {
        return graph.removeVertex(v);
    }

    @Override
    public Set<ExecutableMethod> vertexSet() {
        return graph.vertexSet();
    }

    public int inDegreeOf(ExecutableMethod vertex) {
        return graph.inDegreeOf(vertex);
    }

    public Set<DirectedEdge<ExecutableMethod>> incomingEdgesOf(ExecutableMethod vertex) {
        return graph.incomingEdgesOf(vertex);
    }

    public int outDegreeOf(ExecutableMethod vertex) {
        return graph.outDegreeOf(vertex);
    }

    public Set<DirectedEdge<ExecutableMethod>> outgoingEdgesOf(ExecutableMethod vertex) {
        return graph.outgoingEdgesOf(vertex);
    }

    /**
     * Filter incoming methods, finding {@link ExecutableMethod} and create graph.
     *
     * @param methods {@link IOpenMethod}
     * @return {@link DependencyRulesGraph} graph representing dependencies between executable rules methods.
     */
    public static DependencyRulesGraph filterAndCreateGraph(Collection<IOpenMethod> methods) {
        Set<ExecutableMethod> rulesMethods = new HashSet<>();
        if (methods != null && !methods.isEmpty()) {
            Queue<IOpenMethod> queue = new LinkedList<>(methods);
            while (!queue.isEmpty()) {
                var method = queue.poll();
                if (method instanceof ExecutableMethod executable) {
                    rulesMethods.add(executable);
                } else if (method instanceof OpenMethodDispatcher dispatcher) {
                    queue.addAll(dispatcher.getCandidates());
                }
            }
            return new DependencyRulesGraph(rulesMethods);
        } else {
            throw new OpenlNotCheckedException("There is no rules for building graph.");
        }
    }

}
