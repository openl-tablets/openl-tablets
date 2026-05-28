package org.openl.studio.common.projection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A node of the parsed {@code ?fields=} selection tree.
 *
 * <p>Each node owns the set of explicitly requested child fields. A node with no children is a
 * <em>leaf selection</em>: the corresponding value is serialized in full (the whole object/array),
 * which is how a field selected without a nested {@code (...)} sub-selection behaves.
 *
 * @author Vladyslav Pikus
 */
public final class FieldNode {

    private final Map<String, FieldNode> children = new LinkedHashMap<>();

    public boolean hasChildren() {
        return !children.isEmpty();
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
