package org.openl.rules.dependency.graph;

import org.jgrapht.graph.DefaultEdge;

/**
 * @author Andrei Astrouski
 */
@SuppressWarnings("unchecked")
public class DirectedEdge<V> extends DefaultEdge {

    public V getSourceVertex() {
        return (V) getSource();
    }

    public V getTargetVertex() {
        return (V) getTarget();
    }

}
