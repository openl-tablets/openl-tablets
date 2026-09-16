package org.openl.rules.rest.compile;

/**
 * A table named from beside another one: what it is called, and how to reach it.
 *
 * @param uri  where the table is written, as the engine addresses it
 * @param id   the identifier a screen opens the table by
 * @param name what the table is called
 */
public record TableDescription(String uri, String id, String name) {
}
