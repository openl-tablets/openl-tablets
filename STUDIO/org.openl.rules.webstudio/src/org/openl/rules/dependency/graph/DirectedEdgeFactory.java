package org.openl.rules.dependency.graph;

import org.jgrapht.EdgeFactory;

/**
 * @author Andrei Astrouski
 */
public class DirectedEdgeFactory<V> implements EdgeFactory<V, DirectedEdge<V>> {

    private final Class<DirectedEdge<V>> edgeClass;

    @SuppressWarnings("unchecked")
    public DirectedEdgeFactory(Class directedEdgeClass) {
        this.edgeClass = directedEdgeClass;
    }

    @Override
    public DirectedEdge<V> createEdge(V sourceVertex, V targetVertex) {
        try {
            DirectedEdge<V> edge = edgeClass.newInstance();
            edge.setSourceVertex(sourceVertex);
            edge.setTargetVertex(targetVertex);

            return edge;
        } catch(Exception exception) {
            throw new RuntimeException("Edge factory failed", exception);
        }
    }

}
