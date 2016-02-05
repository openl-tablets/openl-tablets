package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.tbasic.AlgorithmSubroutineMethod;

public class TBasicMethodTraceObject extends ATableTracerNode {

    public TBasicMethodTraceObject(AlgorithmSubroutineMethod method) {
        super("tbasicMethod", "Algorithm Method", method, null);
    }
}
