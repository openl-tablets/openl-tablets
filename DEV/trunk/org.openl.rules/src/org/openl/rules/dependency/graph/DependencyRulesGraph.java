package org.openl.rules.dependency.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
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
public class DependencyRulesGraph implements DirectedGraph<ExecutableRulesMethod, DefaultEdge> {
    
    private DirectedGraph<ExecutableRulesMethod, DefaultEdge> graph;
    
    public DependencyRulesGraph(List<ExecutableRulesMethod> rulesMethods) {
        createGraph();
        fill(rulesMethods);
    }
    
    public DependencyRulesGraph() {
        createGraph();
    }
    
    private void createGraph() {
        EdgeFactory<ExecutableRulesMethod, DefaultEdge> edgeFactory = 
            new ClassBasedEdgeFactory<ExecutableRulesMethod, DefaultEdge>(DefaultEdge.class);
        graph = new DefaultDirectedGraph<ExecutableRulesMethod, DefaultEdge>(edgeFactory);
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
        } else {
            throw new IllegalArgumentException("There is no rules for building graph.");
        }
    }

    public DefaultEdge addEdge(ExecutableRulesMethod sourceVertex, ExecutableRulesMethod targetVertex) {        
        return graph.addEdge(sourceVertex, targetVertex);
    }

    public boolean addEdge(ExecutableRulesMethod sourceVertex, ExecutableRulesMethod targetVertex, DefaultEdge e) {        
        return graph.addEdge(sourceVertex, targetVertex, e);
    }

    public boolean addVertex(ExecutableRulesMethod v) {        
        return graph.addVertex(v);
    }

    public boolean containsEdge(DefaultEdge e) {        
        return graph.containsEdge(e);
    }

    public boolean containsEdge(ExecutableRulesMethod sourceVertex, ExecutableRulesMethod targetVertex) {        
        return graph.containsEdge(sourceVertex, targetVertex);
    }

    public boolean containsVertex(ExecutableRulesMethod v) {
        return graph.containsVertex(v);
    }

    public Set<DefaultEdge> edgeSet() {        
        return graph.edgeSet();
    }

    public Set<DefaultEdge> edgesOf(ExecutableRulesMethod vertex) {        
        return graph.edgesOf(vertex);
    }

    public Set<DefaultEdge> getAllEdges(ExecutableRulesMethod sourceVertex, ExecutableRulesMethod targetVertex) {        
        return graph.getAllEdges(sourceVertex, targetVertex);
    }

    public DefaultEdge getEdge(ExecutableRulesMethod sourceVertex, ExecutableRulesMethod targetVertex) {
        return graph.getEdge(sourceVertex, targetVertex);
    }

    public EdgeFactory<ExecutableRulesMethod, DefaultEdge> getEdgeFactory() {        
        return graph.getEdgeFactory();
    }

    public ExecutableRulesMethod getEdgeSource(DefaultEdge e) {        
        return graph.getEdgeSource(e);
    }

    public ExecutableRulesMethod getEdgeTarget(DefaultEdge e) {        
        return graph.getEdgeTarget(e);
    }

    public double getEdgeWeight(DefaultEdge e) {        
        return graph.getEdgeWeight(e);
    }

    public boolean removeAllEdges(Collection<? extends DefaultEdge> edges) {        
        return graph.removeAllEdges(edges);
    }

    public Set<DefaultEdge> removeAllEdges(ExecutableRulesMethod sourceVertex, ExecutableRulesMethod targetVertex) {        
        return graph.removeAllEdges(sourceVertex, targetVertex);
    }

    public boolean removeAllVertices(Collection<? extends ExecutableRulesMethod> vertices) {        
        return graph.removeAllVertices(vertices);
    }

    public boolean removeEdge(DefaultEdge e) {
        return graph.removeEdge(e);
    }

    public DefaultEdge removeEdge(ExecutableRulesMethod sourceVertex, ExecutableRulesMethod targetVertex) {
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
    public Set<DefaultEdge> incomingEdgesOf(ExecutableRulesMethod arg0) {        
        return graph.incomingEdgesOf(arg0);
    }
    public int outDegreeOf(ExecutableRulesMethod arg0) {        
        return graph.outDegreeOf(arg0);
    }
    public Set<DefaultEdge> outgoingEdgesOf(ExecutableRulesMethod arg0) {        
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
