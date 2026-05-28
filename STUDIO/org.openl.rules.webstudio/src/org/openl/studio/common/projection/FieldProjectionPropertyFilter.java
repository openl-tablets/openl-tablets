package org.openl.studio.common.projection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

/**
 * Path-aware Jackson property filter that applies a hierarchical {@code ?fields=} selection tree.
 *
 * <p>The same filter instance is invoked for every projectable bean in the response graph (they all
 * share one filter id). For each property it computes the bean's path relative to the projection root
 * and consults the matching {@link FieldNode}:
 * <ul>
 *   <li>a node with explicit children keeps only those children (nested objects/arrays are projected
 *       recursively because their own beans resolve to the corresponding sub-node);</li>
 *   <li>a leaf node (a field selected without a {@code (...)} sub-selection) keeps the whole value.</li>
 * </ul>
 *
 * <p>The projection root is anchored to the first projectable bean encountered during serialization,
 * so the same logic works for a single object, a collection/array element, or an element inside a
 * pagination wrapper (the wrapper itself is not projectable and keeps all of its fields).
 *
 * <p>One instance is created per response and serialization is single-threaded, so the lazily captured
 * {@link #anchor} needs no synchronization.
 *
 * @author Vladyslav Pikus
 */
public class FieldProjectionPropertyFilter extends SimpleBeanPropertyFilter {

    private final FieldNode root;
    private String[] anchor;

    public FieldProjectionPropertyFilter(FieldNode root) {
        this.root = root;
    }

    @Override
    public void serializeAsField(Object pojo, JsonGenerator gen, SerializerProvider provider,
                                 PropertyWriter writer) throws Exception {
        if (includes(gen, writer)) {
            writer.serializeAsField(pojo, gen, provider);
        } else if (!gen.canOmitFields()) {
            writer.serializeAsOmittedField(pojo, gen, provider);
        }
    }

    private boolean includes(JsonGenerator gen, PropertyWriter writer) {
        var node = resolveNode(gen);
        // No node, or a leaf selection -> the whole object is kept.
        if (node == null || !node.hasChildren()) {
            return true;
        }
        return node.contains(writer.getName());
    }

    /**
     * Resolves the selection node that governs the bean currently being serialized.
     */
    private FieldNode resolveNode(JsonGenerator gen) {
        var path = absolutePath(gen);
        if (anchor == null) {
            // First projectable bean = projection root; remember where it sits in the JSON tree.
            anchor = path.toArray(new String[0]);
        }
        var node = root;
        for (int i = Math.min(anchor.length, path.size()); i < path.size(); i++) {
            if (node == null || !node.hasChildren()) {
                return node;
            }
            node = node.child(path.get(i));
        }
        return node;
    }

    /**
     * The chain of object field names from the JSON root down to (but excluding) the current bean.
     * Array indices contribute no segment, so collection elements share their container's path.
     */
    private static List<String> absolutePath(JsonGenerator gen) {
        var segments = new ArrayDeque<String>();
        for (JsonStreamContext ctx = gen.getOutputContext().getParent(); ctx != null && !ctx.inRoot(); ctx = ctx.getParent()) {
            var name = ctx.getCurrentName();
            if (name != null) {
                segments.addFirst(name);
            }
        }
        return new ArrayList<>(segments);
    }
}
