package org.openl.studio.common.projection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A node in the parsed {@code ?fields=} selection tree.
 *
 * <p>Each node lists the child fields the client asked for.
 *
 * <p>A node with no children is a leaf: the value is kept whole. This is how a field selected
 * without a {@code (...)} sub-selection behaves.
 *
 * @author Vladyslav Pikus
 */
public final class FieldNode {

    private final Map<String, FieldNode> children = new LinkedHashMap<>();

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Whether no fields were requested under this node.
     *
     * <p>On the root: nothing to project. On a child: the field was selected as a leaf and is kept whole.
     */
    public boolean isEmpty() {
        return children.isEmpty();
    }

    public boolean contains(String name) {
        return children.containsKey(name);
    }

    public FieldNode child(String name) {
        return children.get(name);
    }

    public Map<String, FieldNode> children() {
        return children;
    }

    FieldNode getOrAdd(String name) {
        return children.computeIfAbsent(name, key -> new FieldNode());
    }
}
