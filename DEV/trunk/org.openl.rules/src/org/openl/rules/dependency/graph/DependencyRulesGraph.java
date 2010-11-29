package org.openl.rules.dependency.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.openl.binding.BindingDependencies;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ExecutableRulesMethod;

/**
 * Dependency rules graph, implemented using {@link org.jgrapht.graph.DefaultDirectedGraph}.
 * Vertexes are represented as {@link ExecutableRulesMethod}, 
 * edges {@link org.jgrapht.graph.DefaultEdge}.
 * 
 * @author DLiauchuk
 *
 */
public class DependencyRulesGraph implements DirectedGraph<ExecutableRulesMethod, DirectedEdge<ExecutableRulesMethod>> {

    private DirectedGraph<ExecutableRulesMethod, DirectedEdge<ExecutableRulesMethod>> graph;

    public DependencyRulesGraph(List<ExecutableRulesMethod> rulesMethods) {
        createGraph();
        fill(rulesMethods);
    }

    public DependencyRulesGraph() {
        createGraph();
    }

    private void createGraph() {
        EdgeFactory<ExecutableRulesMethod, DirectedEdge<ExecutableRulesMethod>> edgeFactory = 
            new DirectedEdgeFactory<ExecutableRulesMethod>(DirectedEdge.class);
        graph = new DefaultDirectedGraph<ExecutableRulesMethod, DirectedEdge<ExecutableRulesMethod>>(edgeFactory);
    }

    private void fill(List<ExecutableRulesMethod> rulesMethods) {
        if (rulesMethods != null && rulesMethods.size() > 0) {
            for (ExecutableRulesMethod method : rulesMethods) {
                graph.addVertex(method);
                BindingDependencies dependencies = method.getDependencies();
                if (dependencies != null) {
                    Set<ExecutableRulesMethod> dependentMethods = dependencies.getRulesMethods();
                    if (dependentMethods != null) {
                        for (ExecutableRulesMethod dependentMethod : dependentMethods) {
                            graph.addVertex(dependentMethod);
                            graph.addEdge(method, dependentMethod);
                        }
                    }
                }
            }
        }
    }

    public DirectedEdge<ExecutableRulesMethod> addEdge(
            ExecutableRulesMethod sourceVertex, ExecutableRulesMethod targetVertex) {        
        return graph.addEdge(sourceVertex, targetVertex);
    }

    public boolean addEdge(ExecutableRulesMethod sourceVertex, ExecutableRulesMethod targetVertex,
            DirectedEdge<ExecutableRulesMethod> e) {        
        return graph.addEdge(sourceVertex, targetVertex, e);
    }

    public boolean addVertex(ExecutableRulesMethod v) {        
        return graph.addVertex(v);
    }

    public boolean containsEdge(DirectedEdge<ExecutableRulesMethod> e) {        
        return graph.containsEdge(e);
    }

    public boolean containsEdge(ExecutableRulesMethod sourceVertex, ExecutableRulesMethod targetVertex) {        
        return graph.containsEdge(sourceVertex, targetVertex);
    }

    public boolean containsVertex(ExecutableRulesMethod v) {
        return graph.containsVertex(v);
    }

    public Set<DirectedEdge<ExecutableRulesMethod>> edgeSet() {        
        return graph.edgeSet();
    }

    public Set<DirectedEdge<ExecutableRulesMethod>> edgesOf(ExecutableRulesMethod vertex) {        
        return graph.edgesOf(vertex);
    }

    public Set<DirectedEdge<ExecutableRulesMethod>> getAllEdges(ExecutableRulesMethod sourceVertex,
            ExecutableRulesMethod targetVertex) {        
        return graph.getAllEdges(sourceVertex, targetVertex);
    }

    public DirectedEdge<ExecutableRulesMethod> getEdge(ExecutableRulesMethod sourceVertex,
            ExecutableRulesMethod targetVertex) {
        return graph.getEdge(sourceVertex, targetVertex);
    }

    public EdgeFactory<ExecutableRulesMethod, DirectedEdge<ExecutableRulesMethod>> getEdgeFactory() {        
        return graph.getEdgeFactory();
    }

    public ExecutableRulesMethod getEdgeSource(DirectedEdge<ExecutableRulesMethod> e) {        
        return graph.getEdgeSource(e);
    }

    public ExecutableRulesMethod getEdgeTarget(DirectedEdge<ExecutableRulesMethod> e) {        
        return graph.getEdgeTarget(e);
    }

    public double getEdgeWeight(DirectedEdge<ExecutableRulesMethod> e) {        
        return graph.getEdgeWeight(e);
    }

    public boolean removeAllEdges(Collection<? extends DirectedEdge<ExecutableRulesMethod>> edges) {        
        return graph.removeAllEdges(edges);
    }

    public Set<DirectedEdge<ExecutableRulesMethod>> removeAllEdges(ExecutableRulesMethod sourceVertex,
            ExecutableRulesMethod targetVertex) {        
        return graph.removeAllEdges(sourceVertex, targetVertex);
    }

    public boolean removeAllVertices(Collection<? extends ExecutableRulesMethod> vertices) {        
        return graph.removeAllVertices(vertices);
    }

    public boolean removeEdge(DirectedEdge<ExecutableRulesMethod> e) {
        return graph.removeEdge(e);
    }

    public DirectedEdge<ExecutableRulesMethod> removeEdge(ExecutableRulesMethod sourceVertex,
            ExecutableRulesMethod targetVertex) {
        return graph.removeEdge(sourceVertex, targetVertex);
    }

    public boolean removeVertex(ExecutableRulesMethod v) {        
        return graph.removeVertex(v);
    }

    public Set<ExecutableRulesMethod> vertexSet() {        
        return graph.vertexSet();
    }
    
    public int inDegreeOf(ExecutableRulesMethod arg0) {        
        return graph.inDegreeOf(arg0);
    }
    public Set<DirectedEdge<ExecutableRulesMethod>> incomingEdgesOf(ExecutableRulesMethod arg0) {        
        return graph.incomingEdgesOf(arg0);
    }
    public int outDegreeOf(ExecutableRulesMethod arg0) {        
        return graph.outDegreeOf(arg0);
    }
    public Set<DirectedEdge<ExecutableRulesMethod>> outgoingEdgesOf(ExecutableRulesMethod arg0) {        
        return graph.outgoingEdgesOf(arg0);
    }

    /**
     * Filter incoming methods, finding {@link ExecutableRulesMethod} and create graph.
     * 
     * @param methods {@link IOpenMethod}
     * @return {@link DependencyRulesGraph} graph representing dependencies between executable rules methods. 
     */
    public static DependencyRulesGraph filterAndCreateGraph(List<IOpenMethod> methods) {
        List<ExecutableRulesMethod> rulesMethods = new ArrayList<ExecutableRulesMethod>(); 
        if (methods != null && methods.size() > 0) {
            for (IOpenMethod method : methods) {
                if (method instanceof ExecutableRulesMethod) {
                    rulesMethods.add((ExecutableRulesMethod)method);
                }
            }
            return new DependencyRulesGraph(rulesMethods);
        } else {
            throw new IllegalArgumentException("There is no rules for building graph.");
        }
    }
    
    

}
