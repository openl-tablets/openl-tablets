package org.openl.studio.projects.model.trace;

/**
 * Aggregated profiling time for one table across all its invocations.
 *
 * <p>Accumulated on the fly as frames complete, independent of the retained call tree. It therefore stays
 * accurate even when the tree is truncated for size, counting every execution the run actually made.
 *
 * @param uri        source URI of the table
 * @param name       display name of the table
 * @param kind       kind of the table
 * @param count      number of times the table was invoked in the run
 * @param selfNanos  own execution time across all invocations, excluding the tables it called
 * @param totalNanos inclusive execution time across all invocations, including the tables it called
 */
public record TableProfile(String uri, String name, FrameKind kind, int count, long selfNanos, long totalNanos) {
}
