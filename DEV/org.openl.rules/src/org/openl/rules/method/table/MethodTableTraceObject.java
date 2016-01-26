package org.openl.rules.method.table;

import org.openl.rules.table.ATableTracerNode;

/**
 * Trace object for method table.
 *
 * @author PUdalau
 */
public class MethodTableTraceObject extends ATableTracerNode {

    public MethodTableTraceObject(TableMethod method, Object[] params) {
        super("method", "Method table", method, params);
    }
}
