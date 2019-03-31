package org.openl.rules.dependency.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.openl.binding.BindingDependencies;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ExecutableMethod;

/**
 * Dependency rules graph, implemented using {@link org.jgrapht.graph.DefaultDirectedGraph}. Vertexes are represented as
 * {@link ExecutableMethod}, edges {@link DirectedEdge}.
 * 
 * @author DLiauchuk
 *
 */
public class DependencyRulesGraph implements DirectedGraph<ExecutableMethod, DirectedEdge<ExecutableMethod>> {

    private DirectedGraph<ExecutableMethod, DirectedEdge<ExecutableMethod>> graph;

    public DependencyRulesGraph(List<ExecutableMethod> rulesMethods) {
        createGraph();
        fill(rulesMethods);
    }

    public DependencyRulesGraph() {
        createGraph();
    }

    private void createGraph() {
        EdgeFactory<ExecutableMethod, DirectedEdge<ExecutableMethod>> edgeFactory = new DirectedEdgeFactory<>(
            DirectedEdge.class);
        graph = new DefaultDirectedGraph<>(edgeFactory);
    }

    private void fill(List<ExecutableMethod> rulesMethods) {
        if (rulesMethods != null && !rulesMethods.isEmpty()) {
            for (ExecutableMethod method : rulesMethods) {
                graph.addVertex(method);
                BindingDependencies dependencies = method.getDependencies();
                if (dependencies != null) {
                    Set<ExecutableMethod> dependentMethods = dependencies.getRulesMethods();
                    if (dependentMethods != null) {
                        for (ExecutableMethod dependentMethod : dependentMethods) {
                            graph.addVertex(dependentMethod);
                            graph.addEdge(method, dependentMethod);
                        }
                    }
                }
            }
        }
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
    public EdgeFactory<ExecutableMethod, DirectedEdge<ExecutableMethod>> getEdgeFactory() {
        return graph.getEdgeFactory();
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

    @Override
    public int inDegreeOf(ExecutableMethod arg0) {
        return graph.inDegreeOf(arg0);
    }

    @Override
    public Set<DirectedEdge<ExecutableMethod>> incomingEdgesOf(ExecutableMethod arg0) {
        return graph.incomingEdgesOf(arg0);
    }

    @Override
    public int outDegreeOf(ExecutableMethod arg0) {
        return graph.outDegreeOf(arg0);
    }

    @Override
    public Set<DirectedEdge<ExecutableMethod>> outgoingEdgesOf(ExecutableMethod arg0) {
        return graph.outgoingEdgesOf(arg0);
    }

    /**
     * Filter incoming methods, finding {@link ExecutableMethod} and create graph.
     * 
     * @param methods {@link IOpenMethod}
     * @return {@link DependencyRulesGraph} graph representing dependencies between executable rules methods.
     */
    public static DependencyRulesGraph filterAndCreateGraph(Collection<IOpenMethod> methods) {
        List<ExecutableMethod> rulesMethods = new ArrayList<>();
        if (methods != null && !methods.isEmpty()) {
            for (IOpenMethod method : methods) {
                if (method instanceof ExecutableMethod) {
                    rulesMethods.add((ExecutableMethod) method);
                }
            }
            return new DependencyRulesGraph(rulesMethods);
        } else {
            throw new OpenlNotCheckedException("There is no rules for building graph.");
        }
    }

}
