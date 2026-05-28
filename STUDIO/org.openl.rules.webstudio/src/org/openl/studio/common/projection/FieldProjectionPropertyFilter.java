package org.openl.studio.common.projection;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import lombok.RequiredArgsConstructor;

/**
 * Applies a hierarchical {@code ?fields=} selection to every projectable bean in a response.
 *
 * <p>For each property, the filter looks up the matching node by its path relative to the projection
 * root. A node with explicit children keeps only those children; a leaf node keeps the whole value.
 * Nested objects and arrays are projected the same way -- their own beans resolve to the
 * corresponding sub-nodes.
 *
 * <p>The projection root is the first projectable bean seen during serialization. The same logic
 * works for a single object, a collection element, or an element inside a pagination wrapper -- the
 * wrapper itself is not projectable and keeps all of its fields.
 *
 * <p>One instance per response; serialization is single-threaded, so no synchronization is needed.
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class FieldProjectionPropertyFilter extends SimpleBeanPropertyFilter {

    private final FieldNode root;
    private String[] anchor;

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
     * The selection node that governs the bean currently being serialized, or {@code null} when
     * nothing constrains it.
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
     * Path from the JSON root to the parent of the current bean, listed root-to-leaf.
     *
     * <p>Collection elements do not contribute path segments, so all elements of a collection share
     * their container's path.
     */
    private static List<String> absolutePath(JsonGenerator gen) {
        return Stream.iterate(
                        gen.getOutputContext().getParent(),
                        ctx -> ctx != null && !ctx.inRoot(),
                        JsonStreamContext::getParent)
                .map(JsonStreamContext::getCurrentName)
                .filter(Objects::nonNull)
                .toList()
                .reversed();
    }
}
