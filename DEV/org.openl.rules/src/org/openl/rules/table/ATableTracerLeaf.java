package org.openl.rules.table;

import org.openl.vm.trace.SimpleTracerObject;

public abstract class ATableTracerLeaf extends SimpleTracerObject {
    protected ATableTracerLeaf(String type) {
        super(type);
    }
}
