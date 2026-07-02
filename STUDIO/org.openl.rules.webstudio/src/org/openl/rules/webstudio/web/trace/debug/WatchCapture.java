package org.openl.rules.webstudio.web.trace.debug;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * One captured value of a watched cell, recorded the moment the cell computed during the run.
 *
 * <p>A watch retains the value of a named cell on every execution of its table, so a factor can be read
 * across all coverages or iterations without dumping every frame. Each capture ties the value to the
 * execution it came from: which table, its invocation number, the call path to it, and the breakpoint
 * key that reaches the cell.
 *
 * @param name     the watched cell name (its {@code $...} step label), which is also the series key
 * @param table    display name of the table the cell belongs to
 * @param tableUri source URI of that table
 * @param instance zero-based execution number of the table (its 1st, 2nd, ... invocation in the run)
 * @param path     call path from the root frame to the owning frame, as display names
 * @param ref      breakpoint key {@code uri#cellRef} that reaches this cell
 * @param value    the captured value: a scalar (number, string, boolean) or a short summary for a
 *                 non-scalar
 */
public record WatchCapture(String name, String table, String tableUri, int instance, List<String> path,
                           String ref, @Nullable Object value) {

    public WatchCapture {
        path = List.copyOf(path);
    }
}
