package org.openl.rules.rest.compile;

/**
 * Navigation URL of a table, used by the UI to open a table (for example from the dependency graph). The URL is the
 * table page link built by {@code WebStudio.url("table", uri)}; the caller appends {@code ?id=<tableId>}.
 *
 * @param url table page URL, or {@code null} when the table is unknown
 */
public record TableUrl(String url) {
}
