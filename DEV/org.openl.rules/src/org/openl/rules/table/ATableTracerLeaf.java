package org.openl.rules.table;

import org.openl.vm.trace.SimpleTracerObject;

public abstract class ATableTracerLeaf extends SimpleTracerObject implements ITableTracerObject {

    public ATableTracerLeaf() {
        super();
    }

    public ATableTracerLeaf(Object tracedObject) {
        super(tracedObject);
    }
}
