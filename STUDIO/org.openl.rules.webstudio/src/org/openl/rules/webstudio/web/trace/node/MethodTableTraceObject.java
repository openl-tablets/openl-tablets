package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.method.table.TableMethod;

/**
 * Trace object for method table.
 *
 * @author PUdalau
 */
public class MethodTableTraceObject extends ATableTracerNode {

    MethodTableTraceObject(TableMethod method, Object[] params) {
        super("method", "Method table", method, params);
    }
}
