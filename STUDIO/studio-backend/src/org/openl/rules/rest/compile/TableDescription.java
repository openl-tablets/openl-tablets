package org.openl.rules.rest.compile;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A table named from beside another one: what it is called, and how to reach it.
 *
 * <p>A table written in several versions also carries the values of the dimension properties that tell this version
 * from the others. The name never holds them: a screen writes them, a date among them, in the formats of its reader.
 *
 * @param uri        where the table is written, as the engine addresses it
 * @param id         the identifier a screen opens the table by
 * @param name       what the table is called
 * @param dimensions the values of the dimension properties of a table written in several versions, or {@code null}
 *                   for a table written once
 */
public record TableDescription(String uri, String id, String name, @Nullable Map<String, Object> dimensions) {
}
