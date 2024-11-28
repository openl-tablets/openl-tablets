package org.openl.rules.dependency.graph;

/**
 * @author Andrei Astrouski
 */
public class DirectedEdge<V> {

    private V sourceVertex;
    private V targetVertex;

    public DirectedEdge() {
    }

    public DirectedEdge(V sourceVertex, V targetVertex) {
        this.sourceVertex = sourceVertex;
        this.targetVertex = targetVertex;
    }

    public V getSourceVertex() {
        return sourceVertex;
    }

    public void setSourceVertex(V sourceVertex) {
        this.sourceVertex = sourceVertex;
    }

    public V getTargetVertex() {
        return targetVertex;
    }

    public void setTargetVertex(V targetVertex) {
        this.targetVertex = targetVertex;
    }

}
