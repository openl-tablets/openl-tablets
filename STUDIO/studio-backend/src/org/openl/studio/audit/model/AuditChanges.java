package org.openl.studio.audit.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

/**
 * Collects what a change did, field by field, for the details of an audit event.
 *
 * <p>A field that changed is written as {@code {"from": …, "to": …}}; a field that kept its value is left out. A
 * blank text counts as no value, so a form that sends an empty text in place of a missing one changes nothing.
 */
public final class AuditChanges {

    private final Map<String, Object> changes = new LinkedHashMap<>();

    /**
     * Notes a field when its value changed.
     */
    public AuditChanges compare(String field, @Nullable Object before, @Nullable Object after) {
        var from = valueOf(before);
        var to = valueOf(after);
        if (!Objects.equals(from, to)) {
            var change = new LinkedHashMap<String, @Nullable Object>();
            change.put("from", from);
            change.put("to", to);
            changes.put(field, change);
        }
        return this;
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * The fields that changed, each with its values before and after.
     */
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(changes);
    }

    /**
     * What the second collection of names adds to the first and takes away from it, for the details of an event:
     * {@code {"added": […], "removed": […]}}, each sorted.
     *
     * @return the names added and removed, or an empty map when both collections hold the same names
     */
    public static Map<String, Set<String>> addedAndRemoved(Collection<String> before, Collection<String> after) {
        var added = added(before, after);
        var removed = removed(before, after);
        return added.isEmpty() && removed.isEmpty() ? Map.of() : Map.of("added", added, "removed", removed);
    }

    /**
     * The names found in the second collection and not in the first, sorted.
     */
    public static Set<String> added(Collection<String> before, Collection<String> after) {
        return difference(after, before);
    }

    /**
     * The names found in the first collection and not in the second, sorted.
     */
    public static Set<String> removed(Collection<String> before, Collection<String> after) {
        return difference(before, after);
    }

    /**
     * The names of the first collection the second one does not hold, sorted.
     */
    private static Set<String> difference(Collection<String> names, Collection<String> others) {
        var difference = new TreeSet<>(names);
        difference.removeAll(others);
        return difference;
    }

    private static @Nullable Object valueOf(@Nullable Object value) {
        return value instanceof String text && text.isBlank() ? null : value;
    }
}
